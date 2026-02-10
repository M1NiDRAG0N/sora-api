package com.scit.soragodong.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.scit.soragodong.domain.dto.BoardDto;
import com.scit.soragodong.domain.dto.BoardReplyDto;
import com.scit.soragodong.domain.dto.FileRes;
import com.scit.soragodong.domain.entity.BoardReply;
import com.scit.soragodong.domain.enums.FileRefType;
import com.scit.soragodong.domain.response.ApiResponse;
import com.scit.soragodong.security.CustomUserDetails;
import com.scit.soragodong.service.CommunityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@Slf4j
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService cs;

    @GetMapping("/community")
    public String communityPage(Model model) {
        model.addAttribute("currentUri", "/community");
        int page = 0;
        List<BoardDto> boardDtoList = cs.getBoardList10(page);
        log.debug("{}", boardDtoList);

        model.addAttribute("boardDtoList", boardDtoList);
        return "common";
    }

    @GetMapping("/community/list")
    @ResponseBody
    public List<BoardDto> getBoardListAll(@RequestParam(name = "page", defaultValue = "1") int page) {
        log.info("AJAX 게시글 요청 페이지: {}", page);

        return cs.getBoardList10(page);
    }

    @PostMapping("/community/write")
    @ResponseBody
    public BoardDto write(@RequestPart("board") BoardDto boardDto, 
                          @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        log.info("글쓰기 확인 {}", boardDto);
        BoardDto newBoardDto = cs.writeBoard(boardDto, files);
        log.info("글쓰기 후 newBoardDto 확인 {}", newBoardDto);
        // // ApiResponse 활용.
        // return ApiResponse.success("글 등록에 성공", savedBoardIdx);
        return newBoardDto;
    }

    @GetMapping("/community/view/{boardIdx}")
    @ResponseBody
    public BoardDto view(@PathVariable("boardIdx") Integer boardIdx) {
        log.info("상세보기 index {}", boardIdx);

        BoardDto boardDto = cs.getBoardOne(boardIdx);
        return boardDto;
    }
    
    @GetMapping("/community/files/{boardIdx}")
    @ResponseBody
    public List<FileRes> getBoardFiles(@PathVariable("boardIdx") Integer boardIdx) {
        return cs.getBoardFiles(boardIdx);
    }

    @GetMapping("/community/reply/{boardIdx}")
    @ResponseBody
    public List<BoardReplyDto> reply(@PathVariable("boardIdx") Integer boardIdx) {
        log.info("댓글 boardIdx {}", boardIdx);
        List<BoardReplyDto> replyList = cs.getReplyList(boardIdx);
        log.info("댓글 리스트 {}", replyList);
        return replyList;
    }

    @PostMapping("/community/reply/{boardIdx}")
    @ResponseBody
    public BoardReplyDto writeReply(@PathVariable("boardIdx") Integer boardIdx,
            @RequestBody BoardReplyDto replyDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("댓글 작성용 boardIdx {}", boardIdx);
        log.info("댓글 작성용 userDetails {}", userDetails.getUserIdx());
        log.info("댓글 작성용 replyDto {}", replyDto);

        BoardReplyDto newDto = BoardReplyDto.builder()
                .boardIdx(boardIdx)
                .replyContent(replyDto.replyContent())
                .userIdx(userDetails.getUserIdx())
                .build();
        // 지금 content가 안들어옴 (해결)
        log.info("댓글 작성 dto 완성 {}", newDto);

        BoardReplyDto boardReplyDto = cs.writeReply(newDto);
        log.info("view로 보내기 전 dto 확인 {}", boardReplyDto);
        return boardReplyDto;
    }

}
