package com.scit.soragodong.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.scit.soragodong.domain.entity.LikeCount;
import com.scit.soragodong.domain.entity.LikeCountKey;

import java.util.List;

@Repository
public interface LikeCountRepository extends JpaRepository<LikeCount, LikeCountKey> {

    // 특정 유저가 좋아요한 내역 조회
    @Query("SELECT l FROM LikeCount l WHERE l.id.userIdx = :userIdx")
    List<LikeCount> findByUserId(@Param("userIdx") Integer userIdx);

    // 특정 게시글의 좋아요 수 카운트 (필요 시)
    @Query("SELECT count(l) FROM LikeCount l WHERE l.id.boardIdx = :boardIdx")
    long countByBoardId(@Param("boardIdx") Integer boardIdx);
}
