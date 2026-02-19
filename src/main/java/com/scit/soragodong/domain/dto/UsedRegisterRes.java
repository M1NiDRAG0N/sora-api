package com.scit.soragodong.domain.dto;

import java.time.LocalDateTime;

import com.scit.soragodong.domain.entity.Used;

/**
 * 중고 물건 등록 응답 DTO
 */
public record UsedRegisterRes(
    Integer usedIdx,
    String usedTitle,
    String usedContent,
    Integer usedPrice,
    String tradingLoc,
    Integer notifiedUserCount,
    LocalDateTime createdAt
) {
    public static UsedRegisterRes from(Used used, Integer notifiedCount) {
        return new UsedRegisterRes(
                used.getUsedIdx(),
                used.getUsedTitle(),
                used.getUsedContent(),
                used.getUsedPrice(),
                used.getTradingLoc(),
                notifiedCount,
                used.getCreatedAt()
        );
    }
}
