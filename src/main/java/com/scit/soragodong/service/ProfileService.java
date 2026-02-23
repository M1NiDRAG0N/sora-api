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

import com.scit.soragodong.domain.dto.FileRes;
import com.scit.soragodong.domain.dto.ProfileUpdateDto;
import com.scit.soragodong.domain.entity.Board;
import com.scit.soragodong.domain.entity.BoardReply;
import com.scit.soragodong.domain.entity.LikeCount;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.domain.enums.FileRefType;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.BoardReplyRepository;
import com.scit.soragodong.repository.BoardRepository;
import com.scit.soragodong.repository.LikeCountRepository;
import com.scit.soragodong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
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
    private final com.scit.soragodong.repository.UsedRepository usedRepository; // 중고거래 리포지토리 추가
    private final com.scit.soragodong.repository.FileGrpRepository fileGrpRepository; // 파일 그룹 리포지토리 추가
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;

    public ProfileDto getProfile(Integer targetUserIdx, Integer loginUserIdx) {
        Users user = userRepository.findById(targetUserIdx)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isMyProfile = targetUserIdx.equals(loginUserIdx);

        // 1. 작성한 글 (커뮤니티)
        List<ProfileDto.ProfileItemDto> posts = new ArrayList<>();
        
        List<Board> myBoards = boardRepository.findByUser_UserIdxAndIsUseTrue(targetUserIdx);
        posts.addAll(myBoards.stream().map(this::convertToItem).collect(Collectors.toList()));

        // 2. 작성한 글 (중고거래) - 중고거래 게시글 추가
        List<com.scit.soragodong.domain.entity.Used> myUsedItems = usedRepository.findByCreatedUserAndIsUseTrueOrderByCreatedAtDesc(
                targetUserIdx, org.springframework.data.domain.PageRequest.of(0, 100)).getContent();
        
        posts.addAll(myUsedItems.stream().map(this::convertUsedToItem).collect(Collectors.toList()));

        // 최신순 정렬
        posts.sort((a, b) -> {
            if (a.createdAt() == null) return 1;
            if (b.createdAt() == null) return -1;
            return b.createdAt().compareTo(a.createdAt());
        });
        
        // 3. 작성한 댓글
        List<BoardReply> myReplies = boardReplyRepository.findByUser_UserIdxAndIsUseTrue(targetUserIdx).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

        // 4. 좋아요한 글
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
                .profileImg(user.getProfileIdx() != null ? "/img/" + user.getProfileIdx() : null)
                .mannerScore(user.getMannerScore() != null ? user.getMannerScore() : 36) // 기본 36.5도
                .postCount(posts.size())
                .commentCount(myReplies.size())
                .likeCount(myLikes.size())
                .myProfile(isMyProfile)
                .posts(posts)
                .comments(myReplies.stream().map(this::convertToReplyItem).collect(Collectors.toList()))
                .likes(likedBoards.stream().map(this::convertToItem).collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public void updateProfile(Integer userIdx, ProfileUpdateDto dto, MultipartFile profileImage) {
        Users user = userRepository.findById(userIdx)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1. 비밀번호 변경 시에만 현재 비밀번호 확인
        if (dto.newPassword() != null && !dto.newPassword().isEmpty()) {
            if (dto.currentPassword() == null || !passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_PASSWORD); 
            }
            user.updatePassword(passwordEncoder.encode(dto.newPassword()));
        }

        // 2. 기본 정보 수정 (비밀번호 확인 없이 가능)
        user.updateProfile(dto.nickname(), dto.address(), dto.lat(), dto.lng());

        // 3. 프로필 이미지 수정
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                // FileRefType.USER_PROFILE 사용 (User 엔티티와 연결된 파일 그룹)
                // userIdx를 refId로 사용하여 파일 그룹 생성/조회 및 파일 업로드
                List<FileRes> uploadedFiles = fileService.upload(FileRefType.USER_PROFILE, userIdx, Collections.singletonList(profileImage));
                
                if (!uploadedFiles.isEmpty()) {
                    // 첫 번째 업로드된 파일의 ID를 프로필 이미지 ID로 설정
                    // 주의: User 엔티티의 profileIdx가 Integer 타입이고, FileRes의 fileIdx도 Integer여야 함
                    user.updateProfileImage(uploadedFiles.get(0).fileIdx());
                }
            } catch (Exception e) {
                // 파일 업로드 실패 시 로그 출력 등 예외 처리
                throw new CustomException(ErrorCode.INTERNAL_ERROR);
            }
        }
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
                .createdAt(board.getCreatedAt())
                .build();
    }

    private ProfileDto.ProfileItemDto convertUsedToItem(com.scit.soragodong.domain.entity.Used used) {
        String thumb = null;
        
        // FILE_GRP 테이블에서 refType='USED'이고 refId=usedIdx 인 데이터 조회
        java.util.Optional<com.scit.soragodong.domain.entity.FileGrp> fileGrp = 
            fileGrpRepository.findByRefTypeAndRefId(com.scit.soragodong.domain.enums.FileRefType.USED, used.getUsedIdx());
            
        if (fileGrp.isPresent()) {
             thumb = "/img/group/" + fileGrp.get().getFileGrpIdx(); 
        }

        return ProfileDto.ProfileItemDto.builder()
                .id(used.getUsedIdx())
                .title(used.getUsedTitle())
                .category("중고거래")
                .contentPreview(used.getUsedContent())
                .timeAgo(used.getCreatedAt().toString())
                .price(String.valueOf(used.getUsedPrice())) // 가격 추가
                .type("market") // 프론트엔드 구분을 위한 타입
                .thumbnail(thumb)
                .createdAt(used.getCreatedAt())
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
                .createdAt(reply.getCreatedAt())
                .build();
    }
}
