package com.scit.soragodong.exception;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.scit.soragodong.domain.response.ApiResponse;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /** 예외 발생 위치를 "ClassName.method():line" 형태로 반환 */
    private String locate(Exception ex) {
        StackTraceElement[] trace = ex.getStackTrace();
        if (trace.length == 0) return "unknown";
        StackTraceElement e = trace[0];
        String cls = e.getClassName();
        cls = cls.substring(cls.lastIndexOf('.') + 1);
        return cls + "." + e.getMethodName() + "():" + e.getLineNumber();
    }

    /**
     * 커스텀 예외 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException ex, WebRequest request) {
        log.error("[{}] CustomException: {}", locate(ex), ex.getMessage());
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus httpStatus = HttpStatus.valueOf(errorCode.getStatus());
        return ResponseEntity
                .status(httpStatus)
                .body(ApiResponse.error(errorCode));
    }

    /**
     * Spring Security 인증 예외 처리
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException ex,
            WebRequest request) {
        log.error("[{}] AuthenticationException: {}", locate(ex), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.LOGIN_FAILED));
    }

    /**
     * @Valid 검증 실패 예외 처리
     * RequestBody의 @Valid 검증 실패 시 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.error("[{}] MethodArgumentNotValidException: {}", locate(ex), ex.getMessage());
        
        // 필드별 에러 메시지 추출
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT)
                        .addDetail("validationErrors", errors));
    }

    /**
     * @RequestParam 검증 실패 예외 처리
     * 필수 파라미터 누락 시 발생
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, WebRequest request) {
        log.error("[{}] MissingServletRequestParameterException: {}", locate(ex), ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getParameterName(), "필수 파라미터입니다");
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT)
                        .addDetail("missingParameter", ex.getParameterName()));
    }

    /**
     * JSON 파싱 실패 예외 처리
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        log.error("[{}] HttpMessageNotReadableException: {}", locate(ex), ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT)
                        .addDetail("error", "요청 본문이 올바른 JSON 형식이 아닙니다"));
    }

    /**
     * @PathVariable, @RequestParam 검증 예외 처리
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        log.error("[{}] ConstraintViolationException: {}", locate(ex), ex.getMessage());
        
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage()
                ));
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT)
                        .addDetail("constraintViolations", errors));
    }

    /**
     * 데이터베이스 제약 조건 위반 예외 처리
     * 중복 데이터, 외래키 제약 등
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        log.error("[{}] DataIntegrityViolationException: {}", locate(ex), ex.getMessage());
        
        String message = "데이터 무결성 위반 오류가 발생했습니다";
        if (ex.getMessage().contains("unique")) {
            message = "이미 존재하는 데이터입니다";
        } else if (ex.getMessage().contains("foreign key")) {
            message = "연관된 데이터가 존재하지 않습니다";
        }
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT)
                        .addDetail("error", message));
    }

    /**
     * 엔티티를 찾을 수 없을 때 예외 처리
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleEntityNotFoundException(
            EntityNotFoundException ex, WebRequest request) {
        log.error("[{}] EntityNotFoundException: {}", locate(ex), ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCode.NOT_FOUND));
    }

    /**
     * 파일 처리 예외 처리
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<?>> handleIOException(
            IOException ex, WebRequest request) {
        log.error("[{}] IOException: {}", locate(ex), ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR)
                        .addDetail("error", "파일 처리 중 오류가 발생했습니다"));
    }

    /**
     * 404 Not Found 예외 처리 (컨트롤러 핸들러 없음)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request) {
        log.error("[{}] NoHandlerFoundException: {}", locate(ex), ex.getRequestURL());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCode.NOT_FOUND)
                        .addDetail("path", ex.getRequestURL()));
    }

    /**
     * 404 Not Found 예외 처리 (정적 리소스 없음 - Spring Boot 3.x)
     * RuntimeException을 상속하므로 별도 핸들러 없으면 500으로 처리됨
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFoundException(
            NoResourceFoundException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCode.NOT_FOUND));
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex,
            WebRequest request) {
        log.error("[{}] IllegalArgumentException: {}", locate(ex), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT)
                        .addDetail("error", ex.getMessage()));
    }

    /**
     * IllegalStateException 처리
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalStateException(IllegalStateException ex,
            WebRequest request) {
        log.error("[{}] IllegalStateException: {}", locate(ex), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT)
                        .addDetail("error", ex.getMessage()));
    }

    /**
     * RuntimeException 처리
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<?>> handleRuntimeException(RuntimeException ex,
            WebRequest request) {
        log.error("[{}] RuntimeException: {}", locate(ex), ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR)
                        .addDetail("error", ex.getMessage()));
    }

    /**
     * 일반 예외 처리 (최후의 수단)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception ex, WebRequest request) {
        log.error("[{}] Exception: {}", locate(ex), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR)
                        .addDetail("error", "알 수 없는 오류가 발생했습니다"));
    }
}