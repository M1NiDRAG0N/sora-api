package com.scit.soragodong.domain.dto;

import java.time.LocalDateTime;

public record NoticeDto(
        Integer noticeIdx,
        String title,
        String content,
        Integer fileGrpIdx,
        LocalDateTime createAt,
        LocalDateTime updateAt,
        Boolean isUse
) {}
