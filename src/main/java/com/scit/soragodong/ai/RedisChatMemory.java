package com.scit.soragodong.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 AI 대화 히스토리 저장소
 * - Key: ai:chat:{conversationId} (Redis List)
 * - TTL: 24시간 (대화마다 갱신)
 * - 최대 100개 메시지 유지 (50회 대화)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisChatMemory implements ChatMemory {

    private static final String KEY_PREFIX = "ai:chat:";
    private static final long TTL_HOURS = 24;
    private static final int MAX_MESSAGES = 100;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        int saved = 0;
        for (Message message : messages) {
            try {
                String type = message.getMessageType().name();
                String content = message.getText();
                if (content == null || content.isBlank())
                    continue;
                long timestamp = System.currentTimeMillis();
                String json = objectMapper.writeValueAsString(new MessageRecord(type, content, timestamp));
                stringRedisTemplate.opsForList().rightPush(key, json);
                saved++;
            } catch (Exception e) {
                log.warn("[RedisChatMemory] 메시지 저장 실패: {}", e.getMessage());
            }
        }
        if (saved > 0) {
            stringRedisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
            Long size = stringRedisTemplate.opsForList().size(key);
            if (size != null && size > MAX_MESSAGES) {
                stringRedisTemplate.opsForList().trim(key, size - MAX_MESSAGES, -1);
            }
            log.info("[RedisChatMemory] 저장 완료 - conversationId={}, {}개", conversationId, saved);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        return get(conversationId, MAX_MESSAGES);
    }

    public List<Message> get(String conversationId, int lastN) {
        String key = KEY_PREFIX + conversationId;
        Long size = stringRedisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            log.info("[RedisChatMemory] 대화 이력 없음 - conversationId={}", conversationId);
            return List.of();
        }

        long start = Math.max(0, size - lastN);
        List<String> jsons = stringRedisTemplate.opsForList().range(key, start, -1);
        if (jsons == null)
            return List.of();

        List<Message> messages = new ArrayList<>();
        for (String json : jsons) {
            try {
                MessageRecord record = objectMapper.readValue(json, MessageRecord.class);
                if (record.content() == null || record.content().isBlank())
                    continue;
                messages.add(toMessage(record));
            } catch (Exception e) {
                log.warn("[RedisChatMemory] 메시지 역직렬화 실패: {}", e.getMessage());
            }
        }
        log.info("[RedisChatMemory] 대화 이력 로드 - conversationId={}, {}개", conversationId, messages.size());
        return messages;
    }

    /** 프론트엔드 이력 표출용: role + content + timestamp 반환 */
    public List<Map<String, Object>> getChatHistory(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        Long size = stringRedisTemplate.opsForList().size(key);
        if (size == null || size == 0)
            return List.of();

        long start = Math.max(0, size - MAX_MESSAGES);
        List<String> jsons = stringRedisTemplate.opsForList().range(key, start, -1);
        if (jsons == null)
            return List.of();

        List<Map<String, Object>> result = new ArrayList<>();
        for (String json : jsons) {
            try {
                MessageRecord record = objectMapper.readValue(json, MessageRecord.class);
                if (record.content() == null || record.content().isBlank())
                    continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("role", record.type().toLowerCase());
                item.put("content", record.content());
                item.put("timestamp", record.timestamp() != null ? record.timestamp() : System.currentTimeMillis());
                result.add(item);
            } catch (Exception e) {
                log.warn("[RedisChatMemory] 히스토리 로드 실패: {}", e.getMessage());
            }
        }
        return result;
    }

    @Override
    public void clear(String conversationId) {
        stringRedisTemplate.delete(KEY_PREFIX + conversationId);
        log.info("[RedisChatMemory] 대화 초기화 - conversationId={}", conversationId);
    }

    private Message toMessage(MessageRecord record) {
        return switch (record.type()) {
            case "ASSISTANT" -> new AssistantMessage(record.content());
            default -> new UserMessage(record.content());
        };
    }

    record MessageRecord(
            @JsonProperty("type") String type,
            @JsonProperty("content") String content,
            @JsonProperty("timestamp") Long timestamp) {
    }
}
