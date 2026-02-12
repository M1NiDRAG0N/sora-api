package com.scit.soragodong.domain.dto;

import java.time.LocalDateTime;

public record UserOrderDto(
    Integer orderIndex,
    Integer userIdx,
    Integer productNum,
    Integer storeIdx,
    String productName,
    Integer totalPrice,
    Integer orderQuantity,
    LocalDateTime orderTime,
    Integer orderStatus,
    LocalDateTime cancelledAt,
    Integer cancelReason
) {
    
}