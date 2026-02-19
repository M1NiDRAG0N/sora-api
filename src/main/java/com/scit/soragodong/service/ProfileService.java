package com.scit.soragodong.service;

import com.scit.soragodong.domain.dto.ProfileDto;
import com.scit.soragodong.domain.entity.Board;
import com.scit.soragodong.domain.entity.BoardReply;
import com.scit.soragodong.domain.entity.LikeCount;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.repository.BoardReplyRepository;
import com.scit.soragodong.repository.BoardRepository;
import com.scit.soragodong.repository.LikeCountRepository;
import com.scit.soragodong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final BoardReplyRepository boardReplyRepository;
    private final LikeCountRepository likeCountRepository;

    public ProfileDto getProfile(Integer targetUserIdx, Integer loginUserIdx) {
        Users user = userRepository.findById(targetUserIdx)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isMyProfile = targetUserIdx.equals(loginUserIdx);

        // 1. 작성한 글 (커뮤니티)
        List<Board> myBoards = boardRepository.findByUser_UserIdxAndIsUseTrue(targetUserIdx).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

        // 2. 작성한 댓글
        List<BoardReply> myReplies = boardReplyRepository.findByUser_UserIdxAndIsUseTrue(targetUserIdx).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

        // 3. 좋아요한 글
        List<LikeCount> myLikes = likeCountRepository.findByUserId(targetUserIdx);
        List<Board> likedBoards = new ArrayList<>();
        for (LikeCount like : myLikes) {
            boardRepository.findById(like.getId().getBoardIdx()).ifPresent(board -> {
                if (board.getIsUse()) likedBoards.add(board);
            });
        }
        likedBoards.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())); // 최신순

        // DTO 변환
        return ProfileDto.builder()
                .userIdx(user.getUserIdx())
                .nickname(user.getUserNickname())
                .description(user.getUserAddress()) // 주소로 대체
                .profileImg(user.getProfileIdx() != null ? "/images/profiles/" + user.getProfileIdx() + ".png" : null)
                .mannerScore(user.getMannerScore() != null ? user.getMannerScore() : 36) // 기본 36.5도
                .postCount(myBoards.size())
                .commentCount(myReplies.size())
                .likeCount(myLikes.size())
                .myProfile(isMyProfile)
                .posts(myBoards.stream().map(this::convertToItem).collect(Collectors.toList()))
                .comments(myReplies.stream().map(this::convertToReplyItem).collect(Collectors.toList()))
                .likes(likedBoards.stream().map(this::convertToItem).collect(Collectors.toList()))
                .build();
    }

    private ProfileDto.ProfileItemDto convertToItem(Board board) {
        String thumb = null;
        if (board.getFileGrp() != null) {
             // 썸네일 로직은 복잡하므로 여기서는 fileGrpIdx가 있으면 있다고 가정
             thumb = "/img/group/" + board.getFileGrp().getFileGrpIdx(); 
        }

        return ProfileDto.ProfileItemDto.builder()
                .id(board.getBoardIdx())
                .title(board.getBoardTitle())
                .category(board.getBoardCategory())
                .contentPreview(board.getBoardContent()) // 길이 제한 필요할 수 있음
                // DateTimeUtil이 있다고 가정 (없으면 toString)
                .timeAgo(board.getCreatedAt().toString()) 
                .type("community")
                .thumbnail(thumb)
                .build();
    }

    private ProfileDto.ProfileItemDto convertToReplyItem(BoardReply reply) {
        return ProfileDto.ProfileItemDto.builder()
                .id(reply.getBoard().getBoardIdx()) // 댓글 클릭 시 해당 글로 이동
                .title(reply.getReplyContent()) // 댓글 내용을 제목처럼
                .category("댓글")
                .contentPreview("원글: " + reply.getBoard().getBoardTitle())
                .timeAgo(reply.getCreatedAt().toString())
                .type("reply")
                .build();
    }
}
