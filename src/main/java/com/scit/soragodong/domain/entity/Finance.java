package com.scit.soragodong.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import com.scit.soragodong.common.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "FINANCE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Finance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FINANCE_IDX")
    private Integer financeIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_IDX", nullable = false)
    private Users user;

    @Column(name = "FINANCE_CATEGORY", nullable = false, length = 50)
    private String financeCategory;

    @Column(name = "FINANCE_AMOUNT", nullable = false)
    private Long financeAmount;

    @Column(name = "FINANCE_TYPE", nullable = false, length = 1)
    private String financeType;

    @Column(name = "FINANCE_MEMO", length = 200)
    private String financeMemo;

    @Column(name = "FINANCE_AT", nullable = false)
    private LocalDateTime financeAt;

    public void updateFinance(String category, Long amount, String type, String memo, LocalDateTime at) {
        this.financeCategory = category;
        this.financeAmount = amount;
        this.financeType = type;
        this.financeMemo = memo;
        this.financeAt = at;
    }
}