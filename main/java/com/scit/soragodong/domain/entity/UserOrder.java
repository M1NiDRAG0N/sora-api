package com.scit.soragodong.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "USER_ORDER")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_INDEX", nullable = false)
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_IDX", nullable = false)
    private Users userIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_NUM", nullable = false)
    private StoreProduct storeProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STORE_IDX", nullable = false)
    private Store store;

    @Column(name = "PRODUCT_NAME", nullable = false, length = 200)
    private String productName;

    @Column(name = "TOTAL_PRICE", nullable = false)
    private Integer totalPrice;

    @Column(name = "ORDER_QUANTITY", nullable = false)
    private Integer orderQuantity;

    @Column(name = "ORDER_TIME", nullable = false)
    private LocalDateTime orderTime;

    @Column(name = "ORDER_STATUS", nullable = false)
    @Builder.Default
    private Byte orderStatus = 0;

    @Column(name = "CANCELLED_AT")
    private LocalDateTime cancelledAt;

    @Column(name = "CANCEL_REASON")
    private Byte cancelReason;
}