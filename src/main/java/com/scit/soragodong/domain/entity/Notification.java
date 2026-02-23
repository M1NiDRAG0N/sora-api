package com.scit.soragodong.domain.entity;

import java.time.LocalDateTime;

import com.scit.soragodong.domain.enums.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "NOTIFICATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTI_IDX")
    private Integer notiIdx;

    @Column(name = "USER_IDX", nullable = false)
    private Integer userIdx;

    @Enumerated(EnumType.STRING)
    @Column(name = "NOTI_TYPE", length = 30, nullable = false)
    private NotificationType notiType;

    @Column(name = "REF_ID")
    private Integer refId;

    @Column(name = "MESSAGE", length = 500)
    private String message;

    @Column(name = "IS_READ")
    private Boolean isRead;

    @Builder.Default
    @Column(name = "IS_USE")
    private Boolean isUse = true;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.isRead = this.isRead == null ? false : this.isRead;
        this.isUse = this.isUse == null ? true : this.isUse;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public void delete() {
        this.isUse = false;
    }
}