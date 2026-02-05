package com.scit.soragodong.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "PRODUCT_STOCK_HISTORY")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ProductStockHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HISTORY_IDX", nullable = false)
    private Integer historyIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_NUM", nullable = false)
    private StoreProduct storeProduct;

    @Column(name = "STOCK_TYPE", nullable = false)
    @Builder.Default
    private Byte stockType = 0;

    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;

    @Column(name = "RECEIVING_DATE", nullable = false)
    private LocalDateTime receivingDate;

    @Column(name = "RELEASED_DATE")
    private LocalDateTime releasedDate;

    @Column(name = "NOTE", length = 200)
    private String note;

    @CreationTimestamp
    @Column(name = "CREATE_AT", nullable = false, updatable = false)
    private LocalDateTime createAt;
}