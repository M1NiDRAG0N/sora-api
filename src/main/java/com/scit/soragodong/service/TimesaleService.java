package com.scit.soragodong.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.scit.soragodong.domain.dto.StoreDto;
import com.scit.soragodong.domain.dto.StoreProductDto;
import com.scit.soragodong.domain.entity.Store;
import com.scit.soragodong.domain.entity.StoreProduct;
import com.scit.soragodong.repository.StoreProductRepository;
import com.scit.soragodong.repository.StoreRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
@Transactional
public class TimesaleService {

    private final StoreProductRepository storeProductRepository;
    private final StoreRepository storeRepository;

    /**
     * 모든 상품(음식) 데이터 조회
     */
    public List<StoreProductDto> getAllProducts() {
        List<StoreProduct> products = storeProductRepository.findAll();
        
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 할인 상품만 조회 (이벤트 가격이 있는 경우)
     */
    public List<StoreProductDto> getDiscountProducts() {
        List<StoreProduct> products = storeProductRepository.findByEventPriceIsNotNull();
        
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 가게의 상품 조회
     */
    public List<StoreProductDto> getProductsByStore(Integer storeIdx) {
        List<StoreProduct> products = storeProductRepository.findByStoreStoreIdx(storeIdx);

        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 가게의 재고가 있는 상품만 조회
     * 
     * @param storeIdx 가게 인덱스
     * @return 재고가 있는 상품 목록
     */
    public List<StoreProductDto> getProductsByStoreWithStock(Integer storeIdx) {
        log.debug("가게 상품 목록 조회 시작 (재고 있는 상품만) - storeIdx: {}", storeIdx);
        
        // 재고가 0보다 큰 상품만 조회
        List<StoreProduct> products = storeProductRepository.findByStoreStoreIdxAndProductQuantityGreaterThan(storeIdx, 0);
        
        log.debug("재고 있는 상품 조회 완료 - storeIdx: {}, 상품 수: {}", storeIdx, products.size());
        
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 상품 상세 정보 조회 (예약 모달용)
     * 
     * @param productNum 상품 번호
     * @return 상품 상세 정보
     */
    public StoreProductDto getProductDetail(Integer productNum) {
        log.debug("상품 상세 정보 조회 시작 - productNum: {}", productNum);
        
        StoreProduct product = storeProductRepository.findById(productNum)
                .orElse(null);
        
        if (product == null) {
            log.warn("상품을 찾을 수 없음 - productNum: {}", productNum);
            return null;
        }
        
        // 재고가 없으면 null 반환
        if (product.getProductQuantity() <= 0) {
            log.warn("재고가 없는 상품 - productNum: {}, 재고: {}", productNum, product.getProductQuantity());
            return null;
        }
        
        StoreProductDto productDto = convertToDto(product);
        
        log.debug("상품 상세 정보 조회 완료 - productName: {}", productDto.productName());
        return productDto;
    }
    
    /**
     * 모든 가게 데이터 조회
     */
    public List<StoreDto> getAllStores() {
        List<Store> stores = storeRepository.findAll();
        
        return stores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 사용 중인 가게만 조회 (isUse = 1)
     */
    public List<StoreDto> getActiveStores() {
        List<Store> stores = storeRepository.findByIsUse((byte) 1);
        
        return stores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 이벤트 진행 중인 가게만 조회
     */
    public List<StoreDto> getEventStores() {
        List<Store> stores = storeRepository.findByEventState("진행중");
        
        return stores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 가게 상세 정보 조회
     * 
     * @param storeIdx 가게 인덱스
     * @return 가게 상세 정보 DTO
     */
    public StoreDto getStoreDetail(Integer storeIdx) {
        log.debug("가게 상세 정보 조회 시작 - storeIdx: {}", storeIdx);
        
        // 가게 정보 조회 (사용 중인 가게만)
        Store store = storeRepository.findByStoreIdxAndIsUse(storeIdx, (byte) 1)
                .orElse(null);
        
        if (store == null) {
            log.warn("사용 가능한 가게를 찾을 수 없음 - storeIdx: {}", storeIdx);
            return null;
        }
        
        // 기존 convertToDto 메서드 활용
        StoreDto storeDto = convertToDto(store);
        
        log.debug("가게 상세 정보 조회 완료 - storeName: {}", storeDto.storeName());
        return storeDto;
    }

    /**
     * Entity -> DTO 변환 (Store)
     */
    private StoreDto convertToDto(Store store) {
        return new StoreDto(
            store.getStoreIdx(),
            store.getStoreName(),
            store.getStoreAddress(),
            store.getStoreOpenTime(),
            store.getStoreCloseTime(),
            store.getEventStartTime(),
            store.getEventEndTime(),
            store.getEventState(),
            store.getEventNote(),
            store.getStorePictureIdx(),
            store.getIsUse(),
            store.getCreateAt(),
            store.getStoreLat(),
            store.getStoreLng()
        );
    }

    /**
     * Entity -> DTO 변환 (StoreProduct)
     */
    private StoreProductDto convertToDto(StoreProduct product) {
        return new StoreProductDto(
            product.getProductNum(),
            product.getStore().getStoreIdx(),
            product.getStore().getStoreName(),
            product.getCategory(),
            product.getProductName(),
            product.getPrice(),
            product.getEventPrice(),
            product.getProductQuantity(),
            product.getProductPictureIdx()
        );
    }
}