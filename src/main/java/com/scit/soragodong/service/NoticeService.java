package com.scit.soragodong.service;

import com.scit.soragodong.domain.dto.NoticeDto;
import com.scit.soragodong.domain.entity.Notice;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
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

    @Transactional
    public Integer createNotice(String title, String content) {
        log.info("[NoticeService] 공지사항 생성 시작 - 제목: {}", title);
        try {
            Notice notice = Notice.builder()
                    .title(title)
                    .content(content)
                    .isUse(true)
                    .build();
            Notice saved = noticeRepository.save(notice);
            log.info("[NoticeService] 공지사항 저장 완료 - ID: {}", saved.getNoticeIdx());
            return saved.getNoticeIdx();
        } catch (Exception e) {
            log.error("[NoticeService] 공지사항 저장 중 오류: {}", e.getMessage(), e);
            throw e;
        }
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
