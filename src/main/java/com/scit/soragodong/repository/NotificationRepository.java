package com.scit.soragodong.repository;

import com.scit.soragodong.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    
    /**
     * 사용자의 유효한 알림 목록 (논리삭제 제외, 최신순)
     */
    List<Notification> findByUserIdxAndIsUseTrueOrderByCreatedAtDesc(Integer userIdx);

    /**
     * 읽지 않은 알림 수
     */
    long countByUserIdxAndIsReadFalseAndIsUseTrue(Integer userIdx);
}
