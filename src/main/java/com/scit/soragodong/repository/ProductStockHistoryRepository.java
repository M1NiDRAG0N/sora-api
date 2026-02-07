package com.scit.soragodong.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scit.soragodong.domain.entity.ProductStockHistory;

public interface ProductStockHistoryRepository extends JpaRepository<ProductStockHistory, Integer> {
    
}
