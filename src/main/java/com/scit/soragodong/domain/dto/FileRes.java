package com.scit.soragodong.domain.dto;

public record FileRes(
        Integer fileIdx,
        String originalName,
        String fileUrl,
        Integer fileOrder
) {}
