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
import com.scit.soragodong.repository.CommunityRepository;
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

    public List<BoardDto> getBoardListAll(int page) {
        List<BoardDto> dtoList = new ArrayList<>();

        int pageNum = (page < 1) ? 0 : page - 1;

        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt");

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

    public List<BoardDto> getBoardList(int page) { // ✅ int page 추가
        List<BoardDto> dtoList = new ArrayList<>();

        // 🚨 중요: 프론트는 1페이지부터 시작하지만, DB(PageRequest)는 0페이지부터 시작합니다.
        // 그래서 들어온 값에서 -1을 해줘야 합니다. (1 -> 0, 2 -> 1)
        int pageNum = (page < 1) ? 0 : page - 1;

        // 받아온 pageNum을 여기에 넣습니다.
        Pageable pageable = PageRequest.of(pageNum, 10, Sort.Direction.DESC, "createdAt");

        // ... (나머지 로직은 그대로) ...
        List<Board> boardEntityList = cr.findAll(pageable).getContent();
        log.debug("여기까지 왔나?");
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

}
