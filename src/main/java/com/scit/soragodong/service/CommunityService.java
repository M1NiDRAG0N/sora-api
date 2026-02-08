package com.scit.soragodong.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.scit.soragodong.domain.dto.BoardDto;
import com.scit.soragodong.domain.entity.Board;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.CommunityRepository;
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
    private final CommunityRepository cr;
    private final UserRepository ur;

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

        List<Board> boardEntityList = cr.findAll(pageable).getContent();

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
                    .build();

            Board savedBoard = cr.save(baordEntity);

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
                .createdAt(savedBoard.getCreatedAt())
                .build();

        return dto;
    }

}
