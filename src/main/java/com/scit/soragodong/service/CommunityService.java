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

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class CommunityService {
    private final CommunityRepository cr;

    public List<BoardDto> getBoardListAll() {
        List<BoardDto> dtoList = new ArrayList<>();

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
                    .likeCount(board.getLikeCount())
                    .viewCount(board.getViewCount())
                    .createdAt(board.getCreatedAt())
                    .build();

            dtoList.add(dto);
        }
        return dtoList;
    }

}
