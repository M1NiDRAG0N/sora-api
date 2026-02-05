package com.scit.soragodong.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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

        List<BoardDto> boardDtoList = cs.getBoardListAll();
        log.debug("{}", boardDtoList);

        model.addAttribute("boardDtoList", boardDtoList);
        return "common";
    }

}
