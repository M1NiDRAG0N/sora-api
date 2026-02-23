package com.scit.soragodong.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 조회수 중복 방지 공통 서비스 (Redis Set 기반)
 *
 * 동작 원리:
 *   SADD viewed:{type}:{refId}  {viewerKey}
 *   - 처음 조회 시 → SET에 추가(1 반환) → DB viewCount 증가
 *   - 이미 조회 시 → 중복 무시(0 반환) → DB 증가 안 함
 *   - TTL 24시간 → 같은 사용자도 다음 날 재조회 카운트
 *
 * 사용 예:
 *   viewCountService.record("USED", usedIdx, "u123")   // 중고거래
 *   viewCountService.record("BOARD", boardIdx, "u123") // 커뮤니티
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCountService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final Duration VIEW_TTL = Duration.ofHours(24);

    /**
     * 조회를 기록하고 새 조회 여부를 반환한다.
     *
     * @param type      컨텐츠 타입 식별자 (예: "USED", "BOARD")
     * @param refId     컨텐츠 PK
     * @param viewerKey 조회자 식별자 - 로그인: "u{userId}", 비로그인: "s{sessionId}"
     * @return true  → 새로운 조회 (DB viewCount 증가 필요)
     *         false → 이미 조회한 기록 있음 (증가 생략)
     */
    public boolean record(String type, Integer refId, String viewerKey) {
        String key = String.format("viewed:%s:%d", type, refId);
        Long added = stringRedisTemplate.opsForSet().add(key, viewerKey);
        if (added != null && added > 0) {
            // 첫 조회 → TTL 설정 (중복 EXPIRE 호출 최소화를 위해 새 조회 시에만)
            stringRedisTemplate.expire(key, VIEW_TTL);
            log.debug("[ViewCount] 새 조회 기록 - type={}, refId={}, viewer={}", type, refId, viewerKey);
            return true;
        }
        log.debug("[ViewCount] 중복 조회 무시 - type={}, refId={}, viewer={}", type, refId, viewerKey);
        return false;
    }
}
