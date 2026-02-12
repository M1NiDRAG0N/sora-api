package com.scit.soragodong.domain.entity;

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
import lombok.Setter;

@Entity
@Table(name = "STORE_PRODUCT")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StoreProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_NUM", nullable = false)
    private Integer productNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STORE_IDX", nullable = false)
    private Store store;

    @Column(name = "CATEGORY", length = 50)
    private String category;

    @Column(name = "PRODUCT_NAME", nullable = false, length = 200)
    private String productName;

    @Column(name = "PRICE", nullable = false)
    private Integer price;

    @Column(name = "EVENT_PRICE")
    private Integer eventPrice;

    @Column(name = "PRODUCT_QUANTITY", nullable = false)
    private Integer productQuantity;

    @Column(name = "PRODUCT_PICTURE_IDX", length = 100)
    private String productPictureIdx;
}