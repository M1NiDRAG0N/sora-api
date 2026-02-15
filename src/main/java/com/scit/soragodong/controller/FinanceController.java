package com.scit.soragodong.controller;

import com.scit.soragodong.domain.dto.FinanceDto;
import com.scit.soragodong.service.FinanceService;
// ▼ [팀원 코드에서 확인한 클래스 import]
import com.scit.soragodong.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    /**
     * [화면] 가계부 메인 페이지
     */
    @GetMapping("/finance")
    public String financePage(Model model) {
        model.addAttribute("currentUri", "/finance");
        return "common";
    }

    /**
     * [API] 가계부 작성 (저장)
     * 로그인한 유저 정보(CustomUserDetails)를 받아옵니다.
     */
    @PostMapping("/finance/write")
    @ResponseBody
    public ResponseEntity<String> writeFinance(
            @RequestBody FinanceDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 1. 로그인 체크
        if (userDetails == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        log.info("가계부 작성 요청: {}", dto);

        // 2. 세션에서 유저 ID 꺼내기 (CommunityController 참고함)
        Integer userIdx = userDetails.getUserIdx();
        log.info("작성자 ID: {}", userIdx);

        // 3. 서비스 실행
        financeService.write(dto, userIdx);

        return ResponseEntity.ok("저장되었습니다.");
    }

    /**
     * [API] 가계부 목록 조회
     */
    @GetMapping("/finance/list")
    @ResponseBody
    public ResponseEntity<List<FinanceDto>> getFinanceList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        // 로그인한 사람의 데이터만 가져오기
        Integer userIdx = userDetails.getUserIdx();
        List<FinanceDto> list = financeService.findAll(userIdx);

        return ResponseEntity.ok(list);
    }

    // FinanceController.java 에 추가

    @PostMapping("/finance/delete")
    public ResponseEntity<?> deleteFinance(@RequestBody FinanceDto dto) {
        try {
            financeService.delete(dto.getFinanceIdx());
            return ResponseEntity.ok("삭제 성공");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}