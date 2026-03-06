package com.scit.soragodong.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BoardCategory {

    DISTRIBUTION("소분/나눔", new String[]{"소분", "나눔", "분배", "무료", "공유", "나눠", "드려요", "드림"}),
    TIP         ("꿀팁/정보", new String[]{"팁", "정보", "꿀팁", "노하우", "추천", "방법", "알려", "후기"}),
    HUMOR       ("자유/수다", new String[]{"자유", "수다", "잡담", "일상", "유머", "재미", "웃긴", "소통", "이야기"}),
    COOK        ("요리/집밥", new String[]{"요리", "집밥", "레시피", "음식", "요리법", "자취요리", "밥", "반찬", "조리"}),
    QUESTION    ("질문/고민", new String[]{"질문", "고민", "궁금", "도움", "조언", "어떻게", "추천해", "여쭤"});

    private final String label;
    private final String[] keywords;

    /**
     * AI가 넘겨준 문자열을 enum으로 변환
     * enum name 일치 → label 일치 → 키워드 포함 순으로 매칭, 모두 실패 시 HUMOR 반환
     */
    public static BoardCategory fromAI(String input) {
        if (input == null || input.isBlank()) return HUMOR;
        String upper = input.toUpperCase().trim();
        String lower = input.toLowerCase().trim();

        // 1) enum name 정확 일치 (DISTRIBUTION, TIP 등)
        for (BoardCategory c : values()) {
            if (c.name().equals(upper)) return c;
        }
        // 2) label 일치
        for (BoardCategory c : values()) {
            if (c.label.equals(input.trim())) return c;
        }
        // 3) 키워드 포함 일치
        for (BoardCategory c : values()) {
            for (String kw : c.keywords) {
                if (lower.contains(kw) || kw.contains(lower)) return c;
            }
        }
        return HUMOR;
    }
}
