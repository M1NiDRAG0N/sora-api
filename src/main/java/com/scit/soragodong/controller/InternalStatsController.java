package com.scit.soragodong.controller;

import com.scit.soragodong.service.SseService;
import com.scit.soragodong.util.SystemResourceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/internal") // 관리자 서버가 이 주소로 찾아옵니다.
@Slf4j
@RequiredArgsConstructor
public class InternalStatsController {

    private final SseService sseService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(jakarta.servlet.http.HttpServletRequest request) {
        // 유틸리티를 호출해 현재 리소스 상태를 가져옵니다.
        request.getSession(true);


        Map<String, Object> stats = SystemResourceUtil.getSystemStats();
        stats.put("activeUsers", sseService.getConnectedUserCount());
        log.debug("보내는 데이터: " + stats);

        // JSON 형태로 반환합니다.
        return ResponseEntity.ok(stats);
    }
}
