package com.scit.soragodong.controller;

import com.scit.soragodong.domain.dto.StoreDto;
import com.scit.soragodong.domain.dto.StoreProductDto;
import com.scit.soragodong.domain.dto.UserDto;
import com.scit.soragodong.domain.dto.UserOrderDto;
import com.scit.soragodong.repository.UserRepository;
import com.scit.soragodong.service.TimesaleService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Transactional
@Slf4j
@RequestMapping("/timesale")
public class TimeSaleController {
    
    private final TimesaleService timeSaleService;
    private final UserRepository userRepository;

    @Value("${spring.google.maps.api-key:default-key}")
    private String googleMapsApiKey;

    /**
     * 타임세일 전용 구글맵 API 키 반환
     */
    @GetMapping("/map/key")
    @ResponseBody
    public ResponseEntity<String> getMapKey() {
        log.info("구글맵 API 키 요청 - key: {}", googleMapsApiKey);
        return ResponseEntity.ok(googleMapsApiKey);
    }
    
    /**
     * 타임세일 메인 페이지
     */
    @GetMapping("")
    public String getTimesalePage(Model model) {
        model.addAttribute("currentUri", "/timesale");
        return "common";
    }
    /**
     * 타임세일 상세 페이지
     */
    @GetMapping("/detail")
    public String getTimesaleDetailPage(
            @RequestParam(value = "storeIdx", required = true) Integer storeIdx,
            Model model) {
        
        log.info("타임세일 상세 페이지 요청 - storeIdx: {}", storeIdx);
        
        // storeIdx를 모델에 추가
        model.addAttribute("storeIdx", storeIdx);
        
        // currentUri를 정확하게 설정 (common.html에서 사용)
        model.addAttribute("currentUri", "/timesale/detail");
        
        return "common";
    }

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
     * 지도용 유저 마커 조회
     * URL: /timesale/map/user/{userIdx}
     */
    @GetMapping("/map/user/{userIdx}")
    @ResponseBody
    public ResponseEntity<UserDto> getUserMarker(@PathVariable("userIdx") Integer userIdx) {
        log.info("유저 마커 요청 - userIdx: {}", userIdx);
        try {
            com.scit.soragodong.domain.entity.Users user = userRepository.findById(userIdx).orElse(null);
            if (user == null || user.getUserLat() == null || user.getUserLng() == null) {
                return ResponseEntity.notFound().build();
            }
            UserDto dto = new UserDto(
                user.getUserIdx(), user.getUserEmail(), null,
                user.getUserName(), user.getUserNickname(), user.getUserAddress(),
                user.getUserRole(), user.getUserBadge(), user.getProfileIdx(),
                user.getMannerScore(), user.getMonthlyBudget(),
                user.getUserLat(), user.getUserLng(),
                user.getIsUse(), null, null
            );
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("유저 마커 조회 오류 - userIdx: {}, error: {}", userIdx, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
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
     * 가게 상세 정보 조회 (API용)
     * URL: /timesale/detail/{storeIdx}
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
     * 가게별 상품 목록 조회 (재고가 있는 상품만)
     * URL: /timesale/detail/{storeIdx}/products
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
     * 상품 단건 조회 (예약 모달용)
     * URL: /timesale/product/{productNum}
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
            
            log.info("상품 상세 정보 조회 성공 - productName: {}", product.productName());
            return ResponseEntity.ok(product);
            
        } catch (Exception e) {
            log.error("상품 상세 정보 조회 중 오류 발생 - productNum: {}, error: {}", productNum, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 상품 예약 처리
     * URL: POST /timesale/reserve
     * 
     * @param orderDto 주문 정보
     * @return 생성된 주문 정보
     */
    @PostMapping("/reserve")
    @ResponseBody
    public ResponseEntity<?> reserveProduct(@RequestBody UserOrderDto orderDto) {
        log.info("상품 예약 요청 - userIdx: {}, productNum: {}, quantity: {}", 
                orderDto.userIdx(), orderDto.productNum(), orderDto.orderQuantity());
        
        try {
            UserOrderDto result = timeSaleService.reserveProduct(orderDto);
            
            log.info("상품 예약 성공 - orderIndex: {}", result.orderIndex());
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.error("잘못된 요청 - error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            
        } catch (IllegalStateException e) {
            log.error("예약 실패 (재고 부족 등) - error: {}", e.getMessage());
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
            
        } catch (Exception e) {
            log.error("상품 예약 중 오류 발생 - error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "시스템 오류가 발생했습니다."));
        }
    }
}