package com.scit.soragodong.util;

import java.util.regex.Pattern;

public class ValidationUtil {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^01[0-9]\\d{3,4}\\d{4}$"
    );
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$!%*?&])[A-Za-z\\d@#$!%*?&]{8,16}$"
    );
    
    /**
     * 이메일 유효성 검사
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 휴대폰 번호 유효성 검사 (01XXXXXXXXX)
     */
    public static boolean isValidPhoneNumber(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * 비밀번호 유효성 검사 (8자 이상, 대소문자+숫자+특수문자 포함)
     */
    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }
    
    /**
     * 문자열 공백 확인
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 유효한 값인지 확인 (null 또는 공백 확인)
     */
    public static boolean isValid(String str) {
        return str != null && !str.trim().isEmpty();
    }
    
    /**
     * 문자열 길이 확인
     */
    public static boolean isLengthValid(String str, int minLength, int maxLength) {
        return str != null && str.length() >= minLength && str.length() <= maxLength;
    }
    
    /**
     * 숫자만 포함하는지 확인
     */
    public static boolean isNumeric(String str) {
        return str != null && str.matches("^[0-9]+$");
    }
    
    /**
     * URL 유효성 검사
     */
    public static boolean isValidUrl(String url) {
        return url != null && url.matches("^https?://.*");
    }
}
