package com.scit.soragodong.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.scit.soragodong.domain.entity.Used;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.domain.enums.UsedState;

/**
 * 중고 물품 상세 응답 DTO
 */
public record UsedDetailRes(
    Integer usedIdx,
    String usedTitle,
    String usedContent,
    Integer usedPrice,
    String tradingLoc,
    Double tradingLat,
    Double tradingLng,
    UsedState usedState,
    Integer viewCount,
    Integer likeCount,
    Integer chatCount,
    Boolean isLiked,
    Boolean isOwner,
    Integer createdUser,
    String sellerNickname,
    Integer sellerProfileIdx,
    Integer sellerMannerScore,
    String sellerAddress,
    List<String> images,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static UsedDetailRes from(Used used, List<String> images, Integer likeCount,
                                      Boolean isLiked, Boolean isOwner, Users seller,
                                      Integer chatCount, Integer viewCount) {
        return new UsedDetailRes(
                used.getUsedIdx(),
                used.getUsedTitle(),
                used.getUsedContent(),
                used.getUsedPrice(),
                used.getTradingLoc(),
                used.getTradingLat(),
                used.getTradingLng(),
                used.getUsedState(),
                viewCount,
                likeCount,
                chatCount,
                isLiked,
                isOwner,
                used.getCreatedUser(),
                seller != null ? seller.getUserNickname() : "알 수 없음",
                seller != null ? seller.getProfileIdx() : null,
                seller != null ? seller.getMannerScore() : null,
                seller != null ? seller.getUserAddress() : null,
                images,
                used.getCreatedAt(),
                used.getUpdatedAt()
        );
    }
}
