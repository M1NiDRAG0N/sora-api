package com.scit.soragodong.domain.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record StoreDto(
    Integer storeIdx,
    String storeName,
    String storeAddress,
    LocalTime storeOpenTime,
    LocalTime storeCloseTime,
    LocalTime eventStartTime,
    LocalTime eventEndTime,
    String eventState,
    String eventNote,
    String storePictureIdx,
    Byte isUse,
    LocalDateTime createAt,
    Double storeLat,
    Double storeLng
) {}
