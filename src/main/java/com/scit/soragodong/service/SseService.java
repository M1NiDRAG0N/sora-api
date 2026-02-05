package com.scit.soragodong.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(60 * 1000L);
        emitters.put(userId, emitter);
        return emitter;
    }

    public void send(Long userId, String msg) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(msg);
            } catch (Exception e) {
                emitters.remove(userId);
            }
        }
    }
}