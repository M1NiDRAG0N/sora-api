package com.scit.soragodong.domain.enums;

public enum NotificationType {
    KEYWORD,
    LIKE,
    CHAT,
    USED_KEYWORD_MATCH,  // 중고 키워드 매칭
    USED_LIKE,           // 찜 알림 (판매자가 수신)
    USED_STATE_CHANGE,   // 찜한 상품 상태 변경 (찜한 사람이 수신)
    COMMUNITY_LIKE,      // 커뮤니티 좋아요
    COMMENT,              // 커뮤니티 댓글
	ADMIN_NOTICE,
	NOTICE
}