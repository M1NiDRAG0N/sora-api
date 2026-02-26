package com.scit.soragodong.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scit.soragodong.domain.dto.StoreDto;
import com.scit.soragodong.domain.dto.StoreProductDto;
import com.scit.soragodong.domain.dto.UserOrderDto;
import com.scit.soragodong.domain.entity.ProductStockHistory;
import com.scit.soragodong.domain.entity.Store;
import com.scit.soragodong.domain.entity.StoreProduct;
import com.scit.soragodong.domain.entity.UserOrder;
import com.scit.soragodong.repository.ProductStockHistoryRepository;
import com.scit.soragodong.repository.StoreProductRepository;
import com.scit.soragodong.repository.StoreRepository;
import com.scit.soragodong.repository.UserOrderRepository;
import com.scit.soragodong.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 타임세일 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TimesaleService {

    private final StoreRepository storeRepository;
    private final StoreProductRepository storeProductRepository;
    private final UserOrderRepository userOrderRepository;
    private final ProductStockHistoryRepository stockHistoryRepository;
    private final UserRepository userRepository;

    /**
     * 할인 중인 상품 목록 조회 (이벤트 가격이 있는 상품)
     */
    public List<StoreProductDto> getDiscountProducts() {
        log.info("[getDiscountProducts] 할인 상품 목록 조회 시작");
        
        List<StoreProduct> products = storeProductRepository.findByEventPriceIsNotNull();
        
        log.info("[getDiscountProducts] 조회된 상품 수: {}", products.size());
        
        return products.stream()
                .map(this::convertToStoreProductDto)
                .collect(Collectors.toList());
    }

    /**
     * 이벤트 진행 중인 가게 목록 조회
     */
    public List<StoreDto> getEventStores() {
        log.info("[getEventStores] 이벤트 가게 목록 조회 시작");
        
        // 사용 중인 가게만 조회 (isUse = 1)
        List<Store> stores = storeRepository.findByIsUse((byte) 1);
        
        log.info("[getEventStores] 조회된 가게 수: {}", stores.size());
        
        return stores.stream()
                .map(this::convertToStoreDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 가게의 상세 정보 조회
     * 
     * @param storeIdx 가게 인덱스
     * @return 가게 상세 정보
     */
    public StoreDto getStoreDetail(Integer storeIdx) {
        log.info("[getStoreDetail] 가게 상세 정보 조회 시작 - storeIdx: {}", storeIdx);
        
        if (storeIdx == null) {
            log.error("[getStoreDetail] storeIdx가 null입니다");
            return null;
        }
        
        // 가게 조회 (사용 중인 가게만)
        Store store = storeRepository.findByStoreIdxAndIsUse(storeIdx, (byte) 1)
                .orElse(null);
        
        if (store == null) {
            log.warn("[getStoreDetail] 가게를 찾을 수 없음 - storeIdx: {}", storeIdx);
            return null;
        }
        
        log.info("[getStoreDetail] 가게 조회 성공 - storeName: {}", store.getStoreName());
        
        return convertToStoreDto(store);
    }

    /**
     * 특정 가게의 재고가 있는 상품 목록 조회
     * 
     * @param storeIdx 가게 인덱스
     * @return 재고가 있는 상품 목록
     */
    public List<StoreProductDto> getProductsByStoreWithStock(Integer storeIdx) {
        log.info("[getProductsByStoreWithStock] 가게 상품 목록 조회 시작 - storeIdx: {}", storeIdx);
        
        if (storeIdx == null) {
            log.error("[getProductsByStoreWithStock] storeIdx가 null입니다");
            return List.of();
        }
        
        // 재고가 0보다 큰 상품만 조회
        List<StoreProduct> products = storeProductRepository
                .findByStoreStoreIdxAndProductQuantityGreaterThan(storeIdx, 0);
        
        log.info("[getProductsByStoreWithStock] 조회된 상품 수: {}", products.size());
        
        return products.stream()
                .map(this::convertToStoreProductDto)
                .collect(Collectors.toList());
    }

    /**
     * 상품 단건 조회
     * 
     * @param productNum 상품 번호
     * @return 상품 상세 정보
     */
    public StoreProductDto getProductDetail(Integer productNum) {
        log.info("[getProductDetail] 상품 상세 정보 조회 시작 - productNum: {}", productNum);
        
        if (productNum == null) {
            log.error("[getProductDetail] productNum이 null입니다");
            return null;
        }
        
        // 상품 조회
        StoreProduct product = storeProductRepository.findById(productNum)
                .orElse(null);
        
        if (product == null) {
            log.warn("[getProductDetail] 상품을 찾을 수 없음 - productNum: {}", productNum);
            return null;
        }
        
        log.info("[getProductDetail] 상품 조회 성공 - productName: {}", product.getProductName());
        
        return convertToStoreProductDto(product);
    }

    /**
     * 상품 예약 처리
     * 
     * @param orderDto 주문 정보
     * @return 생성된 주문 정보
     */
    @Transactional
    public UserOrderDto reserveProduct(UserOrderDto orderDto) {
        log.info("[reserveProduct] 예약 처리 시작 - userIdx: {}, productNum: {}, quantity: {}", 
                orderDto.userIdx(), orderDto.productNum(), orderDto.orderQuantity());
        
        // 1. 상품 조회
        StoreProduct product = storeProductRepository.findById(orderDto.productNum())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        
        // 2. 재고 확인
        if (product.getProductQuantity() < orderDto.orderQuantity()) {
            log.error("[reserveProduct] 재고 부족 - 요청: {}, 재고: {}", 
                    orderDto.orderQuantity(), product.getProductQuantity());
            throw new IllegalStateException("재고가 부족합니다.");
        }
        
        // 3. 가게 조회
        Store store = storeRepository.findById(orderDto.storeIdx())
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다."));
        
        // 4. 재고 감소 (PESSIMISTIC LOCK 사용하여 동시성 제어)
        int updatedRows = storeProductRepository.decreaseStock(
                orderDto.productNum(), 
                orderDto.orderQuantity()
        );
        
        if (updatedRows == 0) {
            log.error("[reserveProduct] 재고 감소 실패 - 동시 예약으로 재고 부족");
            throw new IllegalStateException("재고가 부족합니다. 다시 시도해주세요.");
        }
        
        log.info("[reserveProduct] 재고 감소 완료 - productNum: {}, quantity: {}", 
                orderDto.productNum(), orderDto.orderQuantity());
        
        // 5. USER_ORDER 테이블에 주문 정보 저장
        UserOrder order = UserOrder.builder()
                .users(userRepository.findById(orderDto.userIdx())
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")))
                .storeProduct(product)
                .store(store)
                .productName(orderDto.productName())
                .totalPrice(orderDto.totalPrice())
                .orderQuantity(orderDto.orderQuantity())
                .orderTime(LocalDateTime.now())
                .orderStatus((byte) 0) // 0: 예약완료
                .build();
        
        UserOrder savedOrder = userOrderRepository.save(order);
        log.info("[reserveProduct] 주문 저장 완료 - orderIndex: {}", savedOrder.getOrderIndex());
        
        // 6. PRODUCT_STOCK_HISTORY 테이블에 출고 이력 저장
        ProductStockHistory stockHistory = ProductStockHistory.builder()
                .store(store)
                .storeProduct(product)
                .stockType((byte) 0) // 0: 출고
                .quantity(orderDto.orderQuantity())
                .receivingDate(LocalDateTime.now())
                .releasedDate(LocalDateTime.now())
                .note("타임세일 예약 - 주문번호: " + savedOrder.getOrderIndex())
                .build();
        
        stockHistoryRepository.save(stockHistory);
        log.info("[reserveProduct] 재고 이력 저장 완료");
        
        // 7. 결과 DTO 반환
        UserOrderDto resultDto = new UserOrderDto(
                savedOrder.getOrderIndex(),
                savedOrder.getStoreProduct().getProductNum(),
                savedOrder.getStore().getStoreIdx(),
                savedOrder.getUsers().getUserIdx(),
                savedOrder.getProductName(),
                savedOrder.getTotalPrice(),
                savedOrder.getOrderQuantity(),
                savedOrder.getOrderTime(),
                savedOrder.getOrderStatus().intValue(),
                savedOrder.getCancelledAt(),
                savedOrder.getCancelReason() != null ? savedOrder.getCancelReason().intValue() : null
        );
        
        log.info("[reserveProduct] 예약 처리 완료 - orderIndex: {}", resultDto.orderIndex());
        
        return resultDto;
    }

    /**
     * 사용자 예약 내역 조회
     *
     * @param userIdx 사용자 인덱스
     * @return 주문 DTO 목록 (최신순)
     */
    public List<UserOrderDto> getUserOrders(Integer userIdx) {
        log.info("[getUserOrders] 예약 내역 조회 - userIdx: {}", userIdx);
        return userOrderRepository.findByUsers_UserIdxOrderByOrderTimeDesc(userIdx)
                .stream()
                .map(this::convertToOrderDto)
                .collect(Collectors.toList());
    }

    private UserOrderDto convertToOrderDto(UserOrder order) {
        return new UserOrderDto(
                order.getOrderIndex(),
                order.getUsers().getUserIdx(),
                order.getStoreProduct().getProductNum(),
                order.getStore().getStoreIdx(),
                order.getProductName(),
                order.getTotalPrice(),
                order.getOrderQuantity(),
                order.getOrderTime(),
                order.getOrderStatus() != null ? order.getOrderStatus().intValue() : 0,
                order.getCancelledAt(),
                order.getCancelReason() != null ? order.getCancelReason().intValue() : null
        );
    }

    /**
     * Store 엔티티를 StoreDto로 변환
     */
    private StoreDto convertToStoreDto(Store store) {
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
     * StoreProduct 엔티티를 StoreProductDto로 변환
     */
    private StoreProductDto convertToStoreProductDto(StoreProduct product) {
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