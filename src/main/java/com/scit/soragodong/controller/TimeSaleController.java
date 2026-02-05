package com.scit.soragodong.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.scit.soragodong.domain.dto.StoreDto;
import com.scit.soragodong.domain.dto.StoreProductDto;
import com.scit.soragodong.service.TimesaleService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;



@Controller
@RequiredArgsConstructor
@Transactional
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
    
}
