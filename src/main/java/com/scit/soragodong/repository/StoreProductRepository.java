package com.scit.soragodong.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    
    /**
     * 특정 가게의 재고가 있는 상품만 조회
     */
    List<StoreProduct> findByStoreStoreIdxAndProductQuantityGreaterThan(Integer storeIdx, Integer minQuantity);
    
    /**
     * 상품 번호로 조회 (재고 확인용)
     */
    Optional<StoreProduct> findById(Integer productNum);

    /**
     * 상품 + 가게 JOIN FETCH 조회 (별도 store 쿼리 제거용)
     */
    @Query("SELECT p FROM StoreProduct p JOIN FETCH p.store WHERE p.productNum = :productNum")
    Optional<StoreProduct> findByIdWithStore(@Param("productNum") Integer productNum);
    
    /**
     * 재고 감소 (예약 시)
     * 
     * @param productNum 상품 번호
     * @param quantity 감소시킬 수량
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE StoreProduct p SET p.productQuantity = p.productQuantity - :quantity WHERE p.productNum = :productNum AND p.productQuantity >= :quantity")
    int decreaseStock(@Param("productNum") Integer productNum, @Param("quantity") Integer quantity);
}