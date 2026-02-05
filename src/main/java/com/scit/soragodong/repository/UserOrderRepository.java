package com.scit.soragodong.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scit.soragodong.domain.entity.UserOrder;

public interface UserOrderRepository extends JpaRepository<UserOrder, Integer> {
    
}
