package com.scit.soragodong.domain.dto;

public record ChatRoomListDto(
    Integer chatRoomIdx,
    String type,
    Integer otherUserProfileIdx,
    String otherUserNickname,
    String lastMessage,
    String lastMessageAt,
    int unreadCount,
    String productImg
) {}
