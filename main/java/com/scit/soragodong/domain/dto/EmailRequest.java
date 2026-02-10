package com.scit.soragodong.domain.dto;

public record EmailRequest(
    String to,
    String subject,
    String content,
    boolean isHtml
) {
    public EmailRequest(String to, String subject, String content) {
        this(to, subject, content, true);
    }
}
