package com.scit.soragodong.controller;

import com.scit.soragodong.domain.enums.NotificationType;
import com.scit.soragodong.service.NoticeService;
import com.scit.soragodong.service.NotificationService;
import com.scit.soragodong.service.SseService;
import com.scit.soragodong.util.SystemResourceUtil;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.Map;

@RestController
@RequestMapping("/api/internal") // 관리자 서버가 이 주소로 찾아옵니다.
@Slf4j
@RequiredArgsConstructor
public class InternalStatsController {

    private final SseService sseService;
    private final NotificationService notificationService;
    private final NoticeService noticeService;
    private final DataSource dataSource;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = SystemResourceUtil.getSystemStats();
        stats.put("activeUsers", sseService.getConnectedUserCount());

        // HikariCP 커넥션 풀 상태
        if (dataSource instanceof HikariDataSource hikariDs) {
            var poolBean = hikariDs.getHikariPoolMXBean();
            if (poolBean != null) {
                stats.put("hikari_active", poolBean.getActiveConnections());
                stats.put("hikari_idle", poolBean.getIdleConnections());
                stats.put("hikari_pending", poolBean.getThreadsAwaitingConnection());
                stats.put("hikari_total", poolBean.getTotalConnections());
            }
        }

        log.debug("보내는 데이터: " + stats);

        // JSON 형태로 반환합니다.
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/broadcast")
    public ResponseEntity<String> broadcast(@RequestBody Map<String, Object> payload) {
        try {
            // 관리자 서버가 보낸 데이터 추출 (userIdx와 message 두 파라미터만 사용)
            Integer userIdx = (Integer) payload.get("userIdx");
            String message = (String) payload.get("message");

            log.info("[내부 알림 요청] 대상: {}, 메시지: {}", userIdx, message);

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Message is required");
            }

            // 1. 상세 보기를 위한 Notice 생성
            String title = message.length() > 20 ? message.substring(0, 20) + "..." : message;
            Integer noticeIdx = noticeService.createNotice(title, message);

            // 2. 대상에 따른 알림 전송
            if (userIdx != null && userIdx > 0) {
                // 특정 사용자(신고자 또는 피신고자)에게 알림 전송 (DB 저장 + SSE)
                notificationService.send(userIdx, NotificationType.REPORT_RESULT, noticeIdx, message);
                return ResponseEntity.ok("Notification sent to user " + userIdx);
            } else {
                // userIdx가 null이거나 0이면 모든 사용자에게 브로드캐스트 (DB 저장 + SSE)
                notificationService.broadcast(NotificationType.ADMIN_NOTICE, noticeIdx, message);
                return ResponseEntity.ok("Broadcast notification sent successfully");
            }
        } catch (Exception e) {
            log.error("알림 중계 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
