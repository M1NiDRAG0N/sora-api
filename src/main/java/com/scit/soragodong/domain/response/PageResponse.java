package com.scit.soragodong.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private java.util.List<T> content;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;
    private boolean hasPrevious;

    /**
     * 페이징 성공 응답
     */
    public static <T> PageResponse<T> success(
            java.util.List<T> content,
            int currentPage,
            int size,
            long totalElements) {

        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = currentPage < totalPages;
        boolean hasPrevious = currentPage > 1;

        return PageResponse.<T>builder()
            .content(content)
            .currentPage(currentPage)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .hasNext(hasNext)
            .hasPrevious(hasPrevious)
            .build();
    }
}
