package com.scit.soragodong.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scit.soragodong.domain.entity.Store;

public interface  StoreRepository extends JpaRepository<Store, Integer>{
   /**
     * 가게 인덱스와 사용 여부로 가게 조회
     * 
     * @param storeIdx 가게 인덱스
     * @param isUse 사용 여부 (1: 사용, 0: 미사용)
     * @return 가게 정보
     */
    Optional<Store> findByStoreIdxAndIsUse(Integer storeIdx, Byte isUse);
    
    /**
     * 사용 여부로 가게 목록 조회
     * 
     * @param isUse 사용 여부 (1: 사용, 0: 미사용)
     * @return 가게 목록
     */
    List<Store> findByIsUse(Byte isUse);
    
    /**
     * 이벤트 상태로 가게 목록 조회
     * 
     * @param eventState 이벤트 상태 (예: "진행중")
     * @return 가게 목록
     */
    List<Store> findByEventState(String eventState);
}
