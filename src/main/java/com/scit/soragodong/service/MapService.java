package com.scit.soragodong.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scit.soragodong.domain.dto.StoreDto;
import com.scit.soragodong.domain.dto.UserDto;
import com.scit.soragodong.domain.entity.Store;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.repository.StoreRepository;
import com.scit.soragodong.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 구글맵 마커 좌표 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MapService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    /**
     * 사용 중인 전체 가게 목록 반환 (좌표가 있는 가게만)
     */
    public List<StoreDto> getAllStoreMarkers() {
        log.info("[MapService] 전체 가게 마커 조회 시작");

        List<Store> stores = storeRepository.findByIsUse((byte) 1);

        List<StoreDto> result = stores.stream()
                .filter(s -> s.getStoreLat() != null && s.getStoreLng() != null)
                .map(this::convertToStoreDto)
                .collect(Collectors.toList());

        log.info("[MapService] 가게 마커 조회 완료 - {}개", result.size());
        return result;
    }

    /**
     * 특정 가게 단건 반환
     */
    public StoreDto getStoreMarker(Integer storeIdx) {
        log.info("[MapService] 가게 마커 단건 조회 - storeIdx: {}", storeIdx);

        Store store = storeRepository.findByStoreIdxAndIsUse(storeIdx, (byte) 1)
                .orElse(null);

        if (store == null || store.getStoreLat() == null || store.getStoreLng() == null) {
            log.warn("[MapService] 가게 또는 좌표 없음 - storeIdx: {}", storeIdx);
            return null;
        }

        return convertToStoreDto(store);
    }

    /**
     * 특정 유저 마커 반환 (좌표가 있는 경우만)
     */
    public UserDto getUserMarker(Integer userIdx) {
        log.info("[MapService] 유저 마커 조회 - userIdx: {}", userIdx);

        Users user = userRepository.findById(userIdx).orElse(null);

        if (user == null || user.getUserLat() == null || user.getUserLng() == null) {
            log.warn("[MapService] 유저 또는 좌표 없음 - userIdx: {}", userIdx);
            return null;
        }

        return convertToUserDto(user);
    }

    /**
     * Store 엔티티 → StoreDto 변환
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
     * Users 엔티티 → UserDto 변환
     */
    private UserDto convertToUserDto(Users user) {
        return new UserDto(
                user.getUserIdx(),
                user.getUserEmail(),
                null, // password 노출 방지
                user.getUserName(),
                user.getUserNickname(),
                user.getUserAddress(),
                user.getUserRole(),
                user.getUserBadge(),
                user.getProfileIdx(),
                user.getMannerScore(),
                user.getMonthlyBudget(),
                user.getUserLat(),
                user.getUserLng(),
                user.getIsUse(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}