package com.scit.soragodong.domain.dto;

public record ChatMessageDto(
    Integer roomId,
    Integer senderIdx,
    String senderNickname,
    String message,
    String type,
    String createdAt
) {
    public ChatMessageDto withCreatedAt(String newCreatedAt) {
        return new ChatMessageDto(roomId, senderIdx, senderNickname, message, type, newCreatedAt);
    }
}
