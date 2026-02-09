package com.scit.soragodong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scit.soragodong.domain.entity.StoreProduct;

public interface StoreProductRepository extends JpaRepository<StoreProduct, Integer> {
        /**
     * 이벤트 가격이 있는 상품 조회 (할인 상품)
     */
    List<StoreProduct> findByEventPriceIsNotNull();
    
    /**
     * 특정 가게의 상품 조회
     */
    List<StoreProduct> findByStoreStoreIdx(Integer storeIdx);
    
    /**
     * 카테고리별 상품 조회
     */
    List<StoreProduct> findByCategory(String category);
    // 특정 가게의 상품 조회

    // 특정 가게의 재고가 있는 상품만 조회
    List<StoreProduct> findByStoreStoreIdxAndProductQuantityGreaterThan(Integer storeIdx, Integer minQuantity);}
