package com.scit.soragodong.domain.dto;

import lombok.Builder;

import java.util.List;

/**
 * Java Record (JDK 16+)
 * - 불변성(Immutable) 보장
 * - getter, toString, equals, hashCode 자동 생성
 * - 보일러플레이트 코드 제거
 */
@Builder
public record ProfileDto(
        Integer userIdx,
        String nickname,
        String description,
        String profileImg,
        Integer mannerScore,

        // 통계
        Integer postCount,
        Integer commentCount,
        Integer likeCount,

        // 본인 여부
        boolean myProfile,

        // 리스트 데이터
        List<ProfileItemDto> posts,
        List<ProfileItemDto> comments,
        List<ProfileItemDto> likes) {
    // 내부 Inner Record
    @Builder
    public record ProfileItemDto(
            Integer id, // boardIdx
            String title,
            String category,
            String contentPreview,
            String timeAgo,
            String price, // null for community
            String thumbnail, // file path
            String type // 'market', 'community', 'reply'
    ) {
    }
}
