package com.scit.soragodong.domain.dto;

public record ChatRoomCreateDto(
    String type,
    Integer otherUserIdx,
    Integer usedIdx
) {}
