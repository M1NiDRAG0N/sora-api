package com.scit.soragodong.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.scit.soragodong.domain.dto.StoreDto;
import com.scit.soragodong.domain.dto.StoreProductDto;
import com.scit.soragodong.domain.dto.UserOrderDto;
import com.scit.soragodong.service.TimesaleService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Transactional
@Slf4j
@RequestMapping("/timesale")
public class TimeSaleController {
    
    private final TimesaleService timeSaleService;

    /**
     * 음식(상품) 리스트 조회
     */
    @GetMapping("/food/list")
    @ResponseBody
    public ResponseEntity<List<StoreProductDto>> getFoodList() {
        List<StoreProductDto> foodList = timeSaleService.getDiscountProducts();
        return ResponseEntity.ok(foodList);
    }

    /**
     * 가게 리스트 조회
     */
    @GetMapping("/store/list")
    @ResponseBody
    public ResponseEntity<List<StoreDto>> getStoreList() {
        List<StoreDto> storeList = timeSaleService.getEventStores();
        return ResponseEntity.ok(storeList);
    }
    
    /**
     * 가게 상세 정보 조회
     * 
     * @param storeIdx 가게 인덱스
     * @return 가게 상세 정보
     */
    @GetMapping("/detail/{storeIdx}")
    @ResponseBody
    public ResponseEntity<StoreDto> getStoreDetail(@PathVariable("storeIdx") Integer storeIdx) {
        log.info("가게 상세 정보 조회 요청 - storeIdx: {}", storeIdx);
        
        try {
            StoreDto storeDto = timeSaleService.getStoreDetail(storeIdx);
            
            if (storeDto == null) {
                log.warn("가게 정보를 찾을 수 없음 - storeIdx: {}", storeIdx);
                return ResponseEntity.notFound().build();
            }
            
            log.info("가게 상세 정보 조회 성공 - storeName: {}", storeDto.storeName());
            return ResponseEntity.ok(storeDto);
            
        } catch (Exception e) {
            log.error("가게 상세 정보 조회 중 오류 발생 - storeIdx: {}, error: {}", storeIdx, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 테스트용: 가게 정보 간단 조회 (디버깅용)
     */
    @GetMapping("/test/{storeIdx}")
    @ResponseBody
    public ResponseEntity<String> testStoreDetail(@PathVariable("storeIdx") Integer storeIdx) {
        log.info("테스트 요청 - storeIdx: {}", storeIdx);
        
        try {
            StoreDto storeDto = timeSaleService.getStoreDetail(storeIdx);
            
            if (storeDto == null) {
                return ResponseEntity.ok("가게를 찾을 수 없습니다. DB를 확인하세요.");
            }
            
            return ResponseEntity.ok("성공! 가게명: " + storeDto.storeName());
            
        } catch (Exception e) {
            log.error("테스트 실패:", e);
            return ResponseEntity.ok("에러 발생: " + e.getMessage());
        }
    }
    
    /**
     * 가게별 상품 목록 조회 (재고가 있는 상품만)
     * 
     * @param storeIdx 가게 인덱스
     * @return 상품 목록
     */
    @GetMapping("/detail/{storeIdx}/products")
    @ResponseBody
    public ResponseEntity<List<StoreProductDto>> getStoreProducts(@PathVariable("storeIdx") Integer storeIdx) {
        log.info("가게 상품 목록 조회 요청 - storeIdx: {}", storeIdx);
        
        try {
            List<StoreProductDto> products = timeSaleService.getProductsByStoreWithStock(storeIdx);
            
            log.info("가게 상품 목록 조회 성공 - storeIdx: {}, 상품 수: {}", storeIdx, products.size());
            return ResponseEntity.ok(products);
            
        } catch (Exception e) {
            log.error("가게 상품 목록 조회 중 오류 발생 - storeIdx: {}, error: {}", storeIdx, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 상품 상세 정보 조회 (예약 모달용)
     * 
     * @param productNum 상품 번호
     * @return 상품 상세 정보
     */
    @GetMapping("/product/{productNum}")
    @ResponseBody
    public ResponseEntity<StoreProductDto> getProductDetail(@PathVariable("productNum") Integer productNum) {
        log.info("상품 상세 정보 조회 요청 - productNum: {}", productNum);
        
        try {
            StoreProductDto product = timeSaleService.getProductDetail(productNum);
            
            if (product == null) {
                log.warn("상품 정보를 찾을 수 없음 - productNum: {}", productNum);
                return ResponseEntity.notFound().build();
            }
            
            log.info("상품 상세 정보 조회 성공 - productName: {}, storeIdx: {}", 
                product.productName(), product.storeIdx());
            return ResponseEntity.ok(product);
            
        } catch (Exception e) {
            log.error("상품 상세 정보 조회 중 오류 발생 - productNum: {}, error: {}", 
                productNum, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 상품 예약
     * 
     * @param request 예약 요청 정보
     * @return 예약 결과
     */
    @PostMapping("/reserve")
    @ResponseBody
    public ResponseEntity<UserOrderDto> createReservation(@RequestBody UserOrderDto request) {
        log.info("예약 요청 - userIdx: {}, productNum: {}, quantity: {}", 
            request.userIdx(), request.productNum(), request.orderQuantity());
        
        try {
            UserOrderDto order = timeSaleService.createReservation(request);
            
            log.info("예약 성공 - orderIndex: {}, productName: {}", 
                order.orderIndex(), order.productName());
            
            return ResponseEntity.ok(order);
            
        } catch (IllegalArgumentException e) {
            log.warn("예약 실패 (잘못된 요청) - {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            log.error("예약 중 오류 발생 - error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}