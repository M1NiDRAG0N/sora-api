package com.scit.soragodong.service;

import com.scit.soragodong.domain.dto.StoreDto;
import com.scit.soragodong.domain.dto.StoreProductDto;
import com.scit.soragodong.domain.entity.Store;
import com.scit.soragodong.domain.entity.StoreProduct;
import com.scit.soragodong.repository.StoreProductRepository;
import com.scit.soragodong.repository.StoreRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;

@Controller
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
        List<Store> stores = storeRepository.findByIsUse(1);
        
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
     * Entity -> DTO 변환
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
     * Entity -> DTO 변환
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

    //상점 등록용
    public boolean createStore(StoreDto storeDto) {
        Store store = new Store().builder()
                .storeName(storeDto.storeName())
                .storeAddress(storeDto.storeAddress())
                .storeOpenTime(storeDto.storeOpenTime())
                .storeCloseTime(storeDto.storeCloseTime())
                .build();
        storeRepository.save(store);

        return true;
    }

    public void updateIsUse(Integer idx, byte b) {
        Store store = storeRepository.findById(idx)
                    .orElseThrow(()-> new RuntimeException());
        store.setIsUse(b);
        storeRepository.save(store);

    }

    public void updateStoreName(Integer idx, String name) {
        Store store = storeRepository.findById(idx)
                .orElseThrow(()-> new RuntimeException());
        store.setStoreName(name);
        storeRepository.save(store);
    }
}
