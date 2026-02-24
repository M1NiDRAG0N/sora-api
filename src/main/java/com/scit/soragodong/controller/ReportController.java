package com.scit.soragodong.controller;

import com.scit.soragodong.domain.dto.ReportRequest;
import com.scit.soragodong.security.CustomUserDetails;
import com.scit.soragodong.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
	
	@GetMapping("/report")
	public String report(Model model){
		model.addAttribute("currentUri", "/report");
		return "common";
	}

    @PostMapping("/report/submit")
    @ResponseBody
    public ResponseEntity<?> submitReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ReportRequest request) {
        
        log.info("신고 접수: reporter={}, targetType={}, targetId={}", 
                userDetails.getUserIdx(), request.getTargetType(), request.getTargetId());
        
        try {
            reportService.submitReport(userDetails.getUserIdx(), request);
            return ResponseEntity.ok(Map.of("message", "신고가 정상적으로 접수되었습니다."));
        } catch (Exception e) {
            log.error("신고 제출 중 오류 발생", e);
            return ResponseEntity.internalServerError().body("신고 처리 중 오류가 발생했습니다.");
        }
    }
}
