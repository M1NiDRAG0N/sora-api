package com.scit.soragodong.controller;

import com.scit.soragodong.domain.dto.BudgetUpdateDto;
import com.scit.soragodong.domain.dto.FinanceDto;
import com.scit.soragodong.service.FinanceService;
import com.scit.soragodong.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
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
    @Value("${google.maps.api-key:default-key}")
    private String googleMapsApiKey;
    private final FinanceService financeService;

    /**
     * [화면] 가계부 메인 페이지
     */
    @GetMapping("/finance")
    public String financePage(Model model) {
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        model.addAttribute("currentUri", "/finance");
        return "common";
    }

    /**
     * [API] 가계부 작성 (저장)
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

        // 2. 세션에서 유저 ID 꺼내기
        Integer userIdx = userDetails.getUserIdx();
        log.info("작성자 ID: {}", userIdx);

        // 3. 서비스 실행
        financeService.write(dto, userIdx);

        return ResponseEntity.ok("저장되었습니다.");
    }

    /**
     * [API] 가계부 목록 및 예산 조회 (월별 예산 적용)
     */
    @GetMapping("/finance/list")
    @ResponseBody
    public ResponseEntity<?> getFinanceList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "yearMonth", required = false) String yearMonth) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        Integer userIdx = userDetails.getUserIdx();

        // 1. 가계부 리스트 가져오기
        List<FinanceDto> list = financeService.findAll(userIdx);

        // 2. [변경됨] 해당 월(yearMonth)의 예산만 DB에서 가져오기
        // ★ 여기서 Integer를 Long으로 수정
        Long budget = financeService.getBudget(userIdx, yearMonth);

        // 3. 리스트와 예산을 Map에 담아서 한 번에 응답
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("list", list);
        response.put("budget", budget);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/finance/delete")
    public ResponseEntity<?> deleteFinance(@RequestBody FinanceDto dto) {
        try {
            financeService.delete(dto.getFinanceIdx());
            return ResponseEntity.ok("삭제 성공");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * [API] 한 달 예산 설정 (월별 예산 적용)
     */
    @PostMapping("/finance/budget/update")
    @ResponseBody
    public ResponseEntity<String> updateBudget(
            @RequestBody BudgetUpdateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 1. 로그인 체크
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            // 2. 유저 ID 꺼내기
            Integer userIdx = userDetails.getUserIdx();
            log.info("예산 설정 요청 - 유저 ID: {}, 월: {}, 금액: {}", userIdx, dto.getYearMonth(), dto.getAmount());

            // 3. 서비스 실행 시 yearMonth(어느 달인지)도 같이 넘겨줌
            financeService.updateBudget(userIdx, dto.getYearMonth(), dto.getAmount());

            return ResponseEntity.ok("예산이 설정되었습니다.");
        } catch (Exception e) {
            log.error("예산 설정 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("예산 설정 실패");
        }
    }

}