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

    // ===== 커뮤니티 =====
    BOARD_NOT_FOUND(404, "게시글을 찾을 수 없습니다."),
    REPLY_NOT_FOUND(404, "댓글을 찾을 수 없습니다."),
    EXITS_LIKE_ALREADY(404, "이미 추천한 글입니다."),
    // ===== 파일 =====
    RESOURCE_NOT_FOUND(404, "요청한 리소스를 찾을 수 없습니다."),

    // ===== 요청 / 입력 =====
    INVALID_PASSWORD(400, "올바르지 않은 비밀번호 양식입니다."),
    INVALID_INPUT(400, "올바르지 않은 요청입니다."),
    INVALID_VERIFICATION(400, "인증번호가 일치하지 않습니다."),
    VERIFICATION_EXPIRED(400, "인증번호가 만료되었습니다. 다시 요청해주세요."),
    REQUIRED_VALUE_MISSING(400, "필수 입력값이 누락되었습니다."),

    // ===== 공통 =====
    INTERNAL_ERROR(500, "시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    NOT_FOUND(404, "요청한 리소스를 찾을 수 없습니다.");

    private final int status;
    private final String message;
}
