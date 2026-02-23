package com.scit.soragodong.ai;

import com.scit.soragodong.domain.dto.FinanceDto;
import com.scit.soragodong.service.FinanceService;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;
import java.util.stream.Collectors;

public class FinanceTool {

    private final FinanceService financeService;
    private final Integer userIdx;

    public FinanceTool(FinanceService financeService, Integer userIdx) {
        this.financeService = financeService;
        this.userIdx = userIdx;
    }

    @Tool(description = "가계부 내역을 등록합니다. type은 'plus'(수입) 또는 'minus'(지출). date 형식: yyyy-MM-dd, time 형식: HH:mm. 날짜/시간이 없으면 오늘 날짜와 현재 시간을 사용하세요.")
    public String recordFinance(String date, String time, String category, String type, Long amount, String memo) {
        FinanceDto dto = FinanceDto.builder()
                .date(date)
                .time(time)
                .category(category)
                .type(type)
                .amount(amount)
                .memo(memo)
                .isFixed(false)
                .build();
        financeService.write(dto, userIdx);
        String typeLabel = "plus".equals(type) ? "수입" : "지출";
        return String.format("[완료] %s %,d원 등록 (날짜: %s %s, 카테고리: %s, 메모: %s)",
                typeLabel, amount, date, time, category, memo);
    }

    @Tool(description = "가계부 내역 목록 전체를 조회합니다")
    public List<FinanceDto> getFinanceList() {
        return financeService.findAll(userIdx);
    }

    @Tool(description = "특정 월의 수입/지출 요약을 조회합니다. yearMonth 형식: yyyy-MM (예: 2026-02)")
    public String summarizeMonth(String yearMonth) {
        List<FinanceDto> thisMonth = financeService.findAll(userIdx).stream()
                .filter(f -> f.getDate() != null && f.getDate().startsWith(yearMonth))
                .collect(Collectors.toList());

        long income = thisMonth.stream()
                .filter(f -> "plus".equals(f.getType()))
                .mapToLong(FinanceDto::getAmount)
                .sum();

        long expense = thisMonth.stream()
                .filter(f -> "minus".equals(f.getType()))
                .mapToLong(FinanceDto::getAmount)
                .sum();

        return String.format("%s 요약 - 수입: %,d원 / 지출: %,d원 / 잔액: %,d원 (총 %d건)",
                yearMonth, income, expense, income - expense, thisMonth.size());
    }

    @Tool(description = "월별 예산을 설정합니다. yearMonth 형식: yyyy-MM (예: 2026-02)")
    public String setBudget(String yearMonth, Long amount) {
        financeService.updateBudget(userIdx, yearMonth, amount);
        return String.format("[완료] %s 예산 %,d원으로 설정되었습니다.", yearMonth, amount);
    }

    @Tool(description = "가계부 내역을 삭제합니다. financeIdx는 내역 ID입니다.")
    public String deleteFinance(Integer financeIdx) {
        financeService.delete(financeIdx);
        return String.format("[완료] 가계부 내역(ID: %d)이 삭제되었습니다.", financeIdx);
    }
}
