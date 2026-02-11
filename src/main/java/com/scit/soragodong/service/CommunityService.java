package com.scit.soragodong.service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.scit.soragodong.domain.dto.BoardDto;
import com.scit.soragodong.domain.dto.BoardReplyDto;
import com.scit.soragodong.domain.dto.FileRes;
import com.scit.soragodong.domain.entity.Board;
import com.scit.soragodong.domain.entity.BoardReply;
import com.scit.soragodong.domain.entity.File;
import com.scit.soragodong.domain.entity.FileGrp;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.domain.enums.FileRefType;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.BoardReplyRepository;
import com.scit.soragodong.repository.BoardRepository;
import com.scit.soragodong.repository.FileGrpRepository;
import com.scit.soragodong.repository.FileRepository;
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
    private final FileService fileService;
    private final FileGrpRepository fileGrpRepository;
    private final FileRepository fileRepository;

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

        List<Board> boardEntityList = br.findByIsUseTrue(pageable).getContent();

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
                    .fileGrpIdx(board.getFileGrp() != null ? board.getFileGrp().getFileGrpIdx() : null)
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
    public BoardDto writeBoard(BoardDto boardDto, List<MultipartFile> files) {

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

            // 파일 저장 로직
            if (files != null && !files.isEmpty()) {
                // 파일 업로드 (FileGrp 생성 포함)
                fileService.upload(FileRefType.BOARD, savedBoard.getBoardIdx(), files);

                // 생성된 FileGrp 조회
                Optional<FileGrp> fileGrpOpt = fileGrpRepository.findByRefTypeAndRefId(FileRefType.BOARD,
                        savedBoard.getBoardIdx());

                if (fileGrpOpt.isPresent()) {
                    FileGrp fileGrp = fileGrpOpt.get();
                    log.info("FileGrp found: {}", fileGrp.getFileGrpIdx());

                    // Reflection을 사용하여 fileGrp 설정 (Setter가 없으므로)
                    Field field = Board.class.getDeclaredField("fileGrp");
                    field.setAccessible(true);
                    field.set(savedBoard, fileGrp);

                    // 변경사항 강제 저장 (UPDATE 쿼리 유발)
                    savedBoard = br.saveAndFlush(savedBoard);
                    log.info("Board updated with FileGrp");
                } else {
                    log.warn("FileGrp not found after upload");
                }
            }

            return getBoardOne(savedBoard);
        } catch (Exception e) {
            log.error("글쓰기 에러", e);
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
                .fileGrpIdx(savedBoard.getFileGrp() != null ? savedBoard.getFileGrp().getFileGrpIdx() : null)
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
                .fileGrpIdx(entity.getFileGrp() != null ? entity.getFileGrp().getFileGrpIdx() : null)
                .build();

        return boardDto;
    }

    /**
     * 게시글 첨부파일 목록 조회
     */
    public List<FileRes> getBoardFiles(Integer boardIdx) {
        Optional<FileGrp> grpOpt = fileGrpRepository.findByRefTypeAndRefId(FileRefType.BOARD, boardIdx);
        if (grpOpt.isEmpty()) {
            return new ArrayList<>();
        }

        FileGrp group = grpOpt.get();
        List<File> files = fileRepository.findByFileGroupAndIsUseTrueOrderByFileOrder(group);
        log.info("Board {} has {} files", boardIdx, files.size());

        return files.stream()
                .map(f -> new FileRes(f.getFileIdx(), f.getOriginalName(), f.getFilePath(), f.getFileOrder()))
                .collect(Collectors.toList());
    }

    public List<BoardReplyDto> getReplyList(Integer boardIdx) {
        List<BoardReply> replyEntityList = brr.findAllByBoard_BoardIdxOrderByCreatedAtAsc(boardIdx);
        List<BoardReplyDto> dtoList = new ArrayList<>();

        for (BoardReply boardReply : replyEntityList) {

            BoardReplyDto dto = BoardReplyDto.builder()
                    .replyIdx(boardReply.getReplyIdx())
                    .boardIdx(boardReply.getBoard().getBoardIdx())
                    .userNickname(boardReply.getUser().getUserNickname())
                    .userIdx(boardReply.getUser().getUserIdx())
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

    @Transactional // ★ 변경 감지(Dirty Checking)를 위해 필수!
    public void boardDelete(Integer boardIdx) {
        // 1. 게시글 찾기 (없으면 커스텀 예외 발생 -> 핸들러가 잡아서 처리함)
        Board board = br.findById(boardIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        board.delete();

        // 3. 해당 게시글에 달린 댓글들도 모두 찾기
        List<BoardReply> replyList = brr.findAllByBoard_BoardIdx(boardIdx);

        for (BoardReply reply : replyList) {
            reply.delete();
        }
    }

}
