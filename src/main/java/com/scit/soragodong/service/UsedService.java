package com.scit.soragodong.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.scit.soragodong.domain.dto.FileRes;
import com.scit.soragodong.domain.dto.KeywordDto;
import com.scit.soragodong.domain.dto.UsedDetailRes;
import com.scit.soragodong.domain.dto.UsedUpdateReq;
import com.scit.soragodong.domain.dto.UsedListRes;
import com.scit.soragodong.domain.dto.UsedRegisterReq;
import com.scit.soragodong.domain.dto.UsedRegisterRes;
import com.scit.soragodong.domain.entity.FileGrp;
import com.scit.soragodong.domain.entity.Used;
import com.scit.soragodong.domain.entity.UsedKeyword;
import com.scit.soragodong.domain.entity.UsedLike;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.domain.enums.FileRefType;
import com.scit.soragodong.domain.enums.NotificationType;
import com.scit.soragodong.domain.enums.UsedState;
import com.scit.soragodong.domain.response.PageResponse;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.FileGrpRepository;
import com.scit.soragodong.repository.FileRepository;
import com.scit.soragodong.repository.UserRepository;
import com.scit.soragodong.repository.UsedKeywordRepository;
import com.scit.soragodong.repository.UsedLikeRepository;
import com.scit.soragodong.repository.UsedRepository;

import com.scit.soragodong.domain.dto.FinanceDto;
import com.scit.soragodong.service.FinanceService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UsedService {

    private static final int PAGE_SIZE = 10;

    private final UsedRepository usedRepository;
    private final UsedKeywordRepository usedKeywordRepository;
    private final UsedLikeRepository usedLikeRepository;
    private final FileGrpRepository fileGrpRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ViewCountService viewCountService;
    private final FinanceService financeService;

    /**
     * 중고 물건 등록
     * 1. 물건 저장 (트랜잭션 내)
     * 2. 트랜잭션 커밋 후 키워드 매칭 및 알림 전송 (비동기)
     */
    public UsedRegisterRes registerUsedItem(UsedRegisterReq req) {
        log.info("[중고] 물건 등록 시작 - 제목: {}", req.usedTitle());

        Used used = Used.builder()
                .usedTitle(req.usedTitle())
                .usedContent(req.usedContent())
                .usedPrice(req.usedPrice())
                .tradingLoc(req.tradingLoc())
                .tradingLat(req.tradingLatitude())
                .tradingLng(req.tradingLongitude())
                .createdUser(req.createdUser())
                .usedState(UsedState.TRADING)
                .viewCount(0)
                .isUse(true)
                .build();

        Used savedUsed = usedRepository.save(used);
        log.info("[중고] 물건 저장 완료 - ID: {}", savedUsed.getUsedIdx());

        // 트랜잭션 커밋 후 비동기로 키워드 매칭 및 알림 전송
        // (커밋 후 실행해야 다른 트랜잭션에서 Used 데이터가 보임)
        Integer usedIdx = savedUsed.getUsedIdx();
        String title = req.usedTitle();
        String content = req.usedContent();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.notifyUsedKeywordMatches(usedIdx, title, content);
            }
        });

        return UsedRegisterRes.from(savedUsed, 0);
    }

    /**
     * 중고 물품 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public PageResponse<UsedListRes> getUsedList(int page, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, PAGE_SIZE);

        Page<Used> usedPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            usedPage = usedRepository.searchByKeyword(keyword.trim(), pageable);
        } else {
            usedPage = usedRepository.findByIsUseTrueOrderByCreatedAtDesc(pageable);
        }

        List<UsedListRes> content = usedPage.getContent().stream()
                .map(used -> {
                    String thumbnail = getThumbnailUrl(used.getUsedIdx());
                    int likeCount = usedLikeRepository.countByUsedIdx(used.getUsedIdx());
                    return UsedListRes.from(used, thumbnail, likeCount, 0);
                })
                .toList();

        return PageResponse.<UsedListRes>builder()
                .content(content)
                .currentPage(page)
                .totalPages(usedPage.getTotalPages())
                .totalElements(usedPage.getTotalElements())
                .hasNext(usedPage.hasNext())
                .hasPrevious(usedPage.hasPrevious())
                .build();
    }

    /**
     * 중고 물품 상세 조회
     *
     * @param viewerKey 조회자 식별자 (로그인: "u{userId}", 비로그인: "s{sessionId}")
     *                  Redis Set으로 24시간 내 중복 조회를 방지한다.
     */
    @Transactional
    public UsedDetailRes getUsedDetail(Integer usedIdx, Integer currentUserId, String viewerKey) {
        Used used = usedRepository.findByUsedIdxAndIsUseTrue(usedIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 조회수 증가 (중복 방지 - 24시간 내 동일 조회자 재조회 시 생략)
        // 새 조회일 때만 increment → 엔티티가 stale이므로 직접 +1 보정
        boolean isNewView = viewCountService.record("USED", usedIdx, viewerKey);
        if (isNewView) {
            usedRepository.incrementViewCount(usedIdx);
        }
        int viewCount = used.getViewCount() + (isNewView ? 1 : 0);

        // 이미지 URL 목록
        List<String> images = getImageUrls(usedIdx);

        // 좋아요 수 및 현재 사용자 좋아요 여부
        int likeCount = usedLikeRepository.countByUsedIdx(usedIdx);
        boolean isLiked = currentUserId != null &&
                usedLikeRepository.existsByUsedIdxAndUserIdx(usedIdx, currentUserId);

        // 판매자 정보 조회
        Users seller = userRepository.findById(used.getCreatedUser()).orElse(null);

        // 작성자 여부
        boolean isOwner = currentUserId != null &&
                currentUserId.equals(used.getCreatedUser());

        return UsedDetailRes.from(used, images, likeCount, isLiked, isOwner, seller, 0, viewCount);
    }

    /**
     * 썸네일 URL 조회
     */
    private String getThumbnailUrl(Integer usedIdx) {
        return fileGrpRepository.findByRefTypeAndRefId(FileRefType.USED, usedIdx)
                .map(grp -> "/img/group/" + grp.getFileGrpIdx())
                .orElse(null);
    }

    /**
     * 이미지 URL 목록 조회
     */
    private List<String> getImageUrls(Integer usedIdx) {
        Optional<FileGrp> fileGrp = fileGrpRepository.findByRefTypeAndRefId(FileRefType.USED, usedIdx);
        if (fileGrp.isEmpty()) {
            return List.of();
        }
        return fileRepository.findByFileGroupAndIsUseTrueOrderByFileOrder(fileGrp.get()).stream()
                .map(file -> "/img/" + file.getFileIdx())
                .toList();
    }

    // ==================== 수정 / 삭제 ====================

    /**
     * 중고 물품 논리 삭제
     */
    public void deleteUsedItem(Integer usedIdx, Integer userIdx) {
        Used used = usedRepository.findByUsedIdxAndIsUseTrue(usedIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        if (!used.getCreatedUser().equals(userIdx)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        used.delete();
        log.info("[중고] 상품 삭제 완료 - ID: {}, 사용자: {}", usedIdx, userIdx);
    }

    /**
     * 중고 물품 수정
     */
    public void updateUsedItem(Integer usedIdx, UsedUpdateReq req, Integer userIdx) {
        Used used = usedRepository.findByUsedIdxAndIsUseTrue(usedIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        if (!used.getCreatedUser().equals(userIdx)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        used.update(req.usedTitle(), req.usedContent(), req.usedPrice(),
                req.tradingLoc(), req.tradingLatitude(), req.tradingLongitude());

        if (req.deleteFileIdxs() != null && !req.deleteFileIdxs().isEmpty()) {
            fileRepository.deleteAllById(req.deleteFileIdxs());
        }

        log.info("[중고] 상품 수정 완료 - ID: {}", usedIdx);
    }

    /**
     * 수정 시 기존 이미지 파일 목록 조회
     */
    @Transactional(readOnly = true)
    public List<FileRes> getUsedFiles(Integer usedIdx) {
        Optional<FileGrp> fileGrp = fileGrpRepository.findByRefTypeAndRefId(FileRefType.USED, usedIdx);
        if (fileGrp.isEmpty()) {
            return List.of();
        }
        return fileRepository.findByFileGroupAndIsUseTrueOrderByFileOrder(fileGrp.get()).stream()
                .map(f -> new FileRes(f.getFileIdx(), f.getOriginalName(), "/img/" + f.getFileIdx(), f.getFileOrder()))
                .toList();
    }

    /**
     * 상품 상태 변경 (판매자 전용)
     */
    public void updateState(Integer usedIdx, UsedState state, Integer buyerIdx, Integer userIdx) {
        Used used = usedRepository.findByUsedIdxAndIsUseTrue(usedIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (!used.getCreatedUser().equals(userIdx)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        used.updateState(state);
        log.info("[중고] 상태 변경 - ID: {}, 상태: {}", usedIdx, state);

        // 판매 완료 시 가계부에 자동 수입/지출 등록 및 알림
        if (state == UsedState.SOLD) {
            try {
                // 1. 판매자 수입 기록
                FinanceDto income = FinanceDto.builder()
                        .date(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .time(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
                        .category("기타")
                        .type("plus")
                        .amount(used.getUsedPrice() != null ? used.getUsedPrice().longValue() : 0L)
                        .memo("중고거래 수입 - " + used.getUsedTitle())
                        .isFixed(false)
                        .build();
                financeService.write(income, userIdx);
                log.info("[중고] 가계부 수입 자동 등록 완료 - 판매자: {}", userIdx);

                // 2. 구매자 지출 기록 및 알림 (buyerIdx가 있는 경우)
                if (buyerIdx != null) {
                    FinanceDto expense = FinanceDto.builder()
                            .date(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                            .time(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
                            .category("기타")
                            .type("minus")
                            .amount(used.getUsedPrice() != null ? used.getUsedPrice().longValue() : 0L)
                            .memo("중고거래 지출 - " + used.getUsedTitle())
                            .isFixed(false)
                            .build();
                    financeService.write(expense, buyerIdx);
                    log.info("[중고] 가계부 지출 자동 등록 완료 - 구매자: {}", buyerIdx);

                    // 구매자에게 거래 완료 알림 전송
                    String buyerMsg = String.format("'%s' 상품 거래가 완료되어 가계부에 지출이 등록되었습니다.", used.getUsedTitle());
                    notificationService.send(buyerIdx, NotificationType.USED_STATE_CHANGE, usedIdx, buyerMsg);
                }
            } catch (Exception e) {
                log.error("[중고] 가계부 수입/지출 자동 등록 실패", e);
            }
        }

        // 찜한 사용자들에게 상태 변경 알림 (판매자 및 구매자 제외)
        String stateName = switch (state) {
            case TRADING -> "거래중";
            case SALE -> "판매중";
            case RESERVED -> "예약중";
            case SOLD -> "판매완료";
        };
        String msg = String.format("찜한 '%s' 상품이 '%s' 상태로 변경되었습니다.", used.getUsedTitle(), stateName);
        usedLikeRepository.findByUsedIdx(usedIdx).forEach(like -> {
            if (!like.getUserIdx().equals(userIdx) && !like.getUserIdx().equals(buyerIdx)) {
                notificationService.send(like.getUserIdx(), NotificationType.USED_STATE_CHANGE, usedIdx, msg);
            }
        });
    }

    /**
     * 찜(좋아요) 토글
     * @return true: 찜 추가, false: 찜 취소
     */
    public boolean toggleLike(Integer usedIdx, Integer userIdx) {
        Used used = usedRepository.findByUsedIdxAndIsUseTrue(usedIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        if (usedLikeRepository.existsByUsedIdxAndUserIdx(usedIdx, userIdx)) {
            usedLikeRepository.deleteByUsedIdxAndUserIdx(usedIdx, userIdx);
            return false;
        }
        usedLikeRepository.save(UsedLike.builder().usedIdx(usedIdx).userIdx(userIdx).build());

        // 판매자에게 찜 알림 (본인 상품 제외)
        if (!used.getCreatedUser().equals(userIdx)) {
            Users liker = userRepository.findById(userIdx).orElse(null);
            String likerNick = liker != null ? liker.getUserNickname() : "누군가";
            String msg = String.format("'%s'님이 '%s' 상품을 찜했습니다.", likerNick, used.getUsedTitle());
            notificationService.send(used.getCreatedUser(), NotificationType.USED_LIKE, usedIdx, msg);
        }
        return true;
    }

    // ==================== 키워드 관리 ====================

    private static final int MAX_KEYWORDS_PER_USER = 10;

    /**
     * 사용자의 키워드 목록 조회
     */
    @Transactional(readOnly = true)
    public List<KeywordDto> getKeywords(Integer userIdx) {
        return usedKeywordRepository.findByUserIdxAndIsUseTrue(userIdx).stream()
                .map(KeywordDto::from)
                .toList();
    }

    /**
     * 키워드 등록
     */
    public KeywordDto addKeyword(Integer userIdx, String keyword) {
        String trimmedKeyword = keyword.trim();

        if (trimmedKeyword.isEmpty() || trimmedKeyword.length() > 20) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        int currentCount = usedKeywordRepository.countByUserIdxAndIsUseTrue(userIdx);
        if (currentCount >= MAX_KEYWORDS_PER_USER) {
            throw new IllegalStateException("키워드는 최대 " + MAX_KEYWORDS_PER_USER + "개까지 등록 가능합니다.");
        }

        if (usedKeywordRepository.existsByUserIdxAndKeywordAndIsUseTrue(userIdx, trimmedKeyword)) {
            throw new IllegalStateException("이미 등록된 키워드입니다.");
        }

        UsedKeyword newKeyword = UsedKeyword.builder()
                .userIdx(userIdx)
                .keyword(trimmedKeyword)
                .isUse(true)
                .build();

        UsedKeyword saved = usedKeywordRepository.save(newKeyword);
        log.info("[키워드] 등록 완료 - 사용자: {}, 키워드: {}", userIdx, trimmedKeyword);

        return KeywordDto.from(saved);
    }

    /**
     * 키워드 삭제 (물리 삭제)
     */
    public void deleteKeyword(Integer userIdx, Integer keywordIdx) {
        UsedKeyword keyword = usedKeywordRepository.findByKeywordIdxAndUserIdx(keywordIdx, userIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        usedKeywordRepository.delete(keyword);
        log.info("[키워드] 삭제 완료 - 사용자: {}, 키워드ID: {}", userIdx, keywordIdx);
    }
}
