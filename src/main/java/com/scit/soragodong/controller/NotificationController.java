package com.scit.soragodong.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    public SseEmitter subscribeWithPathParam(@PathVariable Integer userId) {
        log.info("[SSE] 구독 요청 - 사용자: {}", userId);
        return sseService.subscribe(userId);
    }
    
    /**
     * 연결 상태 확인
     * GET /notifications/status/{userId}
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<?> getConnectionStatus(@PathVariable Integer userId) {
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
     * POST /notifications/disconnect/{userId}
     */
    @GetMapping("/disconnect/{userId}")
    public ResponseEntity<?> disconnect(@PathVariable Integer userId) {
        sseService.disconnect(userId);
        return ResponseEntity.ok()
                .body(new java.util.HashMap<String, Object>() {{
                    put("success", true);
                    put("message", "연결이 해제되었습니다.");
                }});
    }
}