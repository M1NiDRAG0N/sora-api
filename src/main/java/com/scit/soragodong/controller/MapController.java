package com.scit.soragodong.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scit.soragodong.domain.dto.StoreDto;
import com.scit.soragodong.domain.dto.UserDto;
import com.scit.soragodong.service.MapService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 구글맵 관련 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/map")
@Slf4j
public class MapController {

    private final MapService mapService;

    @Value("${google.maps.api-key:default-key}")
    private String googleMapsApiKey;

    /**
     * 구글맵 API 키 반환
     * GET /api/map/key
     */
    @GetMapping("/key")
    public ResponseEntity<String> getMapKey() {
        return ResponseEntity.ok(googleMapsApiKey);
    }

    /**
     * 전체 가게 마커 목록 반환
     * GET /api/map/stores
     */
    @GetMapping("/stores")
    public ResponseEntity<List<StoreDto>> getStoreMarkers() {
        log.info("[MapController] 전체 가게 마커 요청");

        List<StoreDto> markers = mapService.getAllStoreMarkers();
        return ResponseEntity.ok(markers);
    }

    /**
     * 특정 가게 마커 단건 반환
     * GET /api/map/stores/{storeIdx}
     */
    @GetMapping("/stores/{storeIdx}")
    public ResponseEntity<StoreDto> getStoreMarker(
            @PathVariable("storeIdx") Integer storeIdx) {
        log.info("[MapController] 가게 마커 단건 요청 - storeIdx: {}", storeIdx);

        StoreDto marker = mapService.getStoreMarker(storeIdx);

        if (marker == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(marker);
    }

    /**
     * 특정 유저 마커 반환
     * GET /api/map/user/{userIdx}
     */
    @GetMapping("/user/{userIdx}")
    public ResponseEntity<UserDto> getUserMarker(
            @PathVariable("userIdx") Integer userIdx) {
        log.info("[MapController] 유저 마커 요청 - userIdx: {}", userIdx);

        UserDto marker = mapService.getUserMarker(userIdx);

        if (marker == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(marker);
    }
}