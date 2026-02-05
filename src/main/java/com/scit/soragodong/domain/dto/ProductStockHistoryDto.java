package com.scit.soragodong.domain.dto;

import java.time.LocalDateTime;

public record ProductStockHistoryDto(
        Integer historyIdx,
        Integer productNum,
        Integer stockType,
        Integer quantity,
        LocalDateTime receivingDate,
        LocalDateTime releasedDate,
        String note,
        LocalDateTime createAt) {
}
