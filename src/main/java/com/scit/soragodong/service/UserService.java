package com.scit.soragodong.service;

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

@Service
@AllArgsConstructor
@Transactional
public class UserService {

    private final UserRepository ur;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final EmailService emailService;
    
    public boolean existsByUserEmail(String userEmail) {
        return ur.existsByUserEmail(userEmail);
    }
    
    /**
     * 회원가입
     */
    public void signup(UserDto userDto) {
        // 이메일 중복 확인
        if (ur.existsByUserEmail(userDto.userEmail())) throw new CustomException(ErrorCode.DUPLICATE_EMAIL);

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
        
        // 웰컴 이메일 발송
        emailService.sendWelcomeEmail(savedUser.getUserEmail(), savedUser.getUserName());
    }
}
