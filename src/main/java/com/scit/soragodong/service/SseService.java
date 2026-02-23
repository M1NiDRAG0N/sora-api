package com.scit.soragodong.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE(Server-Sent Events)를 이용한 실시간 알림 서비스
 * 
 * 클라이언트가 /notifications/subscribe/{userId}로 연결하면
 * 서버에서 알림 발생 시 SseEmitter를 통해 실시간으로 전송
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SseService {
    
    // 사용자별 SseEmitter 저장소
    // Map<userId, SseEmitter>
    private final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    private static final long SSE_TIMEOUT = 60 * 60 * 1000L; // 1시간
    private static final String EVENT_NAME = "notification";
    
    /**
     * 사용자의 SSE 구독 생성
     */
    public SseEmitter subscribe(Integer userId) {
        log.info("[SSE] 구독 시작 - 사용자: {}", userId);
        
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.put(userId, emitter);
        
        // 연결 종료 콜백
        emitter.onCompletion(() -> {
            log.info("[SSE] 구독 종료 (완료) - 사용자: {}", userId);
            emitters.remove(userId);
        });
        
        // 타임아웃 콜백
        emitter.onTimeout(() -> {
            log.info("[SSE] 구독 타임아웃 - 사용자: {}", userId);
            emitters.remove(userId);
            emitter.complete();
        });
        
        // 에러 콜백 (클라이언트 연결 끊김은 정상 동작)
        emitter.onError(throwable -> {
            log.warn("[SSE] 연결 끊김 - 사용자: {}, 오류: {}", userId, throwable.getMessage());
            emitters.remove(userId);
            emitter.completeWithError(throwable);
        });
        
        // 초기 연결 확인용 더미 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(System.currentTimeMillis()))
                    .name("connected")
                    .data("SSE 연결 수립")
                    .build());
            log.info("[SSE] 초기 연결 이벤트 전송 - 사용자: {}", userId);
        } catch (IOException e) {
            log.error("[SSE] 초기 이벤트 전송 실패 - 사용자: {}: {}", userId, e.getMessage());
            emitters.remove(userId);
            throw new RuntimeException("SSE 연결 실패", e);
        }
        
        return emitter;
    }
    
    /**
     * 특정 사용자에게 알림 전송
     * 
     * @param userId 대상 사용자 ID
     * @param eventType 이벤트 타입 (예: USED_KEYWORD_MATCH)
     * @param referenceId 참조 ID (예: 물건 ID, 알림 ID)
     * @param message 알림 메시지
     */
    public void notify(Integer userId, String eventType, Integer referenceId, String message) {
        SseEmitter emitter = emitters.get(userId);
        
        if (emitter == null) {
            log.warn("[SSE] 연결된 사용자 없음 - 사용자: {}", userId);
            return;
        }
        
        try {
            // 알림 이벤트 데이터 구성
            String eventData = String.format(
                    "{\"type\":\"%s\",\"refId\":%d,\"message\":\"%s\",\"timestamp\":%d}",
                    eventType,
                    referenceId,
                    message.replace("\"", "\\\""),
                    System.currentTimeMillis()
            );
            
            // SSE 이벤트 전송
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(System.currentTimeMillis()))
                    .name(EVENT_NAME)
                    .data(eventData)
                    .build());
            
            log.info("[SSE] 알림 전송 성공 - 사용자: {}, 유형: {}, 메시지: {}", 
                    userId, eventType, message);
            
        } catch (IOException e) {
            log.error("[SSE] 알림 전송 실패 - 사용자: {}: {}", userId, e.getMessage());
            emitters.remove(userId);
            emitter.completeWithError(e);
        }
    }
    
    /**
     * 여러 사용자에게 일괄 알림 전송
     * 
     * @param userIds 대상 사용자 ID 리스트
     * @param eventType 이벤트 타입
     * @param referenceId 참조 ID
     * @param message 알림 메시지
     */
    public void notifyMultiple(List<Integer> userIds, String eventType, 
                               Integer referenceId, String message) {
        userIds.forEach(userId -> notify(userId, eventType, referenceId, message));
    }
    
    /**
     * 특정 사용자의 연결 상태 확인
     */
    public boolean isConnected(Integer userId) {
        return emitters.containsKey(userId);
    }
    
    /**
     * 연결된 사용자 수
     */
    public int getConnectedUserCount() {
        return emitters.size();
    }
    
    /**
     * 특정 사용자의 연결 강제 종료
     */
    public void disconnect(Integer userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("[SSE] 연결 강제 종료 - 사용자: {}", userId);
        }
    }
    
    /**
     * 모든 연결 종료
     */
    public void disconnectAll() {
        emitters.forEach((userId, emitter) -> {
            emitter.complete();
        });
        emitters.clear();
        log.info("[SSE] 모든 연결 종료 완료");
    }
	
	
}