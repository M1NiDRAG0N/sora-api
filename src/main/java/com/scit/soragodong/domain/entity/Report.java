package com.scit.soragodong.domain.entity;

import com.scit.soragodong.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "REPORTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REPORT_IDX")
    private int reportIdx;

    @Column(name = "REPORTER_IDX", nullable = false)
    private int reporterIdx; // 신고자 ID

    @Column(name = "TARGET_TYPE", nullable = false)
    private String targetType; // BOARD, COMMENT, USED_ITEM 등 (enum 안씀)

    @Column(name = "TARGET_IDX", nullable = false)
    private Long targetId;

    @Column(name = "REASON", nullable = false)
    private String reason;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "STATUS")
    @Builder.Default
    private String status = "PENDING"; // PENDING, REVIEWING, COMPLETED (enum 안씀)
}
