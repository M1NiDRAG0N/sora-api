package com.scit.soragodong.service;

import java.util.Map;
import java.util.Random;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.scit.soragodong.domain.dto.UserDto;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.domain.enums.UserRole;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
@Transactional
public class UserService {

    private final UserRepository ur;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final EmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String VERIFICATION_CODE_PREFIX = "signup:";
    private static final String PASSWORD_RESET_PREFIX = "reset:";
    private static final long VERIFICATION_CODE_EXPIRY = 10; // 10분
    
    public boolean existsByUserEmail(String userEmail) {
        return ur.existsByUserEmail(userEmail);
    }
    
    public boolean existsByUserNickname(String userNickname) {
        return ur.existsByUserNickname(userNickname);
    }
    
    /**
     * 이메일 인증 코드 발송
     */
    public void sendVerificationCode(String email) {
        // 이메일 중복 확인
        if (ur.existsByUserEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        
        // 인증 코드 생성
        String verificationCode = generateVerificationCode();
        
        // Redis에 저장 (10분 유효)
        String redisKey = VERIFICATION_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(redisKey, verificationCode, VERIFICATION_CODE_EXPIRY, TimeUnit.MINUTES);
        
        // 이메일 발송
        emailService.sendVerificationEmail(email, verificationCode);
    }
    
    /**
     * 이메일 인증 검증
     */
    public void verifyEmail(String email, String verificationCode) {
        String redisKey = VERIFICATION_CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        
        if (storedCode == null) {
            throw new CustomException(ErrorCode.VERIFICATION_EXPIRED);
        }
        
        if (!storedCode.equals(verificationCode)) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION);
        }
        
        // 검증 완료 표시 (key 유지, value 변경)
        redisTemplate.opsForValue().set(redisKey, "verified", VERIFICATION_CODE_EXPIRY, TimeUnit.MINUTES);
    }
    
    /**
     * 회원가입 (이메일 인증 완료 후 실행)
     */
    public void signup(UserDto userDto) {
        // 이메일 인증 확인
        String redisKey = VERIFICATION_CODE_PREFIX + userDto.userEmail();
        String verificationStatus = redisTemplate.opsForValue().get(redisKey);
        
        if (verificationStatus == null) 
            throw new CustomException(ErrorCode.NOT_FOUND_VERIFICATION);
        
        if (!"verified".equals(verificationStatus))
            throw new CustomException(ErrorCode.INVALID_VERIFICATION);
        
        // 이메일 중복 재확인
        if (ur.existsByUserEmail(userDto.userEmail()))
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);

        // 비밀번호 암호화
        String encryptedPassword = bCryptPasswordEncoder.encode(userDto.password());

        // Users 엔티티 생성
        Users user = Users.builder()
            .userEmail(userDto.userEmail())
            .password(encryptedPassword)
            .userName(userDto.userName())
            .userNickname(userDto.userNickname())
            .userAddress(userDto.userAddress())
            .userLat(userDto.userLat())
            .userLng(userDto.userLng())
            .userRole(UserRole.USER)
            .isUse(true)
            .mannerScore(100)
            .monthlyBudget(0)
            .build();

        // 저장
        Users savedUser = ur.save(user);
        
        // Redis 인증 코드 삭제
        redisTemplate.delete(redisKey);
        
        // 환영 이메일 발송
        emailService.sendWelcomeEmail(savedUser.getUserEmail(), savedUser.getUserName());
    }
    
    /**
     * 이메일 인증 상태 및 남은 TTL 조회
     * status: "none" | "pending" | "verified"
     */
    public Map<String, Object> checkVerificationStatus(String email) {
        String redisKey = VERIFICATION_CODE_PREFIX + email;
        String value = redisTemplate.opsForValue().get(redisKey);
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        long remaining = (ttl != null && ttl > 0) ? ttl : 0;

        if (value == null) {
            return Map.of("status", "none", "remainingSeconds", 0);
        } else if ("verified".equals(value)) {
            return Map.of("status", "verified", "remainingSeconds", remaining);
        } else {
            return Map.of("status", "pending", "remainingSeconds", remaining);
        }
    }

    /**
     * 6자리 인증 코드 생성
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
    
    /**
     * 비밀번호 리셋 코드 발송 (기존 회원용)
     */
    public void sendPasswordResetCode(String email) {
        // 이메일 존재 확인
        Users user = ur.findByUserEmail(email)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        // 인증 코드 생성
        String verificationCode = generateVerificationCode();
        
        // Redis에 저장 (10분 유효)
        String redisKey = PASSWORD_RESET_PREFIX + email;
        redisTemplate.opsForValue().set(redisKey, verificationCode, VERIFICATION_CODE_EXPIRY, TimeUnit.MINUTES);
        
        // 이메일 발송
        emailService.sendVerificationEmail(email, verificationCode);
    }
    
    /**
     * 비밀번호 리셋 코드 검증
     */
    public void verifyPasswordResetCode(String email, String verificationCode) {
        String redisKey = PASSWORD_RESET_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        
        if (storedCode == null) {
            throw new CustomException(ErrorCode.VERIFICATION_EXPIRED);
        }
        
        if (!storedCode.equals(verificationCode)) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION);
        }
        
        // 검증 완료 표시
        redisTemplate.opsForValue().set(redisKey, "verified", VERIFICATION_CODE_EXPIRY, TimeUnit.MINUTES);
    }
    
    /**
     * 비밀번호 리셋 (이메일 인증 후)
     */
    public void resetPassword(String email, String newPassword) {
        // 이메일 인증 확인
        String redisKey = PASSWORD_RESET_PREFIX + email;
        String verificationStatus = redisTemplate.opsForValue().get(redisKey);
        
        if (verificationStatus == null) {
            throw new CustomException(ErrorCode.VERIFICATION_EXPIRED);
        }
        
        if (!"verified".equals(verificationStatus)) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION);
        }
        
        // 사용자 존재 확인
        Users user = ur.findByUserEmail(email)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        // 비밀번호 암호화 및 업데이트
        String encryptedPassword = bCryptPasswordEncoder.encode(newPassword);
        ur.updatePassword(email, encryptedPassword);
        
        // Redis 인증 코드 삭제
        redisTemplate.delete(redisKey);
    }}