package com.scit.soragodong.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.scit.soragodong.domain.entity.ProductStockHistory;

/**
 * 상품 재고 이력 Repository
 */
@Repository
public interface ProductStockHistoryRepository extends JpaRepository<ProductStockHistory, Integer> {
    
}