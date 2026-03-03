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

    @Tool(description = "가계부 내역을 등록합니다. date는 날짜(yyyy-MM-dd 형식, 예: 2026-03-03), time은 시간(HH:mm 형식, 예: 14:30), type은 반드시 'plus'(수입) 또는 'minus'(지출) 문자열. 날짜/시간이 없으면 오늘 날짜와 현재 시간을 사용하세요.")
    public String recordFinance(String date, String time, String category, String type, Long amount, String memo) {
        // AI가 date 자리에 type 값("plus"/"minus")을 넣은 경우 교정
        if ("plus".equals(date) || "minus".equals(date)) {
            if (!"plus".equals(type) && !"minus".equals(type)) {
                type = date;
            }
            date = null; // 기본값으로 재처리
        }

        // AI가 time 자리에 날짜(yyyy-MM-dd)를 넣은 경우 교정
        if (time != null && time.matches("\\d{4}-\\d{2}-\\d{2}")) {
            if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                date = time;
            }
            time = null; // 기본값으로 재처리
        }

        // date 형식 검증: yyyy-MM-dd 패턴이 아니면 오늘 날짜로
        if (date == null || date.isBlank() || !date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        // time 형식 검증: HH:mm 패턴이 아니면 현재 시간으로
        if (time == null || time.isBlank() || !time.matches("\\d{1,2}:\\d{2}")) {
            time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        }

        // 시간 형식 정규화: "9:00" → "09:00"
        String[] parts = time.split(":");
        time = String.format("%02d:%02d", Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));

        // type 정규화: 한국어나 기타 값 처리
        if (!"plus".equals(type) && !"minus".equals(type)) {
            type = "minus"; // 불명확한 경우 지출로 기본 처리
        }

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
