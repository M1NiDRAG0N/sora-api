package com.scit.soragodong.domain.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record BoardReplyDto(
                Integer replyIdx,
                Integer boardIdx,
                Integer userIdx,
                String userNickname,
                String replyContent,
                Boolean isUse,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}
