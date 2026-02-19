package com.scit.soragodong.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BudgetUpdateDto {
    private Integer amount;
    private String yearMonth; // 프론트에서 보낸 "2026-02" 를 받을 변수 추가
}