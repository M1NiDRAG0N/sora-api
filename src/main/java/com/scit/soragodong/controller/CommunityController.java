package com.scit.soragodong.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;

@Controller
@Slf4j
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService cs;

    // 커뮤니티 메인 조회
    @GetMapping("/community")
    public String communityPage(Model model) {
        model.addAttribute("currentUri", "/community");
        // int page = 0;
        // String category = "ALL";
        // String keyword = "";
        // List<BoardDto> boardDtoList = cs.getBoardList10Sorted(page, category,
        // keyword);
        // log.debug("{}", boardDtoList);

        return "common";
    }

    // 커뮤니티 메인 무한스크롤 비동기 조회
    @GetMapping("/community/list") // POST -> GET으로 변경
    @ResponseBody
    public List<BoardDto> getBoardList(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "keyword", required = false) String keyword,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("페이지 {}", page);
        log.info("카테고리 {}", category);
        log.info("키워드 {}", keyword);
        return cs.getBoardList10Sorted(page, category, keyword, userDetails.getUserIdx());
    }

    // 글쓰기
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

    // 상세보기 데이터 불러오기
    @GetMapping("/community/view/{boardIdx}")
    @ResponseBody
    public BoardDto view(@PathVariable("boardIdx") Integer boardIdx,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("상세보기 index {}", boardIdx);

        // 쿠키 확인 로직
        Cookie[] cookies = request.getCookies();
        boolean viewed = false;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("viewed_board_" + boardIdx)) {
                    viewed = true;
                    break;
                }
            }
        }

        if (!viewed) {
            cs.incrementViewCount(boardIdx);
            
            // 쿠키 생성 (24시간 유지)
            Cookie cookie = new Cookie("viewed_board_" + boardIdx, "true");
            cookie.setMaxAge(60 * 60 * 24); 
            cookie.setPath("/"); // 모든 경로에서 유효
            response.addCookie(cookie);
        }

        BoardDto boardDto = cs.getBoardOne(boardIdx, userDetails.getUserIdx());
        return boardDto;
    }

    // 커뮤니티 파일 불러오기
    @GetMapping("/community/files/{boardIdx}")
    @ResponseBody
    public List<FileRes> getBoardFiles(@PathVariable("boardIdx") Integer boardIdx) {
        return cs.getBoardFiles(boardIdx);
    }

    // 댓글 리스트 조회
    @GetMapping("/community/reply/{boardIdx}")
    @ResponseBody
    public List<BoardReplyDto> reply(@PathVariable("boardIdx") Integer boardIdx) {
        log.info("댓글 boardIdx {}", boardIdx);
        List<BoardReplyDto> replyList = cs.getReplyList(boardIdx);
        log.info("댓글 리스트 {}", replyList);
        return replyList;
    }

    // 댓글 작성
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

    // 댓글 하나만 불러오기
    @GetMapping("/community/replyOne/{replyIdx}")
    @ResponseBody
    public BoardReplyDto readReplyOneForUpdate(@PathVariable("replyIdx") Integer replyIdx) {
        log.info("댓글 수정 replyIdx {}", replyIdx);

        BoardReplyDto boardReplyDto = cs.getReplyOne(replyIdx);
        log.info("view로 보내기 전 dto 확인 {}", boardReplyDto);
        return boardReplyDto;
    }

    // 게시글 삭제
    @DeleteMapping("/community/delete/{boardIdx}")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable("boardIdx") Integer boardIdx) {
        log.info("삭제 요청 boardIdx: {}", boardIdx);

        // 서비스 호출 (여기서 에러 나면 GlobalExceptionHandler로 자동 이동)
        cs.boardDelete(boardIdx);

        // 성공 시 응답 (ApiResponse 사용)
        return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다."));
    }

    // 댓글 삭제
    @DeleteMapping("/community/deleteReply/{replyIdx}")
    public ResponseEntity<ApiResponse<?>> deleteReply(@PathVariable("replyIdx") Integer replyIdx) {
        log.info("삭제 요청 replyIdx: {}", replyIdx);

        cs.replyDelete(replyIdx);

        // 성공 시 응답 (ApiResponse 사용)
        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다."));
    }

    // 댓글 수정
    @PutMapping("/community/reply/update/{replyIdx}")
    public ResponseEntity<ApiResponse<?>> putMethodName(@PathVariable("replyIdx") Integer replyIdx,
            @RequestBody BoardReplyDto boardReplyDto) {
        log.info("댓글 수정 요청 replyIdx: {}", replyIdx);
        log.info("댓글 수정 요청 boardReplydto: {}", boardReplyDto);

        cs.replyUpdate(replyIdx, boardReplyDto.replyContent());

        return ResponseEntity.ok(ApiResponse.success("댓글이 수정되었습니다."));
    }

    @PutMapping("/community/edit")
    public ResponseEntity<ApiResponse<?>> editPost(
            @RequestPart("board") BoardDto boardDto,
            @RequestPart(value = "newFiles", required = false) List<MultipartFile> newFiles,
            @RequestPart(value = "deleteFileIdxs", required = false) List<Integer> deleteFileIdxs,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("수정 요청: {}", boardDto);
        log.info("삭제할 파일 ID 목록: {}", deleteFileIdxs);
        log.info("새로 추가할 파일 개수: {}", (newFiles != null ? newFiles.size() : 0));

        // 서비스 호출
        cs.updateBoard(boardDto, newFiles, deleteFileIdxs, userDetails.getUserIdx());

        return ResponseEntity.ok(ApiResponse.success("수정 성공"));
    }

    @GetMapping("/community/like/{boardIdx}")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> toggleLike(@PathVariable("boardIdx") Integer boardIdx,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("좋아요 토글 boardIdx: {}, userIdx: {}", boardIdx, userDetails.getUserIdx());
        
        boolean isLiked = cs.toggleLike(boardIdx, userDetails.getUserIdx());

        return ResponseEntity.ok(ApiResponse.success(isLiked));
    }

}
