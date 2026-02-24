package com.scit.soragodong.service;

import com.scit.soragodong.domain.dto.NoticeDto;
import com.scit.soragodong.domain.entity.Notice;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public List<NoticeDto> getAllNotices() {
        return noticeRepository.findByIsUseTrueOrderByCreateAtDesc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public NoticeDto getNotice(Integer noticeIdx) {
        Notice notice = noticeRepository.findById(noticeIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
        return convertToDto(notice);
    }

    private NoticeDto convertToDto(Notice notice) {
        return new NoticeDto(
                notice.getNoticeIdx(),
                notice.getTitle(),
                notice.getContent(),
                notice.getFileGrp() != null ? notice.getFileGrp().getFileGrpIdx() : null,
                notice.getCreateAt() != null ? notice.getCreateAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                notice.getUpdateAt() != null ? notice.getUpdateAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                notice.getIsUse()
        );
    }
}
