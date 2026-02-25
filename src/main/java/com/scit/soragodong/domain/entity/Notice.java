package com.scit.soragodong.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "NOTICE")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTICE_IDX")
    private Integer noticeIdx;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Column(name = "CONTENT", nullable = false, columnDefinition = "TEXT")
    private String content;

    // 외래 키 매핑 (FILE_GRP 테이블과 연관관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_GRP_IDX", foreignKey = @ForeignKey(name = "FK_NOTICE_FILE_GRP"))
    private FileGrp fileGrp;

    @Column(name = "CREATE_AT", nullable = false, updatable = false)
    private LocalDateTime createAt;

    @Column(name = "UPDATE_AT")
    private LocalDateTime updateAt;

    @Builder.Default
    @Column(name = "IS_USE", nullable = false)
    private Boolean isUse = true;

    @PrePersist
    protected void onCreate() {
        this.createAt = LocalDateTime.now();
        this.updateAt = LocalDateTime.now();
        if (this.isUse == null) this.isUse = true;
    }
}

