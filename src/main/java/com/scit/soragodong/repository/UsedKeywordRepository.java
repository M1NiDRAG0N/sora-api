package com.scit.soragodong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.scit.soragodong.domain.entity.UsedKeyword;

@Repository
public interface UsedKeywordRepository extends JpaRepository<UsedKeyword, Integer> {

    /**
     * 활성화된 키워드 조회
     */
    List<UsedKeyword> findByIsUseTrue();

    /**
     * 특정 사용자의 키워드 조회
     */
    List<UsedKeyword> findByUserIdxAndIsUseTrue(Integer userIdx);

    /**
     * 특정 사용자의 키워드 개수
     */
    int countByUserIdxAndIsUseTrue(Integer userIdx);

    /**
     * 중복 키워드 확인
     */
    boolean existsByUserIdxAndKeywordAndIsUseTrue(Integer userIdx, String keyword);

    /**
     * 특정 키워드 조회 (사용자 + 키워드 ID)
     */
    java.util.Optional<UsedKeyword> findByKeywordIdxAndUserIdx(Integer keywordIdx, Integer userIdx);
}
