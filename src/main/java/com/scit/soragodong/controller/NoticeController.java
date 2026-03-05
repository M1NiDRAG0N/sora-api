package com.scit.soragodong.controller;

import com.scit.soragodong.domain.dto.NoticeDto;
import com.scit.soragodong.domain.response.ApiResponse;
import com.scit.soragodong.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/notice/list")
    @ResponseBody
    public ApiResponse<List<NoticeDto>> getNoticeList() {
        return ApiResponse.success(noticeService.getAllNotices());
    }

    @GetMapping("/notice/{noticeIdx}")
    @ResponseBody
    public ApiResponse<NoticeDto> getNoticeDetail(@PathVariable(name = "noticeIdx") Integer noticeIdx) {
        return ApiResponse.success(noticeService.getNotice(noticeIdx));
    }
}
