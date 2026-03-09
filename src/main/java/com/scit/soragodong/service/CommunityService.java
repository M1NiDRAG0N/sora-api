package com.scit.soragodong.service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.scit.soragodong.domain.enums.NotificationType;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.BoardReplyRepository;
import com.scit.soragodong.repository.BoardRepository;
import com.scit.soragodong.repository.FileGrpRepository;
import com.scit.soragodong.repository.FileRepository;
import com.scit.soragodong.repository.LikeCountRepository;
import com.scit.soragodong.repository.UserRepository;

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
    private final NotificationService notificationService;

    /**
     * 게시글 10개씩 조회 분류, 무한스크롤
     * 
     * @param page
     * @return
     */
    public List<BoardDto> getBoardList10Sorted(int page, String category, String keyword, Integer userIdx) {
        // 1. 페이지 계산
        int pageNum = (page < 1) ? 0 : page - 1;
        Pageable pageable;
        Page<Board> boardPage;

        // 2. 검색 조건 유무 확인
        // '인기글'은 별도 처리하므로 hasCategory 체크에서 제외
        boolean isPopular = "인기글".equals(category);
        boolean hasCategory = category != null && !category.isEmpty() && !category.equals("전체") && !isPopular;
        boolean hasKeyword = keyword != null && !keyword.isEmpty();

        // 3. 쿼리 실행 분기
        if (isPopular) {
            // [인기글] 최근 7일 + 좋아요 10개 이상 + 좋아요 순
            pageable = PageRequest.of(pageNum, 10);
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            boardPage = br.findByCreatedAtAfterAndLikeCountGreaterThanEqualAndIsUseTrueOrderByLikeCountDesc(
                    sevenDaysAgo, 10, pageable);

        } else {
            // [일반글] 최신순 정렬
            pageable = PageRequest.of(pageNum, 10, Sort.Direction.DESC, "createdAt");

            if (hasCategory && hasKeyword) {
                boardPage = br.findByIsUseTrueAndBoardCategoryAndBoardTitleContainingOrIsUseTrueAndBoardCategoryAndBoardContentContaining(
                        category, keyword, category, keyword, pageable);
            } else if (hasCategory) {
                boardPage = br.findByBoardCategoryAndIsUseTrue(category, pageable);
            } else if (hasKeyword) {
                boardPage = br.findByIsUseTrueAndBoardTitleContainingOrIsUseTrueAndBoardContentContaining(
                        keyword, keyword, pageable);
            } else {
                boardPage = br.findByIsUseTrue(pageable);
            }
        }

        // 4. Entity -> DTO 변환 (공통 로직)
        List<BoardDto> dtoList = new ArrayList<>();
        for (Board board : boardPage.getContent()) {
            boolean isLiked = false;
            // 로그인한 경우 좋아요 여부 확인
            if (userIdx != null) {
                LikeCountKey likeCountKey = new LikeCountKey(userIdx, board.getBoardIdx());
                isLiked = lcr.existsById(likeCountKey);
            }
            
            BoardDto dto = BoardDto.builder()
                    .boardIdx(board.getBoardIdx())
                    .userIdx(board.getUser().getUserIdx())
                    .userNickname(board.getUser().getUserNickname())
                    .boardCategory(board.getBoardCategory())
                    .profileIdx(board.getUser().getProfileIdx())
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
                .profileIdx(savedBoard.getUser().getProfileIdx())
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

        LikeCountKey key = new LikeCountKey(userIdx, boardIdx);
        boolean isLiked = lcr.existsById(key);

        BoardDto boardDto = BoardDto.builder()
                .boardIdx(entity.getBoardIdx())
                .userIdx(entity.getUser().getUserIdx())
                .userNickname(entity.getUser().getUserNickname())
                .profileIdx(entity.getUser().getProfileIdx())
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
                    .profileIdx(boardReply.getUser().getProfileIdx())
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

            // 알림 발송 (본인 글에 쓴 댓글 제외)
            if (!board.getUser().getUserIdx().equals(user.getUserIdx())) {
                notificationService.send(
                        board.getUser().getUserIdx(),
                        NotificationType.COMMENT,
                        board.getBoardIdx(),
                        "내 게시글에 새 댓글이 달렸습니다.");
            }

            BoardReplyDto boardReplyDto2 = BoardReplyDto.builder()
                    .replyIdx(baordReplyEntity.getReplyIdx())
                    .boardIdx(baordReplyEntity.getBoard().getBoardIdx())
                    .userIdx(baordReplyEntity.getUser().getUserIdx())
                    .profileIdx(baordReplyEntity.getUser().getProfileIdx())
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
                .profileIdx(replyEntity.getUser().getProfileIdx())
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
            // 파일 업로드 (FileGrp 생성 포함)
            fileService.upload(FileRefType.BOARD, board.getBoardIdx(), newFiles);

            if (board.getFileGrp() == null) {
                // 생성된 FileGrp 조회
                Optional<FileGrp> fileGrpOpt = fileGrpRepository.findByRefTypeAndRefId(FileRefType.BOARD,
                        board.getBoardIdx());

                if (fileGrpOpt.isPresent()) {
                    FileGrp fileGrp = fileGrpOpt.get();
                    log.info("FileGrp found during update: {}", fileGrp.getFileGrpIdx());

                    // Reflection을 사용하여 fileGrp 설정 (Setter가 없으므로)
                    try {
                        java.lang.reflect.Field field = Board.class.getDeclaredField("fileGrp");
                        field.setAccessible(true);
                        field.set(board, fileGrp);
                        br.save(board);
                    } catch (Exception e) {
                        log.error("Failed to set fileGrp: {}", e.getMessage());
                    }
                }
            }
        }
    }

    @Transactional
    public boolean toggleLike(Integer boardIdx, Integer userIdx) {
        LikeCountKey key = new LikeCountKey(userIdx, boardIdx);
        boolean exists = lcr.existsById(key);

        Board board = br.findById(boardIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        if (exists) {
            // 이미 좋아요 상태 -> 취소 (삭제)
            lcr.deleteById(key);
            board.decreaseLike();
            return false; // 이제 좋아요 아님
        } else {
            // 좋아요 안 누른 상태 -> 등록 (추가)
            lcr.save(LikeCount.builder().id(key).build());
            board.increaseLike();
            return true; // 이제 좋아요 상태임
        }
    }

    @Transactional
    public void incrementViewCount(Integer boardIdx) {
        Board board = br.findById(boardIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
        board.increaseViewCount();
    }

}
