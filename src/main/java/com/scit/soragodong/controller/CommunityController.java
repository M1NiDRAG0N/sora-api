package com.scit.soragodong.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.scit.soragodong.domain.dto.BoardDto;
import com.scit.soragodong.service.CommunityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService cs;

    @GetMapping("/community")
    public String communityPage(Model model) {
        model.addAttribute("currentUri", "/community");
        int page = 0;
        List<BoardDto> boardDtoList = cs.getBoardListAll(page);
        log.debug("{}", boardDtoList);

        model.addAttribute("boardDtoList", boardDtoList);
        return "common";
    }

    @GetMapping("/community/list")
    @ResponseBody
    public List<BoardDto> getBoardListAll(@RequestParam(name = "page", defaultValue = "1") int page) {
        log.info("AJAX 게시글 요청 페이지: {}", page);

        return cs.getBoardList(page);
    }

}
