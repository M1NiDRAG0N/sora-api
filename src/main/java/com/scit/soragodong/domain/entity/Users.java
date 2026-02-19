package com.scit.soragodong.domain.entity;

import com.scit.soragodong.common.BaseEntity;
import com.scit.soragodong.domain.enums.UserRole;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "USERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Users extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_IDX")
    private Integer userIdx;

    @Column(name = "USER_EMAIL", length = 200, nullable = false)
    private String userEmail;

    @Column(name = "PASSWORD", length = 300, nullable = false)
    private String password;

    @Column(name = "USER_NAME", length = 50, nullable = false)
    private String userName;

    @Column(name = "USER_NICKNAME", length = 50, nullable = false)
    private String userNickname;

    @Column(name = "USER_ADDRESS", length = 500, nullable = false)
    private String userAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "USER_ROLE", length = 10, nullable = false)
    private UserRole userRole;

    @Column(name = "USER_BADGE", length = 50)
    private String userBadge;

    @Column(name = "PROFILE_IDX")
    private Integer profileIdx;

    @Column(name = "MANNER_SCORE")
    private Integer mannerScore;

    @Column(name = "MONTHLY_BUDGET")
    private Integer monthlyBudget;

    @Column(name = "USER_LAT", nullable = false)
    private Double userLat;

    @Column(name = "USER_LNG", nullable = false)
    private Double userLng;

    @Column(name = "IS_USE", nullable = false)
    private Boolean isUse;

    // [추가] 예산(MonthlyBudget) 수정 전용 메서드
    public void updateMonthlyBudget(Integer amount) {
        this.monthlyBudget = amount;
    }

}
