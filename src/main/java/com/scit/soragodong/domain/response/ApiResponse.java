package com.scit.soragodong.domain.response;

import java.util.HashMap;
import java.util.Map;

import com.scit.soragodong.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    private int status;
    private String message;
    private T data;
    
    @Builder.Default
    private Map<String, Object> detail = new HashMap<>();
    
    /**
     * 성공 응답
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .status(200)
            .message("Success")
            .data(data)
            .build();
    }
    
    /**
     * 성공 응답 (커스텀 메시지)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .status(200)
            .message(message)
            .data(data)
            .build();
    }
    
    /**
     * 실패 응답
     */
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
            .status(status)
            .message(message)
            .build();
    }

    /**
     * 실패 응답 (ErrorCode 사용)
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
            .status(errorCode.getStatus())
            .message(errorCode.getMessage())
            .build();
    }
    
    /**
     * 상세 정보 추가
     * @param key 키
     * @param value 값
     * @return ApiResponse (체이닝 가능)
     */
    public ApiResponse<T> addDetail(String key, Object value) {
        this.detail.put(key, value);
        return this;
    }
    
    /**
     * 여러 상세 정보 추가
     * @param details 상세 정보 맵
     * @return ApiResponse (체이닝 가능)
     */
    public ApiResponse<T> addDetails(Map<String, Object> details) {
        this.detail.putAll(details);
        return this;
    }
    
    /**
     * 실패 응답 (400)
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return error(400, message);
    }
    
    /**
     * 실패 응답 (401)
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return error(401, message);
    }
    
    /**
     * 실패 응답 (403)
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return error(403, message);
    }
    
    /**
     * 실패 응답 (404)
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return error(404, message);
    }
    
    /**
     * 실패 응답 (500)
     */
    public static <T> ApiResponse<T> internalServerError(String message) {
        return error(500, message);
    }
}
