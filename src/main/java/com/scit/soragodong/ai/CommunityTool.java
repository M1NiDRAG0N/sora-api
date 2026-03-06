package com.scit.soragodong.ai;

import com.scit.soragodong.domain.dto.BoardDto;
import com.scit.soragodong.domain.enums.BoardCategory;
import com.scit.soragodong.service.CommunityService;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;

public class CommunityTool {

    private final CommunityService communityService;
    private final Integer userIdx;

    public CommunityTool(CommunityService communityService, Integer userIdx) {
        this.communityService = communityService;
        this.userIdx = userIdx;
    }

    @Tool(description = "커뮤니티 게시글을 작성합니다. category는 DISTRIBUTION(소분/나눔), TIP(꿀팁/정보), HUMOR(자유/수다), COOK(요리/집밥), QUESTION(질문/고민) 중 하나입니다.")
    public String writeBoard(String category, String title, String content) {
        BoardCategory boardCategory = BoardCategory.fromAI(category);
        BoardDto dto = BoardDto.builder()
                .userIdx(userIdx)
                .boardCategory(boardCategory.name())
                .boardTitle(title)
                .boardContent(content)
                .build();
        BoardDto result = communityService.writeBoard(dto, null);
        return String.format("[완료] 게시글 '%s'가 [%s] 카테고리에 작성되었습니다. (게시글 ID: %d)",
                title, category, result.boardIdx());
    }

    @Tool(description = "커뮤니티 게시글 목록을 조회합니다. category와 keyword는 선택 사항입니다. category를 지정하지 않으면 전체 카테고리를 조회합니다.")
    public List<BoardDto> getBoardList(String category, String keyword) {
        return communityService.getBoardList10Sorted(1, category, keyword, userIdx);
    }

    @Tool(description = "커뮤니티에서 특정 게시글의 상세 정보를 조회합니다.")
    public BoardDto getBoardDetail(Integer boardIdx) {
        return communityService.getBoardOne(boardIdx, userIdx);
    }

    @Tool(description = "커뮤니티 게시글을 삭제합니다. 본인이 작성한 글만 삭제 가능합니다.")
    public String deleteBoard(Integer boardIdx) {
        communityService.boardDelete(boardIdx);
        return String.format("[완료] 게시글(ID: %d)이 삭제되었습니다.", boardIdx);
    }
}
