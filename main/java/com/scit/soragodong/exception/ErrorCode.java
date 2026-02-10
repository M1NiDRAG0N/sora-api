package com.scit.soragodong.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // ===== 인증 / 권한 =====
    LOGIN_FAILED(401, "아이디 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(401, "로그인이 필요합니다."),
    ACCESS_DENIED(403, "접근 권한이 없습니다."),

    // ===== 사용자 =====
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(409, "이미 사용 중인 이메일입니다."),

    // ===== 요청 / 입력 =====
    INVALID_INPUT(400, "입력값이 올바르지 않습니다."),
    REQUIRED_VALUE_MISSING(400, "필수 입력값이 누락되었습니다."),

    // ===== 공통 =====
    INTERNAL_ERROR(500, "시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    
    private final int status;
    private final String message;
}
