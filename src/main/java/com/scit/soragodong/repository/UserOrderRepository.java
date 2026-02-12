package com.scit.soragodong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.scit.soragodong.domain.entity.UserOrder;

/**
 * 사용자 주문 Repository
 */
@Repository
public interface UserOrderRepository extends JpaRepository<UserOrder, Integer> {
    
    /**
     * 사용자별 주문 목록 조회
     */
List<UserOrder> findByUsers_UserIdxOrderByOrderTimeDesc(Integer userIdx);
    
    /**
     * 가게별 주문 목록 조회
     */
    List<UserOrder> findByStoreStoreIdxOrderByOrderTimeDesc(Integer storeIdx);
    
    /**
     * 주문 상태별 조회
     */
    List<UserOrder> findByOrderStatus(Byte orderStatus);
}