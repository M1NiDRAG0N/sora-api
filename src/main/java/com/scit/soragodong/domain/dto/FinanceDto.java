package com.scit.soragodong.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private Integer duration;
    private Integer financeIdx;
    private String category;
    private Long amount;
    private String type; // "plus" or "minus" (프론트 값과 일치시킴)
    private String memo;
    private String date; // "2026-01-21"
    private String time; // "11:30"
    private Integer day; // 21 (달력 매핑용)

    // [추가됨] 고정 지출 여부
    @JsonProperty("isFixed")
    private Boolean isFixed;

    // Entity -> DTO 변환 (화면으로 보낼 때)
    public static FinanceDto fromEntity(Finance entity) {
        // DB에 저장된 "+", "-" 기호를 다시 "plus", "minus"로 변환
        String typeStr = "+".equals(entity.getFinanceType()) ? "plus" : "minus";

        return FinanceDto.builder()
                .financeIdx(entity.getFinanceIdx()) // ★ 번호표(ID) 꼭 챙기기!
                .category(entity.getFinanceCategory())
                .amount(entity.getFinanceAmount())
                .type(typeStr) // ★ 변환된 값(plus/minus) 넣기
                .memo(entity.getFinanceMemo())
                .date(entity.getFinanceAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .time(entity.getFinanceAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                .day(entity.getFinanceAt().getDayOfMonth())
                .isFixed(entity.getIsFixed())
                .duration(entity.getDuration())
                .build();
    }
}