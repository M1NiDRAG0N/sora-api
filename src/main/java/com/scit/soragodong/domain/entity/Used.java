package com.scit.soragodong.domain.entity;

import com.scit.soragodong.common.BaseEntity;
import com.scit.soragodong.domain.enums.UsedState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "USED")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Used extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USED_IDX")
    private Integer usedIdx;

    @Column(name = "USED_TITLE", length = 500, nullable = false)
    private String usedTitle;

    @Column(name = "USED_CONTENT", columnDefinition = "TEXT", nullable = false)
    private String usedContent;

    @Column(name = "USED_PRICE", nullable = false)
    private Integer usedPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "USED_STATE", length = 20)
    private UsedState usedState;

    @Column(name = "TRADING_LOC", length = 500)
    private String tradingLoc;

    @Column(name = "TRADING_LAT")
    private Double tradingLat;

    @Column(name = "TRADING_LNG")
    private Double tradingLng;

    @Column(name = "VIEW_COUNT")
    private Integer viewCount;

    @Column(name = "CREATED_USER", nullable = false)
    private Integer createdUser;

    @Column(name = "IS_USE")
    private Boolean isUse;
}