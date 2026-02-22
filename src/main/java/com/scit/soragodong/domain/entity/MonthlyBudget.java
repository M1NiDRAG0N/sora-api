package com.scit.soragodong.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "MONTHLY_BUDGET")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MonthlyBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BUDGET_IDX") // [추가됨] DB 컬럼명과 정확히 매칭!
    private Integer budgetIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_IDX", nullable = false)
    private Users user;

    // 수정 후 (YEAR_MONTH 양옆에 백틱 ` 추가)
    @Column(name = "`YEAR_MONTH`", length = 10, nullable = false)
    private String yearMonth;

    @Column(name = "BUDGET_AMOUNT", nullable = false)
    private Integer budgetAmount;

    // 업데이트용 메서드
    public void updateAmount(Integer amount) {
        this.budgetAmount = amount;
    }
}