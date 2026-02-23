package com.scit.soragodong.domain.dto;

import lombok.Builder;

/**
 * 프로필 수정 요청용 DTO
 */
@Builder
public record ProfileUpdateDto(
        String nickname,
        String address,
        Double lat,
        Double lng,
        String currentPassword, // 현재 비밀번호 (본인 확인용)
        String newPassword      // 변경할 비밀번호 (선택)
) {}
