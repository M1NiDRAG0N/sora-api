package com.scit.soragodong.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.scit.soragodong.common.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "LIKE_COUNT")
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LikeCount {

    // 복합키 (객체 매핑 없이 ID값만 가짐)
    @EmbeddedId
    private LikeCountKey id;

    @CreatedDate
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}