package com.scit.soragodong.repository;

import com.scit.soragodong.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
	
	
	// NotificationRepository.java
	@Query("SELECT n FROM Notification n " +
			"WHERE (n.userIdx = :userIdx OR n.userIdx = 0) " +
			"AND n.isUse = true " +
			// userIdx가 0인 것(공지)을 0순위로, 나머지를 1순위로 둔 뒤 최신순 정렬
			"ORDER BY CASE WHEN n.userIdx = 0 THEN 0 ELSE 1 END ASC, n.createdAt DESC")
	List<Notification> findActiveNotifications(@Param("userIdx") Integer userIdx);
	
}
