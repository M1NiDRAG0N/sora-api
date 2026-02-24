package com.scit.soragodong.domain.dto;

public record NoticeDto(
        Integer noticeIdx,
        String title,
        String content,
        Integer fileGrpIdx,
        String createAt,
        String updateAt,
        Boolean isUse
) {}
