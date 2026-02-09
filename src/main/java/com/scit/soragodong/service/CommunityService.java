package com.scit.soragodong.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.scit.soragodong.domain.dto.BoardDto;
import com.scit.soragodong.domain.dto.BoardReplyDto;
import com.scit.soragodong.domain.entity.Board;
import com.scit.soragodong.domain.entity.BoardReply;
import com.scit.soragodong.domain.entity.FileGrp;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.domain.enums.FileRefType;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.BoardReplyRepository;
import com.scit.soragodong.repository.BoardRepository;
import com.scit.soragodong.repository.UserRepository;
import com.scit.soragodong.util.DateTimeUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class CommunityService {
    private final BoardRepository br;
    private final UserRepository ur;
    private final BoardReplyRepository brr;

    /**
     * 게시글 10개씩 조회
     * 
     * @param page
     * @return
     */
    public List<BoardDto> getBoardList10(int page) {
        List<BoardDto> dtoList = new ArrayList<>();

        int pageNum = (page < 1) ? 0 : page - 1;

        Pageable pageable = PageRequest.of(pageNum, 10, Sort.Direction.DESC, "createdAt");

        List<Board> boardEntityList = br.findAll(pageable).getContent();

        for (Board board : boardEntityList) {

            BoardDto dto = BoardDto.builder()
                    .boardIdx(board.getBoardIdx())
                    .userIdx(board.getUser().getUserIdx())
                    .userNickname(board.getUser().getUserNickname())
                    .boardCategory(board.getBoardCategory())
                    .boardTitle(board.getBoardTitle())
                    .boardContent(board.getBoardContent())
                    .isUse(board.getIsUse())
                    .timeAgo(DateTimeUtil.calculateTimeAgo(board.getCreatedAt()))
                    .likeCount(board.getLikeCount())
                    .viewCount(board.getViewCount())
                    .createdAt(board.getCreatedAt())
                    .replyCount(board.getReplyCount())
                    .build();

            dtoList.add(dto);
        }
        return dtoList;
    }

    /**
     * 글쓰기
     * 
     * @param boardDto
     * @return
     */
    public BoardDto writeBoard(BoardDto boardDto) {

        // user를 못찾으면 에러 던짐
        Users user = ur.findById(boardDto.userIdx())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        try {
            Board baordEntity = Board.builder()
                    .user(user)
                    .boardCategory(boardDto.boardCategory())
                    .boardTitle(boardDto.boardTitle())
                    .boardContent(boardDto.boardContent())
                    .replyCount(0)
                    .build();

            Board savedBoard = br.save(baordEntity);

            return getBoardOne(savedBoard);
        } catch (Exception e) {
            // 저장 실패하면 에러를 던짐
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }

    }

    /**
     * 글쓰기 후 비동기 새로고침을 위한 서비스
     * 
     * @param savedBoard
     * @return
     */
    private BoardDto getBoardOne(Board savedBoard) {

        BoardDto dto = BoardDto.builder()
                .boardIdx(savedBoard.getBoardIdx())
                .userIdx(savedBoard.getUser().getUserIdx())
                .userNickname(savedBoard.getUser().getUserNickname())
                .boardCategory(savedBoard.getBoardCategory())
                .boardTitle(savedBoard.getBoardTitle())
                .boardContent(savedBoard.getBoardContent())
                .isUse(savedBoard.getIsUse())
                .timeAgo(DateTimeUtil.calculateTimeAgo(savedBoard.getCreatedAt()))
                .likeCount(savedBoard.getLikeCount())
                .viewCount(savedBoard.getViewCount())
                .replyCount(savedBoard.getReplyCount())
                .createdAt(savedBoard.getCreatedAt())
                .build();

        return dto;
    }

    public BoardDto getBoardOne(Integer boardIdx) {
        Board entity = br.findById(boardIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        BoardDto boardDto = BoardDto.builder()
                .boardIdx(entity.getBoardIdx())
                .userIdx(entity.getUser().getUserIdx())
                .userNickname(entity.getUser().getUserNickname())
                .boardCategory(entity.getBoardCategory())
                .boardTitle(entity.getBoardTitle())
                .boardContent(entity.getBoardContent())
                .isUse(entity.getIsUse())
                .timeAgo(DateTimeUtil.calculateTimeAgo(entity.getCreatedAt()))
                .likeCount(entity.getLikeCount())
                .viewCount(entity.getViewCount())
                .createdAt(entity.getCreatedAt())
                .replyCount(entity.getReplyCount())
                .build();

        return boardDto;
    }

    public List<BoardReplyDto> getReplyList(Integer boardIdx) {
        List<BoardReply> replyEntityList = brr.findAllByBoard_BoardIdxOrderByCreatedAtAsc(boardIdx);
        List<BoardReplyDto> dtoList = new ArrayList<>();

        for (BoardReply boardReply : replyEntityList) {

            BoardReplyDto dto = BoardReplyDto.builder()
                    .replyIdx(boardReply.getReplyIdx())
                    .userNickname(boardReply.getUser().getUserNickname())
                    .replyContent(boardReply.getReplyContent())
                    .timeAgo(DateTimeUtil.calculateTimeAgo(boardReply.getCreatedAt()))
                    .build();

            dtoList.add(dto);
        }
        return dtoList;
    }

    public BoardReplyDto writeReply(BoardReplyDto boardReplyDto) {
        // 1. 게시글 번호(Integer)로 -> 진짜 게시글 객체(Board) 찾아오기
        Board board = br.findById(boardReplyDto.boardIdx())
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 2. 유저 번호(Integer)로 -> 진짜 유저 객체(Users) 찾아오기
        Users user = ur.findById(boardReplyDto.userIdx())
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        try {
            BoardReply baordReplyEntity = BoardReply.builder()
                    .board(board)
                    .user(user)
                    .replyContent(boardReplyDto.replyContent())
                    .build();

            brr.save(baordReplyEntity);

            BoardReplyDto boardReplyDto2 = BoardReplyDto.builder()
                    .boardIdx(baordReplyEntity.getBoard().getBoardIdx())
                    .userIdx(baordReplyEntity.getUser().getUserIdx())
                    .userNickname(
                            baordReplyEntity.getUser().getUserNickname())
                    .replyContent(baordReplyEntity.getReplyContent())
                    .isUse(baordReplyEntity.getIsUse())
                    .createdAt(baordReplyEntity.getCreatedAt())
                    .build();

            return boardReplyDto2;

        } catch (Exception e) {
            // 저장 실패하면 에러를 던짐
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
    }

}
