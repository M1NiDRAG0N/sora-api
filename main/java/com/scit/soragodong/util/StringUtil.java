package com.scit.soragodong.util;

public class StringUtil {
    
    /**
     * 문자열 Null 체크 및 기본값 반환
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return str == null || str.trim().isEmpty() ? defaultValue : str;
    }
    
    /**
     * 문자열 자르기
     */
    public static String truncate(String str, int length) {
        return str != null && str.length() > length ? str.substring(0, length) + "..." : str;
    }
    
    /**
     * 문자열 반복
     */
    public static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    /**
     * CamelCase를 snake_case로 변환
     */
    public static String camelToSnake(String str) {
        return str.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }
    
    /**
     * snake_case를 CamelCase로 변환
     */
    public static String snakeToCamel(String str) {
        String[] parts = str.split("_");
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            sb.append(parts[i].substring(0, 1).toUpperCase())
              .append(parts[i].substring(1).toLowerCase());
        }
        return sb.toString();
    }
    
    /**
     * 특수문자 제거
     */
    public static String removeSpecialChars(String str) {
        return str != null ? str.replaceAll("[^a-zA-Z0-9]", "") : str;
    }
    
    /**
     * 마스킹 처리 (예: 010-xxxx-1234)
     */
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return phone.substring(0, phone.length() - 4).replaceAll(".(?=.)", "x") + 
               phone.substring(phone.length() - 4);
    }
    
    /**
     * 이메일 마스킹 처리 (예: a****@example.com)
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) {
            name = name.charAt(0) + "*";
        } else {
            name = name.substring(0, 2) + "*".repeat(name.length() - 2);
        }
        return name + "@" + parts[1];
    }
}
