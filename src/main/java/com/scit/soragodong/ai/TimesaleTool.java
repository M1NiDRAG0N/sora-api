package com.scit.soragodong.ai;

import com.scit.soragodong.domain.dto.StoreDto;
import com.scit.soragodong.domain.dto.StoreProductDto;
import com.scit.soragodong.domain.dto.UserOrderDto;
import com.scit.soragodong.service.TimesaleService;
import org.springframework.ai.tool.annotation.Tool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TimesaleTool {

    private final TimesaleService timesaleService;
    private final Integer userIdx;
    private final Double userLat;
    private final Double userLng;

    public TimesaleTool(TimesaleService timesaleService, Integer userIdx, Double userLat, Double userLng) {
        this.timesaleService = timesaleService;
        this.userIdx = userIdx;
        this.userLat = userLat;
        this.userLng = userLng;
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

    @Tool(description = "사용자가 먹고 싶거나 원하는 음식/상품/가게를 키워드로 검색합니다. 매칭되는 타임세일 가게와 상품을 거리순으로 반환하며, 각 가게 링크를 포함합니다.")
    public String searchTimesaleByKeyword(String keyword) {
        String kw = keyword.toLowerCase();
        List<StoreProductDto> allProducts = timesaleService.getDiscountProducts();
        List<StoreDto> allStores = timesaleService.getEventStores();

        record StoreMatch(StoreDto store, List<StoreProductDto> products, double distance) {}

        List<StoreMatch> matches = new ArrayList<>();
        for (StoreDto store : allStores) {
            boolean storeNameMatch = store.storeName().toLowerCase().contains(kw);
            List<StoreProductDto> matchingProducts = allProducts.stream()
                    .filter(p -> p.storeIdx().equals(store.storeIdx()))
                    .filter(p -> p.productName().toLowerCase().contains(kw)
                            || (p.category() != null && p.category().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());

            if (storeNameMatch || !matchingProducts.isEmpty()) {
                List<StoreProductDto> productsToShow = storeNameMatch
                        ? allProducts.stream().filter(p -> p.storeIdx().equals(store.storeIdx())).collect(Collectors.toList())
                        : matchingProducts;
                double distance = calculateDistance(store.storeLat(), store.storeLng());
                matches.add(new StoreMatch(store, productsToShow, distance));
            }
        }

        if (matches.isEmpty()) {
            return "'" + keyword + "' 관련 타임세일 상품이나 가게를 찾을 수 없어요.";
        }

        matches.sort(Comparator.comparingDouble(StoreMatch::distance));

        StringBuilder sb = new StringBuilder();
        sb.append("'").append(keyword).append("' 검색 결과 (").append(matches.size()).append("개):\n");
        for (int i = 0; i < Math.min(matches.size(), 5); i++) {
            StoreMatch m = matches.get(i);
            sb.append("\n[").append(m.store().storeName()).append("](/timesale/detail?storeIdx=").append(m.store().storeIdx()).append(")");
            if (userLat != null && m.distance() < Double.MAX_VALUE) {
                sb.append(String.format(" (약 %.1fkm)", m.distance()));
            }
            sb.append("\n주소: ").append(m.store().storeAddress());
            for (StoreProductDto p : m.products()) {
                if (p.eventPrice() != null) {
                    sb.append(String.format("\n  • %s %,d원→%,d원 (재고 %d개)", p.productName(), p.price(), p.eventPrice(), p.productQuantity()));
                } else {
                    sb.append(String.format("\n  • %s %,d원 (재고 %d개)", p.productName(), p.price(), p.productQuantity()));
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private double calculateDistance(Double lat, Double lng) {
        if (userLat == null || userLng == null || lat == null || lng == null) return Double.MAX_VALUE;
        double dlat = (lat - userLat) * 111.0;
        double dlng = (lng - userLng) * 111.0 * Math.cos(Math.toRadians(userLat));
        return Math.sqrt(dlat * dlat + dlng * dlng);
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
