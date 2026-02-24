package com.scit.soragodong.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.scit.soragodong.domain.entity.UsedLike;

import com.scit.soragodong.domain.entity.Used;

@Repository
public interface UsedLikeRepository extends JpaRepository<UsedLike, Integer> {

    /**
     * 특정 물품의 좋아요 수
     */
    int countByUsedIdx(Integer usedIdx);

    /**
     * 사용자의 좋아요 여부 확인
     */
    boolean existsByUsedIdxAndUserIdx(Integer usedIdx, Integer userIdx);

    /**
     * 사용자의 좋아요 조회
     */
    Optional<UsedLike> findByUsedIdxAndUserIdx(Integer usedIdx, Integer userIdx);

    /**
     * 특정 물품의 모든 찜 목록 (상태 변경 알림용)
     */
    List<UsedLike> findByUsedIdx(Integer usedIdx);

    /**
     * 사용자의 좋아요 삭제
     */
    void deleteByUsedIdxAndUserIdx(Integer usedIdx, Integer userIdx);

    /**
     * 사용자가 찜한 목록 조회 (Used 엔티티 직접 반환, 찜한 시간 내림차순)
     */
    @Query("SELECT u FROM Used u JOIN UsedLike ul ON u.usedIdx = ul.usedIdx WHERE ul.userIdx = :userIdx AND u.isUse = true ORDER BY ul.createdAt DESC")
    List<Used> findLikedUsedByUser(@Param("userIdx") Integer userIdx);

    /**
     * 사용자가 찜한 개수 조회
     */
    int countByUserIdx(Integer userIdx);
}
