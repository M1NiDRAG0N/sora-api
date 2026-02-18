package com.scit.soragodong.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scit.soragodong.domain.dto.KeywordDto;
import com.scit.soragodong.domain.dto.UsedDetailRes;
import com.scit.soragodong.domain.dto.UsedListRes;
import com.scit.soragodong.domain.dto.UsedRegisterReq;
import com.scit.soragodong.domain.dto.UsedRegisterRes;
import com.scit.soragodong.domain.entity.FileGrp;
import com.scit.soragodong.domain.entity.Notification;
import com.scit.soragodong.domain.entity.Used;
import com.scit.soragodong.domain.entity.UsedKeyword;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.domain.enums.FileRefType;
import com.scit.soragodong.domain.enums.NotificationType;
import com.scit.soragodong.domain.enums.UsedState;
import com.scit.soragodong.domain.response.PageResponse;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.FileGrpRepository;
import com.scit.soragodong.repository.FileRepository;
import com.scit.soragodong.repository.NotificationRepository;
import com.scit.soragodong.repository.UserRepository;
import com.scit.soragodong.repository.UsedKeywordRepository;
import com.scit.soragodong.repository.UsedLikeRepository;
import com.scit.soragodong.repository.UsedRepository;

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
    private final NotificationRepository notificationRepository;
    private final UsedLikeRepository usedLikeRepository;
    private final FileGrpRepository fileGrpRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final SseService sseService;
    
    /**
     * 중고 물건 등록 및 알림 전송
     * 1. 물건 저장
     * 2. 키워드 매칭
     * 3. 매칭된 사용자에게 알림 전송 (SSE)
     * 4. 알림 데이터 DB 저장
     */
    public UsedRegisterRes registerUsedItem(UsedRegisterReq req) {
        log.info("[중고] 물건 등록 시작 - 제목: {}", req.usedTitle());
        
        // 1️⃣ 물건 저장
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
        
        // 2️⃣ 키워드 매칭 및 알림 전송
        Integer notifiedCount = notifyMatchedUsers(savedUsed, req.usedTitle(), req.usedContent());
        
        log.info("[중고] 물건 등록 완료 - 알림 발송 사용자: {}", notifiedCount);
        return UsedRegisterRes.from(savedUsed, notifiedCount);
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
     */
    @Transactional
    public UsedDetailRes getUsedDetail(Integer usedIdx, Integer currentUserId) {
        Used used = usedRepository.findByUsedIdxAndIsUseTrue(usedIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 조회수 증가
        usedRepository.incrementViewCount(usedIdx);

        // 이미지 URL 목록
        List<String> images = getImageUrls(usedIdx);

        // 좋아요 수 및 현재 사용자 좋아요 여부
        int likeCount = usedLikeRepository.countByUsedIdx(usedIdx);
        boolean isLiked = currentUserId != null &&
                usedLikeRepository.existsByUsedIdxAndUserIdx(usedIdx, currentUserId);

        // 판매자 정보 조회
        Users seller = userRepository.findById(used.getCreatedUser()).orElse(null);

        return UsedDetailRes.from(used, images, likeCount, isLiked, seller, 0);
    }

    /**
     * 썸네일 URL 조회 (FileController 위임)
     */
    private String getThumbnailUrl(Integer usedIdx) {
        return fileGrpRepository.findByRefTypeAndRefId(FileRefType.USED, usedIdx)
                .map(grp -> "/img/group/" + grp.getFileGrpIdx())
                .orElse(null);
    }

    /**
     * 이미지 URL 목록 조회 (FileController 위임)
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

    /**
     * 키워드 매칭 및 사용자 알림
     */
    private Integer notifyMatchedUsers(Used used, String title, String content) {
        // 모든 활성화된 키워드 조회
        List<UsedKeyword> allKeywords = usedKeywordRepository.findByIsUseTrue();
        log.info("[키워드] 활성화된 키워드 개수: {}", allKeywords.size());
        
        if (allKeywords.isEmpty()) {
            return 0;
        }
        
        // 검색 텍스트 (제목 + 내용)
        String searchText = (title + " " + content).toLowerCase();
        
        // 매칭된 사용자 추출 (중복 제거)
        Set<Integer> matchedUserIds = new HashSet<>();
        for (UsedKeyword keyword : allKeywords) {
            if (isKeywordMatched(searchText, keyword.getKeyword())) {
                matchedUserIds.add(keyword.getUserIdx());
                log.info("[키워드 매칭] 사용자 {} - 키워드 '{}'", keyword.getUserIdx(), keyword.getKeyword());
            }
        }
        
        if (matchedUserIds.isEmpty()) {
            log.info("[키워드] 매칭된 사용자 없음");
            return 0;
        }
        
        // 각 사용자에게 알림 전송
        for (Integer userId : matchedUserIds) {
            sendNotificationToUser(userId, used);
        }
        
        return matchedUserIds.size();
    }
    
    /**
     * 키워드 매칭 로직
     * - 정확한 단어 매칭 (공백 기준)
     * - 대소문자 무시
     */
    private boolean isKeywordMatched(String searchText, String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        
        // 정규식: 단어 경계로 정확히 매칭
        String regex = "\\b" + Pattern.quote(lowerKeyword) + "\\b";
        return Pattern.compile(regex).matcher(searchText).find();
    }
    
    /**
     * 사용자에게 알림 전송
     * 1. 알림 데이터 DB 저장
     * 2. SSE를 통한 실시간 알림 전송
     */
    private void sendNotificationToUser(Integer userId, Used used) {
        try {
            // 알림 메시지 생성
            String message = String.format("'%s' 상품이 당신의 관심 키워드와 매칭되었습니다!", used.getUsedTitle());
            
            // 1️⃣ 알림 데이터 저장
            Notification notification = Notification.builder()
                    .userIdx(userId)
                    .notiType(NotificationType.USED_KEYWORD_MATCH)
                    .refId(used.getUsedIdx())
                    .message(message)
                    .isRead(false)
                    .build();
            
            Notification savedNotification = notificationRepository.save(notification);
            log.info("[알림 저장] 사용자 {} - 알림 ID: {}", userId, savedNotification.getNotiIdx());
            
            // 2️⃣ SSE 실시간 알림 전송
            sseService.notify(userId, NotificationType.USED_KEYWORD_MATCH.name(), 
                    savedNotification.getNotiIdx(), message);
            log.info("[SSE 알림 전송] 사용자 {}", userId);
            
        } catch (Exception e) {
            log.error("[알림 전송 실패] 사용자 {}: {}", userId, e.getMessage());
            // 알림 전송 실패해도 물건 등록은 진행
        }
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
        // 키워드 정제
        String trimmedKeyword = keyword.trim();

        if (trimmedKeyword.isEmpty() || trimmedKeyword.length() > 20) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 최대 개수 확인
        int currentCount = usedKeywordRepository.countByUserIdxAndIsUseTrue(userIdx);
        if (currentCount >= MAX_KEYWORDS_PER_USER) {
            throw new IllegalStateException("키워드는 최대 " + MAX_KEYWORDS_PER_USER + "개까지 등록 가능합니다.");
        }

        // 중복 확인
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
     * 키워드 삭제 (soft delete)
     */
    public void deleteKeyword(Integer userIdx, Integer keywordIdx) {
        UsedKeyword keyword = usedKeywordRepository.findByKeywordIdxAndUserIdx(keywordIdx, userIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // Soft delete를 위해 엔티티에 setter 필요, 또는 물리 삭제
        usedKeywordRepository.delete(keyword);
        log.info("[키워드] 삭제 완료 - 사용자: {}, 키워드ID: {}", userIdx, keywordIdx);
    }
}
