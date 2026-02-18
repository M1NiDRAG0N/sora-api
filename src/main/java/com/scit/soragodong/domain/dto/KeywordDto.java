package com.scit.soragodong.domain.dto;

import java.time.LocalDateTime;

import com.scit.soragodong.domain.entity.UsedKeyword;

/**
 * 키워드 DTO
 */
public record KeywordDto(
    Integer keywordIdx,
    String keyword,
    LocalDateTime createdAt
) {
    public static KeywordDto from(UsedKeyword entity) {
        return new KeywordDto(
                entity.getKeywordIdx(),
                entity.getKeyword(),
                entity.getCreatedAt()
        );
    }
}
