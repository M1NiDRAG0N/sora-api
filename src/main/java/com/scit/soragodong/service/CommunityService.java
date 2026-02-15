package com.scit.soragodong.service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
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
import com.scit.soragodong.domain.entity.LikeCount;
import com.scit.soragodong.domain.entity.LikeCountKey;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.domain.enums.FileRefType;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.BoardReplyRepository;
import com.scit.soragodong.repository.BoardRepository;
import com.scit.soragodong.repository.FileGrpRepository;
import com.scit.soragodong.repository.FileRepository;
import com.scit.soragodong.repository.LikeCountRepository;
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
    private final LikeCountRepository lcr;

    /**
     * 게시글 10개씩 조회 분류, 무한스크롤
     * 
     * @param page
     * @return
     */
    public List<BoardDto> getBoardList10Sorted(int page, String category, String keyword, Integer userIdx) {
        // 1. 결과 담을 빈 리스트 준비 (원래 하시던 방식)
        List<BoardDto> dtoList = new ArrayList<>();

        // 2. 페이지 계산 (1페이지 -> 0번 인덱스)
        int pageNum = (page < 1) ? 0 : page - 1;

        // 3. 정렬 설정 (최신순)
        Pageable pageable = PageRequest.of(pageNum, 10, Sort.Direction.DESC, "createdAt");
        log.info("검색창 키워드 확인 {}", keyword);
        // 4. 검색 조건 유무 확인 (null이거나 빈 문자열 체크)
        boolean hasCategory = category != null && !category.isEmpty() && !category.equals("전체");
        boolean hasKeyword = keyword != null && !keyword.isEmpty();

        Page<Board> boardPage;

        // 5. 상황에 따라 다른 쿼리 호출 (if-else 분기)
        if (hasCategory && hasKeyword) {
            boardPage = br
                    .findByIsUseTrueAndBoardCategoryAndBoardTitleContainingOrIsUseTrueAndBoardCategoryAndBoardContentContaining(
                            category, keyword, category, keyword, pageable);

        } else if (hasCategory) {
            boardPage = br.findByBoardCategoryAndIsUseTrue(category, pageable);

        } else if (hasKeyword) {
            boardPage = br.findByIsUseTrueAndBoardTitleContainingOrIsUseTrueAndBoardContentContaining(
                    keyword, keyword, pageable);

        } else {
            boardPage = br.findByIsUseTrue(pageable);
        }

        List<Board> boardEntityList = boardPage.getContent();
        for (Board board : boardEntityList) {
            boolean isLiked = false;
            LikeCountKey likeCountKey = new LikeCountKey(board.getBoardIdx(), userIdx);
            isLiked = lcr.existsById(likeCountKey);
            BoardDto dto = BoardDto.builder()
                    .boardIdx(board.getBoardIdx())
                    .userIdx(board.getUser().getUserIdx())
                    .userNickname(board.getUser().getUserNickname())
                    .boardCategory(board.getBoardCategory())
                    .profileIdx(board.getUser()
                            .getProfileIdx())
                    .boardTitle(board.getBoardTitle())
                    .boardContent(board.getBoardContent())
                    .isUse(board.getIsUse())
                    .isLiked(isLiked)
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
                .likeCount(savedBoard.getLikeCount())
                .viewCount(savedBoard.getViewCount())
                .replyCount(savedBoard.getReplyCount())
                .createdAt(savedBoard.getCreatedAt())
                .fileGrpIdx(savedBoard.getFileGrp() != null ? savedBoard.getFileGrp().getFileGrpIdx() : null)
                .build();

        return dto;
    }

    public BoardDto getBoardOne(Integer boardIdx, Integer userIdx) {
        Board entity = br.findById(boardIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        LikeCountKey key = new LikeCountKey(boardIdx, userIdx);
        boolean isLiked = lcr.existsById(key);

        BoardDto boardDto = BoardDto.builder()
                .boardIdx(entity.getBoardIdx())
                .userIdx(entity.getUser().getUserIdx())
                .userNickname(entity.getUser().getUserNickname())
                .boardCategory(entity.getBoardCategory())
                .boardTitle(entity.getBoardTitle())
                .boardContent(entity.getBoardContent())
                .isUse(entity.getIsUse())
                .isLiked(isLiked)
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

        List<BoardReply> replyEntityList = brr.findAllByBoard_BoardIdxAndIsUseTrueOrderByCreatedAtAsc(boardIdx);
        List<BoardReplyDto> dtoList = new ArrayList<>();

        for (BoardReply boardReply : replyEntityList) {

            BoardReplyDto dto = BoardReplyDto.builder()
                    .replyIdx(boardReply.getReplyIdx())
                    .boardIdx(boardReply.getBoard().getBoardIdx())
                    .userNickname(boardReply.getUser().getUserNickname())
                    .userIdx(boardReply.getUser().getUserIdx())
                    .replyContent(boardReply.getReplyContent())
                    .createdAt((boardReply.getCreatedAt()))
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

    public BoardReplyDto getReplyOne(Integer replyIdx) {

        BoardReply replyEntity = brr.findById(replyIdx).orElseThrow(() -> new CustomException(
                ErrorCode.REPLY_NOT_FOUND));

        BoardReplyDto dto = BoardReplyDto.builder()
                .replyIdx(replyEntity.getReplyIdx())
                .boardIdx(replyEntity.getBoard().getBoardIdx())
                .userNickname(replyEntity.getUser().getUserNickname())
                .userIdx(replyEntity.getUser().getUserIdx())
                .createdAt((replyEntity.getCreatedAt()))
                .replyContent(replyEntity.getReplyContent())
                .build();

        return dto;
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

    public void replyDelete(Integer replyIdx) {
        BoardReply boardReply = brr.findById(replyIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.REPLY_NOT_FOUND));

        boardReply.delete();
    }

    public void replyUpdate(Integer replyIdx, String replyContent) {
        BoardReply boardReply = brr.findById(replyIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.REPLY_NOT_FOUND));

        boardReply.updateContent(replyContent);

        brr.save(boardReply);
    }

    @Transactional
    public void updateBoard(BoardDto dto, List<MultipartFile> newFiles, List<Integer> deleteFileIdxs, Integer userIdx) {
        // 1. 게시글 조회 (record는 getter에 'get'이 안 붙습니다!)
        Board board = br.findById(dto.boardIdx())
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        // 2. 본인 확인
        if (!board.getUser().getUserIdx().equals(userIdx)) {
            throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
        }

        // 3. 내용 수정 (Record 값 꺼내기)
        board.updateBoard(dto.boardTitle(), dto.boardContent(), dto.boardCategory());

        // 4. 파일 삭제 요청 처리
        if (deleteFileIdxs != null && !deleteFileIdxs.isEmpty()) {
            // (선택) 실제 파일 삭제 로직이 필요하면 여기서 파일 정보 조회 후 삭제
            // List<BoardFile> files = bfr.findAllById(deleteFileIdxs);
            // ... 파일 삭제 로직 ...

            // DB에서 정보 삭제
            fileRepository.deleteAllById(deleteFileIdxs);
        }

        // 5. 새 파일 업로드
        if (newFiles != null && !newFiles.isEmpty()) {
            // 파일 그룹 ID가 없으면(기존 파일 0개) 새로 생성
            Integer fileGrpIdx = board.getFileGrp().getFileGrpIdx();
            if (fileGrpIdx == null) {
                // 예: 시퀀스나 UUID로 그룹 ID 생성 로직
                // fileGrpIdx = fileService.createFileGroup();
                // board.updateFileGrpIdx(fileGrpIdx); // 엔티티에 그룹ID 업데이트 필요
            }

            // 파일 저장 로직 호출 (기존 write 때 썼던 것 재사용)
            // fileService.saveFiles(newFiles, fileGrpIdx);
        }
    }

    @Transactional
    public void addLike(Integer boardIdx, Integer userIdx) {
        LikeCountKey key = new LikeCountKey(boardIdx, userIdx);
        boolean exists = lcr.existsById(key);
        log.info("존재 확인 {}", exists);
        if (exists) {
            throw new CustomException(ErrorCode.EXITS_LIKE_ALREADY);
        }

        LikeCount likeCount = LikeCount.builder()
                .id(key)
                .build();
        lcr.save(likeCount);

        Board board = br.findById(
                boardIdx)
                .orElseThrow(
                        () -> new RuntimeException("글을 찾을 수 없습니다."));

        log.info("야발로그 {}", board);
        board.increaseLike();
    }

}
