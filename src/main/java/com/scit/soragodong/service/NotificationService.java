package com.scit.soragodong.service;

import com.scit.soragodong.domain.dto.NotificationRes;
import com.scit.soragodong.domain.entity.Notification;
import com.scit.soragodong.domain.entity.Used;
import com.scit.soragodong.domain.entity.UsedKeyword;
import com.scit.soragodong.domain.enums.NotificationType;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.NotificationRepository;
import com.scit.soragodong.repository.UsedKeywordRepository;
import com.scit.soragodong.repository.UsedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 공통 알림 서비스
 * - 알림 DB 저장 및 SSE 실시간 전송
 * - 중고거래 키워드 매칭 알림 (비동기)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UsedRepository usedRepository;
    private final UsedKeywordRepository usedKeywordRepository;
    private final SseService sseService;

    /**
     * 알림 DB 저장 + SSE 전송 (공통 동기 메서드)
     * 다른 서비스에서 직접 호출 가능
     */
    @Transactional
    public void send(Integer userIdx, NotificationType type, Integer refId, String message) {
        Notification notification = Notification.builder()
                .userIdx(userIdx)
                .notiType(type)
                .refId(refId)
                .message(message)
                .isRead(false)
                .build();
        Notification saved = notificationRepository.save(notification);
        log.info("[알림 저장] 사용자 {} - 유형: {}, 알림 ID: {}", userIdx, type, saved.getNotiIdx());

        try {
            sseService.notify(userIdx, type.name(), refId, message);
            log.info("[SSE 알림 전송] 사용자 {}", userIdx);
        } catch (Exception e) {
            log.warn("[SSE] 전송 실패 - 사용자 {}: {}", userIdx, e.getMessage());
        }
    }

    /**
     * 모든 사용자에게 알림 전송 (DB 저장 + SSE)
     */
    @Transactional
    public void broadcast(NotificationType type, Integer refId, String message) {
        log.info("[알림 브로드캐스트] 유형: {}, 메시지: {}", type, message);

        // 1. DB에 userIdx=0으로 공지용 알림 저장 (필요한 경우)
        // 여기서는 모든 개별 사용자에게 알림이 가도록 처리하는 것이 사용자의 요청에 부합할 수 있음.
        // 하지만 효율성을 위해 모든 유저에게 하나씩 저장함.

        // 2. 현재 접속 중인 모든 사용자에게 SSE 전송 및 DB 저장
        // (실제 서비스에서는 시스템 공지 테이블을 따로 두기도 하지만,
        // 현재 NotificationService.getNotifications 로직을 보면 userIdx=0인 경우를 처리하는 로직이 있음)

        Notification notification = Notification.builder()
                .userIdx(0) // 시스템 공지용 ID
                .notiType(type)
                .refId(refId)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        // SSE는 현재 연결된 모든 사용자에게 보냄
        sseService.broadcast(refId, message);
    }

    /**
     * 중고 키워드 매칭 및 알림 전송 (비동기)
     * - 상품 등록 트랜잭션 커밋 후 별도 스레드에서 실행
     * - 자체 트랜잭션으로 DB 작업 처리
     */
    @Async
    @Transactional
    public void notifyUsedKeywordMatches(Integer usedIdx, String title, String content) {
        log.info("[알림] 중고 키워드 매칭 시작 - 상품 ID: {}", usedIdx);

        Used used = usedRepository.findByUsedIdxAndIsUseTrue(usedIdx).orElse(null);
        if (used == null) {
            log.warn("[알림] 상품을 찾을 수 없음 - ID: {}", usedIdx);
            return;
        }

        List<UsedKeyword> allKeywords = usedKeywordRepository.findByIsUseTrue();
        log.info("[키워드] 활성화된 키워드 개수: {}", allKeywords.size());
        if (allKeywords.isEmpty())
            return;

        String searchText = (title + " " + content).toLowerCase();
        Set<Integer> matchedUserIdxs = new HashSet<>();

        for (UsedKeyword keyword : allKeywords) {
            String lowerKeyword = keyword.getKeyword().toLowerCase().trim();
            log.debug("[키워드 매칭 검사] searchText='{}', keyword='{}'", searchText, lowerKeyword);
            if (searchText.contains(lowerKeyword) && !keyword.getUserIdx().equals(used.getCreatedUser())) {
                matchedUserIdxs.add(keyword.getUserIdx());
                log.info("[키워드 매칭] 사용자 {} - 키워드 '{}'", keyword.getUserIdx(), keyword.getKeyword());
            }
        }

        if (matchedUserIdxs.isEmpty()) {
            log.info("[키워드] 매칭된 사용자 없음");
            return;
        }

        for (Integer userIdx : matchedUserIdxs) {
            try {
                String message = String.format("'%s' 상품이 당신의 관심 키워드와 매칭되었습니다!", used.getUsedTitle());
                Notification notification = Notification.builder()
                        .userIdx(userIdx)
                        .notiType(NotificationType.USED_KEYWORD_MATCH)
                        .refId(usedIdx)
                        .message(message)
                        .isRead(false)
                        .build();
                Notification saved = notificationRepository.save(notification);
                log.info("[알림 저장] 사용자 {} - 알림 ID: {}", userIdx, saved.getNotiIdx());

                sseService.notify(userIdx, NotificationType.USED_KEYWORD_MATCH.name(), saved.getNotiIdx(), message);
                log.info("[SSE 알림 전송] 사용자 {}", userIdx);
            } catch (Exception e) {
                log.error("[알림 전송 실패] 사용자 {}: {}", userIdx, e.getMessage(), e);
            }
        }

        log.info("[알림] 키워드 매칭 완료 - 알림 발송 사용자: {}", matchedUserIdxs.size());
    }

    // ===== 알림 조회/관리 =====

    /**
     * 사용자 알림 목록 조회 (논리삭제 제외)
     */
    @Transactional(readOnly = true)
    public List<NotificationRes> getNotifications(Integer userIdx) {

        List<Notification> notifications = notificationRepository.findActiveNotifications(userIdx);

        return notifications.stream()
                .sorted((n1, n2) -> {
                    // n1이 공지(0)면 앞으로 (-1), n2가 공지(0)면 뒤로 (1)
                    if (n1.getUserIdx() == 0 && n2.getUserIdx() != 0)
                        return -1;
                    if (n1.getUserIdx() != 0 && n2.getUserIdx() == 0)
                        return 1;
                    // 둘 다 공지거나 둘 다 일반이면 최신순 정렬
                    return n2.getCreatedAt().compareTo(n1.getCreatedAt());
                })
                .map(NotificationRes::from)
                .toList();
    }

    /**
     * 읽지 않은 알림 수
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Integer userIdx) {
        return notificationRepository.countByUserIdxAndIsReadFalseAndIsUseTrue(userIdx);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Integer notiIdx, Integer userIdx) {
        Notification notification = notificationRepository.findById(notiIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (!notification.getUserIdx().equals(userIdx)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        notification.markAsRead();
    }

    /**
     * 알림 논리 삭제
     */
    @Transactional
    public void deleteNotification(Integer notiIdx, Integer userIdx) {
        Notification notification = notificationRepository.findById(notiIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (!notification.getUserIdx().equals(userIdx)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        notification.delete();
    }
}
