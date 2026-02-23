package com.scit.soragodong.ai;

import com.scit.soragodong.domain.dto.KeywordDto;
import com.scit.soragodong.domain.dto.UsedListRes;
import com.scit.soragodong.domain.dto.UsedRegisterReq;
import com.scit.soragodong.domain.enums.UsedState;
import com.scit.soragodong.domain.response.PageResponse;
import com.scit.soragodong.service.UsedService;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;

public class UsedMarketTool {

    private final UsedService usedService;
    private final Integer userIdx;
    private final Double userLat;
    private final Double userLng;
    private final String userAddress;

    public UsedMarketTool(UsedService usedService, Integer userIdx,
                          Double userLat, Double userLng, String userAddress) {
        this.usedService = usedService;
        this.userIdx = userIdx;
        this.userLat = userLat;
        this.userLng = userLng;
        this.userAddress = userAddress;
    }

    @Tool(description = "중고 물품을 등록합니다. tradingLoc를 지정하지 않으면 사용자의 기본 주소를 사용합니다.")
    public String registerUsed(String title, String content, Integer price, String tradingLoc) {
        String loc = (tradingLoc != null && !tradingLoc.isEmpty()) ? tradingLoc : userAddress;
        UsedRegisterReq req = new UsedRegisterReq(title, content, price, loc, userLat, userLng, userIdx);
        usedService.registerUsedItem(req);
        return String.format("[완료] '%s' 상품이 %,d원에 등록되었습니다. 거래 장소: %s", title, price, loc);
    }

    @Tool(description = "중고 물품 목록을 조회합니다. keyword로 검색할 수 있으며 없으면 전체 목록을 조회합니다.")
    public PageResponse<UsedListRes> getUsedList(String keyword) {
        return usedService.getUsedList(1, keyword);
    }

    @Tool(description = "내 중고 물품의 상태를 변경합니다. state는 TRADING(거래중), SALE(판매중), RESERVED(예약중), SOLD(판매완료) 중 하나입니다.")
    public String updateUsedState(Integer usedIdx, String state) {
        usedService.updateState(usedIdx, UsedState.valueOf(state.toUpperCase()), userIdx);
        String stateName = switch (state.toUpperCase()) {
            case "TRADING" -> "거래중";
            case "SALE" -> "판매중";
            case "RESERVED" -> "예약중";
            case "SOLD" -> "판매완료";
            default -> state;
        };
        return String.format("[완료] 상품(ID: %d) 상태가 '%s'로 변경되었습니다.", usedIdx, stateName);
    }

    @Tool(description = "내 중고 물품을 삭제합니다.")
    public String deleteUsed(Integer usedIdx) {
        usedService.deleteUsedItem(usedIdx, userIdx);
        return String.format("[완료] 상품(ID: %d)이 삭제되었습니다.", usedIdx);
    }

    @Tool(description = "중고 거래 알림 키워드를 등록합니다. 해당 키워드가 포함된 물품이 등록되면 알림을 받습니다.")
    public String addKeyword(String keyword) {
        KeywordDto result = usedService.addKeyword(userIdx, keyword);
        return String.format("[완료] 키워드 '%s'가 등록되었습니다. (ID: %d)", result.keyword(), result.keywordIdx());
    }

    @Tool(description = "등록된 중고 거래 알림 키워드 목록을 조회합니다.")
    public List<KeywordDto> getKeywords() {
        return usedService.getKeywords(userIdx);
    }

    @Tool(description = "중고 거래 알림 키워드를 삭제합니다. keywordIdx는 키워드 ID입니다.")
    public String deleteKeyword(Integer keywordIdx) {
        usedService.deleteKeyword(userIdx, keywordIdx);
        return String.format("[완료] 키워드(ID: %d)가 삭제되었습니다.", keywordIdx);
    }
}
