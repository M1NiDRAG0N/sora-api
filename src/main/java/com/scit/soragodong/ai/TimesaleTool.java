package com.scit.soragodong.ai;

import com.scit.soragodong.domain.dto.StoreDto;
import com.scit.soragodong.domain.dto.StoreProductDto;
import com.scit.soragodong.domain.dto.UserOrderDto;
import com.scit.soragodong.service.TimesaleService;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;

public class TimesaleTool {

    private final TimesaleService timesaleService;
    private final Integer userIdx;

    public TimesaleTool(TimesaleService timesaleService, Integer userIdx) {
        this.timesaleService = timesaleService;
        this.userIdx = userIdx;
    }

    @Tool(description = "현재 할인 중인 타임세일 상품 목록을 조회합니다. 이벤트 가격(eventPrice)이 있는 상품만 반환됩니다.")
    public List<StoreProductDto> getDiscountProducts() {
        return timesaleService.getDiscountProducts();
    }

    @Tool(description = "타임세일 이벤트를 진행 중인 가게 목록을 조회합니다.")
    public List<StoreDto> getEventStores() {
        return timesaleService.getEventStores();
    }

    @Tool(description = "특정 가게의 재고가 있는 상품 목록을 조회합니다.")
    public List<StoreProductDto> getStoreProducts(Integer storeIdx) {
        return timesaleService.getProductsByStoreWithStock(storeIdx);
    }

    @Tool(description = "타임세일 상품을 예약합니다. productNum은 상품 번호, storeIdx는 가게 번호, quantity는 주문 수량입니다. 가격은 자동 계산됩니다.")
    public String reserveProduct(Integer productNum, Integer storeIdx, Integer quantity) {
        StoreProductDto product = timesaleService.getProductDetail(productNum);
        if (product == null) {
            return "상품을 찾을 수 없습니다.";
        }

        Integer unitPrice = (product.eventPrice() != null) ? product.eventPrice() : product.price();
        Integer totalPrice = unitPrice * quantity;

        UserOrderDto orderDto = new UserOrderDto(
                null, userIdx, productNum, storeIdx,
                product.productName(), totalPrice, quantity,
                null, null, null, null
        );

        UserOrderDto result = timesaleService.reserveProduct(orderDto);
        return String.format("[완료] '%s' %d개 예약 완료! 주문번호: %d, 총 금액: %,d원",
                product.productName(), quantity, result.orderIndex(), totalPrice);
    }
}
