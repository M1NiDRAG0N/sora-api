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
    
    private int code;
    private String message;
    private java.util.List<T> data;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    
    /**
     * 페이징 성공 응답
     */
    public static <T> PageResponse<T> success(
            java.util.List<T> data,
            int page,
            int size,
            long totalElements) {
        
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page < totalPages - 1;
        
        return PageResponse.<T>builder()
            .code(200)
            .message("Success")
            .data(data)
            .page(page)
            .size(size)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .hasNext(hasNext)
            .build();
    }
}
