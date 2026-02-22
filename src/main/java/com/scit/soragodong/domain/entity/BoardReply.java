package com.scit.soragodong.domain.entity;

import com.scit.soragodong.common.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "BOARD_REPLY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BoardReply extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REPLY_IDX")
    private Integer replyIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BOARD_IDX", referencedColumnName = "BOARD_IDX")
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY) //
    @JoinColumn(name = "USER_IDX", referencedColumnName = "USER_IDX")
    private Users user;

    // @Column(name = "USER_IDX", nullable = false)
    // private Integer userIdx;

    @Column(name = "REPLY_CONTENT")
    private String replyContent;

    @Column(name = "IS_USE", nullable = false)
    @Builder.Default // 빌더 쓸 때도 이 값이 기본으로 들어가게 해줌
    private Boolean isUse = true;

    // ★ 댓글 삭제 편의 메서드
    public void delete() {
        this.isUse = false;
    }

    public void updateContent(String content) {
        this.replyContent = content;
    }
}