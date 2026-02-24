package com.scit.soragodong.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    private String targetType; // BOARD, COMMENT, USED, etc.
    private Long targetId;
    private String reason;
    private String description;
}
