package com.scit.soragodong.domain.dto;


public record StoreProductDto(
    Integer productNum,
    Integer storeIdx,
    String storeName,
    String category,
    String productName,
    Integer price,
    Integer eventPrice,
    Integer productQuantity,
    String productPictureIdx
) 
{} 

