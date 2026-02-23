package com.scit.soragodong.domain.dto;

import java.time.format.DateTimeFormatter;

import com.scit.soragodong.domain.entity.Notification;
import com.scit.soragodong.domain.enums.NotificationType;

public record NotificationRes(
        Integer notiIdx,
        NotificationType notiType,
        Integer refId,
        String message,
        Boolean isRead,
        String createdAt   // ISO 문자열 - 프론트에서 Utils.timeAgo() 로 처리
) {
    public static NotificationRes from(Notification n) {
        return new NotificationRes(
                n.getNotiIdx(),
                n.getNotiType(),
                n.getRefId(),
                n.getMessage(),
                n.getIsRead(),
                n.getCreatedAt() != null
                        ? n.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null
        );
    }
}
