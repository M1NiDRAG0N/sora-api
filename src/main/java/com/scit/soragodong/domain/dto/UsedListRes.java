package com.scit.soragodong.domain.dto;

import java.time.LocalDateTime;

import com.scit.soragodong.domain.entity.Used;
import com.scit.soragodong.domain.enums.UsedState;

/**
 * 중고 물품 목록 응답 DTO
 */
public record UsedListRes(
    Integer usedIdx,
    String usedTitle,
    Integer usedPrice,
    String tradingLoc,
    UsedState usedState,
    Integer viewCount,
    Integer likeCount,
    Integer chatCount,
    String thumbnailPath,
    LocalDateTime createdAt
) {
    public static UsedListRes from(Used used, String thumbnailPath, Integer likeCount, Integer chatCount) {
        return new UsedListRes(
                used.getUsedIdx(),
                used.getUsedTitle(),
                used.getUsedPrice(),
                used.getTradingLoc(),
                used.getUsedState(),
                used.getViewCount(),
                likeCount != null ? likeCount : 0,
                chatCount != null ? chatCount : 0,
                thumbnailPath,
                used.getCreatedAt()
        );
    }

    public static UsedListRes from(Used used, String thumbnailPath) {
        return from(used, thumbnailPath, 0, 0);
    }
}
