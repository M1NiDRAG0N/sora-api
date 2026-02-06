package com.scit.soragodong.domain.entity;

import com.scit.soragodong.common.BaseEntity;
import com.scit.soragodong.domain.enums.UserRole;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "BOARD")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BOARD_IDX")
    private Integer boardIdx;

    @ManyToOne(fetch = FetchType.LAZY) //
    @JoinColumn(name = "USER_IDX", referencedColumnName = "USER_IDX")
    private Users user;

    @Column(name = "BOARD_CATEGORY", length = 20, nullable = false)
    private String boardCategory;

    @Column(name = "BOARD_TITLE", length = 200, nullable = false)
    private String boardTitle;

    @Column(name = "BOARD_CONTENT", length = 200, nullable = false, columnDefinition = "text")
    private String boardContent;

    @Column(name = "IS_USE", nullable = false)
    private Boolean isUse;

    @Builder.Default
    @Column(name = "LIKE_COUNT", nullable = false)
    private Integer likeCount = 0;

    // 조회수 (기본값 0)
    @Builder.Default
    @Column(name = "VIEW_COUNT", nullable = false)
    private Integer viewCount = 0;

    @PrePersist
    protected void onCreate() {
        if (this.isUse == null)
            this.isUse = true;
    }

}