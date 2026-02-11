package com.scit.soragodong.domain.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record BoardDto(
        Integer boardIdx,
        Integer userIdx,
        String boardCategory,
        String boardTitle,
        String boardContent,
        String userNickname,
        Boolean isUse,
        Integer likeCount,
        Integer viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer fileGrpIdx,
        Integer replyCount) {
}