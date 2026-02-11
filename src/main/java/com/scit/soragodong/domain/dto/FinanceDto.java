package com.scit.soragodong.domain.dto;

import com.scit.soragodong.domain.entity.Finance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceDto {
    private Integer financeIdx;
    private String category;
    private Long amount;
    private String type; // "inc" or "exp"
    private String memo;
    private String date; // "2026-01-21"
    private String time; // "11:30"
    private Integer day; // 21 (달력 매핑용)

    // Entity -> DTO 변환 편의 메서드
    public static FinanceDto fromEntity(Finance entity) {
        return FinanceDto.builder()
                .financeIdx(entity.getFinanceIdx())
                .category(entity.getFinanceCategory())
                .amount(entity.getFinanceAmount())
                .type(entity.getFinanceType())
                .memo(entity.getFinanceMemo())
                // 날짜 포맷팅 (YYYY-MM-DD)
                .date(entity.getFinanceAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                // 시간 포맷팅 (HH:mm)
                .time(entity.getFinanceAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                // 일자 추출 (달력 매핑용)
                .day(entity.getFinanceAt().getDayOfMonth())
                .build();
    }
}