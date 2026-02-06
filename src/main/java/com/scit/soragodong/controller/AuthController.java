package com.scit.soragodong.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

import com.scit.soragodong.domain.dto.UserDto;
import com.scit.soragodong.domain.response.ApiResponse;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.service.UserService;
import com.scit.soragodong.util.ValidationUtil;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    @Value("${google.maps.api-key:default-key}")
    private String googleMapsApiKey;
    
    private final UserService us;
    
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        return "auth/signup";
    }

    @PostMapping("/check-email")
    @ResponseBody
    public ApiResponse<?> checkEmail(@RequestBody UserDto userDto) {
        boolean isDuplicate = us.existsByUserEmail(userDto.userEmail());
        return ApiResponse.success(Map.of("isDuplicate", isDuplicate));
    }

    @PostMapping("/send-verification-code")
    @ResponseBody
    public ApiResponse<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        String email = request.get("userEmail");

        if (!ValidationUtil.isValid(email)) {
            throw new CustomException(ErrorCode.REQUIRED_VALUE_MISSING);
        }

        if (!ValidationUtil.isValidEmail(email)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        us.sendVerificationCode(email);
        return ApiResponse.success("인증 코드가 이메일로 발송되었습니다.");
    }

    @PostMapping("/verify-email")
    @ResponseBody
    public ApiResponse<?> verifyEmail(@RequestBody Map<String, String> request) {
        String email = request.get("userEmail");
        String verificationCode = request.get("verificationCode");

        if (!ValidationUtil.isValid(email) || !ValidationUtil.isValid(verificationCode)) {
            throw new CustomException(ErrorCode.REQUIRED_VALUE_MISSING);
        }

        us.verifyEmail(email, verificationCode);
        return ApiResponse.success("이메일이 인증되었습니다. 회원가입을 진행해주세요.");
    }

    @PostMapping("/signup")
    @ResponseBody
    public ApiResponse<?> signup(@RequestBody UserDto userDto) {
        // 필수 입력값 검증
        if (!ValidationUtil.isValid(userDto.userEmail()) || 
            !ValidationUtil.isValid(userDto.password()) ||
            !ValidationUtil.isValid(userDto.userName()) ||
            !ValidationUtil.isValid(userDto.userNickname()) ||
            !ValidationUtil.isValid(userDto.userAddress())) {
            throw new CustomException(ErrorCode.REQUIRED_VALUE_MISSING);
        }

        // 이메일 형식 검증
        if (!ValidationUtil.isValidEmail(userDto.userEmail())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 비밀번호 유효성 검증
        if (!ValidationUtil.isValidPassword(userDto.password())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 회원가입 (이메일 인증 확인)
        us.signup(userDto);
        return ApiResponse.success("회원가입이 완료되었습니다.");
    }

    @GetMapping("/find")
    public String findAccountPage() {
        return "auth/findAccount";
    }
    

}
