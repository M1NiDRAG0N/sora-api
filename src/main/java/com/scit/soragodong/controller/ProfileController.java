package com.scit.soragodong.controller;

import com.scit.soragodong.domain.dto.ProfileDto;
import com.scit.soragodong.domain.dto.ProfileUpdateDto;
import com.scit.soragodong.domain.dto.UserOrderDto;
import com.scit.soragodong.security.CustomUserDetails;
import com.scit.soragodong.service.ProfileService;
import com.scit.soragodong.service.TimesaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;
    private final TimesaleService timesaleService;

    // 프로필 HTML 조각 반환 (초기 로딩용 아님, 데이터는 JSON으로)
    @GetMapping("/view")
    public String getProfileView() {
        return "profile/profile :: div"; // profile.html의 th:fragment="div"
    }

    @GetMapping("/api/{userIdx}")
    @ResponseBody
    public ResponseEntity<ProfileDto> getProfileData(
            @PathVariable("userIdx") Integer targetUserIdx,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Integer loginUserIdx = userDetails.getUserIdx();
        ProfileDto profileDto = profileService.getProfile(targetUserIdx, loginUserIdx);
        
        return ResponseEntity.ok(profileDto);
    }
    
    // 내 프로필 정보
    @GetMapping("/api/me")
    @ResponseBody
    public ResponseEntity<ProfileDto> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Integer loginUserIdx = userDetails.getUserIdx();
        ProfileDto profileDto = profileService.getProfile(loginUserIdx, loginUserIdx);
        
        return ResponseEntity.ok(profileDto);
    }

    @PutMapping("/update")
    @ResponseBody
    public ResponseEntity<String> updateProfile(
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestPart("data") ProfileUpdateDto data,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        profileService.updateProfile(userDetails.getUserIdx(), data, profileImage);
        return ResponseEntity.ok("Profile updated successfully");
    }

    // 내 예약 내역 조회
    @GetMapping("/api/orders")
    @ResponseBody
    public ResponseEntity<List<UserOrderDto>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<UserOrderDto> orders = timesaleService.getUserOrders(userDetails.getUserIdx());
        return ResponseEntity.ok(orders);
    }

    // 내가 찜한 중고 물품 목록 조회
    @GetMapping("/api/likes/used")
    @ResponseBody
    public ResponseEntity<java.util.List<com.scit.soragodong.domain.dto.UsedListRes>> getMyLikedUsedItems(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        java.util.List<com.scit.soragodong.domain.dto.UsedListRes> list = profileService.getMyLikedUsedItems(userDetails.getUserIdx());
        return ResponseEntity.ok(list);
    }
}
