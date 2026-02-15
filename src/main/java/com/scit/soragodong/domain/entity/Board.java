package com.scit.soragodong.domain.entity;

import org.hibernate.annotations.Formula;

import com.scit.soragodong.common.BaseEntity;

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
    @Builder.Default // 빌더 쓸 때도 이 값이 기본으로 들어가게 해줌
    private Boolean isUse = true;

    @Builder.Default
    @Column(name = "LIKE_COUNT", nullable = false)
    private Integer likeCount = 0;

    // 조회수 (기본값 0)
    @Builder.Default
    @Column(name = "VIEW_COUNT", nullable = false)
    private Integer viewCount = 0;

    @OneToOne(fetch = FetchType.LAZY) // 또는 @ManyToOne
    @JoinColumn(name = "FILE_GRP_IDX")
    private FileGrp fileGrp;

    @Formula("(SELECT count(1) FROM BOARD_REPLY r WHERE r.BOARD_IDX = BOARD_IDX and r.IS_USE = true)")
    private int replyCount;

    public void delete() {
        this.isUse = false;
    }

    public void updateBoard(String title, String content, String category) {
        this.boardTitle = title;
        this.boardContent = content;
        this.boardCategory = category;
    }

    // 추천 증가 메서드
    // 서비스 단에서 그냥 1더해서 저장해도 되지만 여기서 숫자 증가 로직을 만들어놓는게 더 안정적임.
    public void increaseLike() {
        this.likeCount++;
    }
}