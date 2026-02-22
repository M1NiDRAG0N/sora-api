package com.scit.soragodong.domain.dto;

import java.util.List;

/**
 * 중고 물품 수정 요청 DTO
 */
public record UsedUpdateReq(
    String usedTitle,
    String usedContent,
    Integer usedPrice,
    String tradingLoc,
    Double tradingLatitude,
    Double tradingLongitude,
    List<Integer> deleteFileIdxs
) {}
