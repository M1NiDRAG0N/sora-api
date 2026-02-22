package com.scit.soragodong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.scit.soragodong.domain.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    
    /**
     * 사용자의 알림 조회
     */
    List<Notification> findByUserIdxOrderByCreatedAtDesc(Integer userIdx);
    
    /**
     * 사용자의 읽지 않은 알림 조회
     */
    List<Notification> findByUserIdxAndIsReadFalseOrderByCreatedAtDesc(Integer userIdx);
}
