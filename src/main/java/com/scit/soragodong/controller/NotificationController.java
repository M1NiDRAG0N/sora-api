package com.scit.soragodong.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.scit.soragodong.security.CustomUserDetails;
import com.scit.soragodong.domain.dto.NotificationRes;
import com.scit.soragodong.domain.response.ApiResponse;
import com.scit.soragodong.service.NotificationService;
import com.scit.soragodong.service.SseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 실시간 알림 관련 REST API
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final SseService sseService;
    
    /**
     * SSE 구독 엔드포인트
     * 
     * 클라이언트가 이 엔드포인트에 연결하면 서버에서 알림을 실시간으로 받을 수 있습니다.
     * 
     * GET /notifications/subscribe
     * 또는
     * GET /notifications/subscribe/{userId}
     * 
     * @return SseEmitter (클라이언트와의 실시간 연결)
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = Integer.parseInt(userDetails.getUsername());
        log.info("[SSE] 구독 요청 - 사용자: {}", userId);
        return sseService.subscribe(userId);
    }
    
    /**
     * SSE 구독 엔드포인트 (경로 매개변수 버전)
     * @param userId 사용자 ID
     */
    @GetMapping(value = "/subscribe/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeWithPathParam(@PathVariable(name = "userId") Integer userId) {
        log.info("[SSE] 구독 요청 - 사용자: {}", userId);
        return sseService.subscribe(userId);
    }
    
    /**
     * 연결 상태 확인
     * GET /notifications/status/{userId}
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<?> getConnectionStatus(@PathVariable(name = "userId") Integer userId) {
        boolean isConnected = sseService.isConnected(userId);
        return ResponseEntity.ok()
                .body(new java.util.HashMap<String, Object>() {{
                    put("userId", userId);
                    put("isConnected", isConnected);
                    put("connectedUsers", sseService.getConnectedUserCount());
                }});
    }
    
    /**
     * 연결 해제
     * GET /notifications/disconnect/{userId}
     */
    @GetMapping("/disconnect/{userId}")
    public ResponseEntity<?> disconnect(@PathVariable(name = "userId") Integer userId) {
        sseService.disconnect(userId);
        return ResponseEntity.ok()
                .body(new java.util.HashMap<String, Object>() {{
                    put("success", true);
                    put("message", "연결이 해제되었습니다.");
                }});
    }

    // ===== 알림 CRUD =====

    /**
     * 알림 목록 조회
     * GET /notifications
     */
    @GetMapping
    public ApiResponse<List<NotificationRes>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(notificationService.getNotifications(userDetails.getUserIdx()));
    }

    /**
     * 읽지 않은 알림 수
     * GET /notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(notificationService.getUnreadCount(userDetails.getUserIdx()));
    }

    /**
     * 알림 읽음 처리
     * PATCH /notifications/{notiIdx}/read
     */
    @PatchMapping("/{notiIdx}/read")
    public ApiResponse<?> markAsRead(
            @PathVariable Integer notiIdx,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAsRead(notiIdx, userDetails.getUserIdx());
        return ApiResponse.success("읽음 처리되었습니다.");
    }

    /**
     * 알림 삭제 (논리 삭제)
     * DELETE /notifications/{notiIdx}
     */
    @DeleteMapping("/{notiIdx}")
    public ApiResponse<?> deleteNotification(
            @PathVariable Integer notiIdx,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.deleteNotification(notiIdx, userDetails.getUserIdx());
        return ApiResponse.success("삭제되었습니다.");
    }
}