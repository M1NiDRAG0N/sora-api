package com.scit.soragodong.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FinanceCategory {

    SALARY  ("월급",  new String[]{"월급", "급여", "임금", "연봉", "보너스", "상여", "수당", "알바비", "아르바이트"}),
    FOOD    ("식비",  new String[]{"식비", "밥", "음식", "식사", "점심", "저녁", "아침", "카페", "커피", "배달", "외식", "편의점", "마트", "장보기", "치킨", "피자"}),
    TRANSPORT("교통비", new String[]{"교통비", "교통", "버스", "지하철", "택시", "기차", "ktx", "주유", "기름", "주차", "톨게이트", "고속도로"}),
    SHOPPING("쇼핑",  new String[]{"쇼핑", "옷", "의류", "신발", "가방", "쿠팡", "네이버쇼핑", "온라인쇼핑", "구매", "구입"}),
    EVENT   ("경조사", new String[]{"경조사", "결혼", "장례", "돌잔치", "축의금", "부조금", "선물", "생일", "명절"}),
    SAVINGS ("저축",  new String[]{"저축", "적금", "예금", "투자", "펀드", "주식", "코인", "저금", "비상금"}),
    ETC     ("기타",  new String[]{"기타", "잡비", "기타지출", "기타수입", "유틸리티", "공과금", "월세", "관리비", "보험", "의료비", "병원", "약국", "학원", "교육"});

    private final String label;
    private final String[] keywords;

    /**
     * AI가 넘겨준 문자열(한국어 키워드 등)을 enum으로 변환
     * 정확히 일치 → 키워드 포함 순으로 매칭, 모두 실패 시 ETC 반환
     */
    public static FinanceCategory fromAI(String input) {
        if (input == null || input.isBlank()) return ETC;
        String lower = input.toLowerCase().trim();

        // 1) label 정확 일치
        for (FinanceCategory c : values()) {
            if (c.label.equals(lower)) return c;
        }
        // 2) 키워드 포함 일치
        for (FinanceCategory c : values()) {
            for (String kw : c.keywords) {
                if (lower.contains(kw) || kw.contains(lower)) return c;
            }
        }
        return ETC;
    }

    /** 카테고리 목록을 AI 설명용 문자열로 반환 */
    public static String descriptionForAI() {
        StringBuilder sb = new StringBuilder();
        for (FinanceCategory c : values()) {
            sb.append(String.format("- %s: %s%n", c.label, String.join(", ", c.keywords)));
        }
        return sb.toString();
    }
}
