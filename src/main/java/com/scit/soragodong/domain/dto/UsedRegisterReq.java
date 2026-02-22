package com.scit.soragodong.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 중고 물건 등록 요청 DTO
 */
public record UsedRegisterReq(
    @NotBlank(message = "제목은 필수입니다")
    String usedTitle,
    
    @NotBlank(message = "설명은 필수입니다")
    String usedContent,
    
    @NotNull(message = "가격은 필수입니다")
    Integer usedPrice,
    
    String tradingLoc,
    
    Double tradingLatitude,
    
    Double tradingLongitude,
    
    @NotNull(message = "사용자 정보는 필수입니다")
    Integer createdUser
) {}
