package com.scit.soragodong.domain.dto;

import java.time.LocalDateTime;
import com.scit.soragodong.domain.enums.UserRole;

public record UserDto(
    Integer userIdx,
    String userEmail,
    String password,
    String userName,
    String userNickname,
    String userAddress,
    UserRole userRole,
    String userBadge,
    Integer profileIdx,
    Integer mannerScore,
    Integer monthlyBudget,
    Double userLat,
    Double userLng,
    Boolean isUse,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
