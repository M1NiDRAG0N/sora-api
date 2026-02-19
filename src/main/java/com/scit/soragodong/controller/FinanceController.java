package com.scit.soragodong.controller;

import com.scit.soragodong.domain.dto.BudgetUpdateDto;
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
     * [API] 가계부 목록 및 예산 조회 (월별 예산 적용)
     */
    @GetMapping("/finance/list")
    @ResponseBody
    public ResponseEntity<?> getFinanceList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String yearMonth) { // [추가] 프론트에서 넘어온 연-월 받기

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        Integer userIdx = userDetails.getUserIdx();

        // 1. 가계부 리스트 가져오기
        List<FinanceDto> list = financeService.findAll(userIdx);

        // 2. [변경됨] 해당 월(yearMonth)의 예산만 DB에서 가져오기
        Integer budget = financeService.getBudget(userIdx, yearMonth);

        // 3. 리스트와 예산을 Map에 담아서 한 번에 응답
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("list", list);
        response.put("budget", budget);

        return ResponseEntity.ok(response);
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
    // 상단 import 영역에 아래 줄 추가 (경로는 본인 프로젝트에 맞게 확인)
    // import com.scit.soragodong.domain.dto.BudgetUpdateDto;

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

            // 3. [변경됨] 서비스 실행 시 yearMonth(어느 달인지)도 같이 넘겨줌
            financeService.updateBudget(userIdx, dto.getYearMonth(), dto.getAmount());

            return ResponseEntity.ok("예산이 설정되었습니다.");
        } catch (Exception e) {
            log.error("예산 설정 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("예산 설정 실패");
        }
    }
}