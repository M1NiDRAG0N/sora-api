package com.scit.soragodong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scit.soragodong.domain.entity.Store;

public interface  StoreRepository extends JpaRepository<Store, Integer>{
        /**
     * 사용 중인 가게 조회
     */
    List<Store> findByIsUse(Integer isUse);
    
    /**
     * 이벤트 상태별 가게 조회
     */
    List<Store> findByEventState(String eventState);
    
    /**
     * 가게명으로 검색
     */
    List<Store> findByStoreNameContaining(String storeName);
}
