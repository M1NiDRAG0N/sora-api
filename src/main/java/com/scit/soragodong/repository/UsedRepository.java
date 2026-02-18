package com.scit.soragodong.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.scit.soragodong.domain.entity.Used;

@Repository
public interface UsedRepository extends JpaRepository<Used, Integer> {

    /**
     * 활성화된 물품 목록 조회 (최신순)
     */
    Page<Used> findByIsUseTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 키워드 검색 (제목 또는 내용에 포함)
     */
    @Query("SELECT u FROM Used u WHERE u.isUse = true AND (u.usedTitle LIKE %:keyword% OR u.usedContent LIKE %:keyword%) ORDER BY u.createdAt DESC")
    Page<Used> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 특정 사용자의 물품 목록
     */
    Page<Used> findByCreatedUserAndIsUseTrueOrderByCreatedAtDesc(Integer createdUser, Pageable pageable);

    /**
     * 활성화된 물품 상세 조회
     */
    Optional<Used> findByUsedIdxAndIsUseTrue(Integer usedIdx);

    /**
     * 조회수 증가
     */
    @Modifying
    @Query("UPDATE Used u SET u.viewCount = u.viewCount + 1 WHERE u.usedIdx = :usedIdx")
    void incrementViewCount(@Param("usedIdx") Integer usedIdx);
}

