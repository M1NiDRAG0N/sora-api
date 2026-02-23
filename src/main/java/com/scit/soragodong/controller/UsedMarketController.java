package com.scit.soragodong.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.scit.soragodong.domain.dto.FileRes;
import com.scit.soragodong.domain.dto.KeywordDto;
import com.scit.soragodong.domain.dto.UsedDetailRes;
import com.scit.soragodong.domain.dto.UsedListRes;
import com.scit.soragodong.domain.dto.UsedRegisterReq;
import com.scit.soragodong.domain.dto.UsedRegisterRes;
import com.scit.soragodong.domain.dto.UsedUpdateReq;
import com.scit.soragodong.domain.enums.FileRefType;
import com.scit.soragodong.domain.enums.UsedState;
import com.scit.soragodong.domain.response.ApiResponse;
import com.scit.soragodong.domain.response.PageResponse;
import com.scit.soragodong.security.CustomUserDetails;
import com.scit.soragodong.service.FileService;
import com.scit.soragodong.service.UsedService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/used-market")
@RequiredArgsConstructor
@Slf4j
public class UsedMarketController {
    
    @Value("${google.maps.api-key:default-key}")
    private String googleMapsApiKey;
    
    private final UsedService usedService;
    private final FileService fileService;

    @GetMapping("")
    public String usedMarketPage(Model model) {
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        model.addAttribute("currentUri", "/used-market");
        return "common";
    }
    
    /**
     * 중고 물건 등록 (파일 포함)
     * POST /used-market/register
     */
    @PostMapping("/register")
    @ResponseBody
    public ApiResponse<UsedRegisterRes> registerUsedItem(
            @RequestParam(name = "usedTitle") String usedTitle,
            @RequestParam(name = "usedContent") String usedContent,
            @RequestParam(name = "usedPrice") Integer usedPrice,
            @RequestParam(name = "tradingLoc", required = false) String tradingLoc,
            @RequestParam(name = "tradingLatitude", required = false) Double tradingLatitude,
            @RequestParam(name = "tradingLongitude", required = false) Double tradingLongitude,
            @RequestParam(name = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("[중고거래] 상품 등록 요청 - 제목: {}, 사용자: {}", usedTitle, userDetails.getUserIdx());

        Integer userIdx = userDetails.getUserIdx();

        UsedRegisterReq req = new UsedRegisterReq(
                usedTitle, usedContent, usedPrice,
                tradingLoc, tradingLatitude, tradingLongitude,
                userIdx
        );

        UsedRegisterRes result = usedService.registerUsedItem(req);

        // 파일 업로드 (실패해도 상품 등록은 유지)
        if (files != null && !files.isEmpty()) {
            try {
                fileService.upload(FileRefType.USED, result.usedIdx(), files);
                log.info("[중고거래] 파일 업로드 완료 - 상품 ID: {}", result.usedIdx());
            } catch (Exception e) {
                log.warn("[중고거래] 파일 업로드 실패: {}", e.getMessage());
            }
        }

        return ApiResponse.success("상품이 성공적으로 등록되었습니다.", result);
    }

    /**
     * 중고 물품 목록 조회 (페이징, 검색)
     * GET /used-market/list?page=1&keyword=검색어
     */
    @GetMapping("/list")
    @ResponseBody
    public ApiResponse<PageResponse<UsedListRes>> getUsedList(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "keyword", required = false) String keyword) {

        log.info("[중고거래] 목록 조회 - 페이지: {}, 키워드: {}", page, keyword);
        PageResponse<UsedListRes> result = usedService.getUsedList(page, keyword);
        return ApiResponse.success(result);
    }

    /**
     * 중고 물품 상세 조회
     * GET /used-market/{usedIdx}
     */
    @GetMapping("/{usedIdx}")
    @ResponseBody
    public ApiResponse<UsedDetailRes> getUsedDetail(
            @PathVariable(name = "usedIdx") Integer usedIdx,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("[중고거래] 상세 조회 - ID: {}", usedIdx);
        Integer currentUserId = userDetails.getUserIdx();
        String viewerKey = "u" + currentUserId;

        UsedDetailRes result = usedService.getUsedDetail(usedIdx, currentUserId, viewerKey);
        return ApiResponse.success(result);
    }

    /**
     * 중고 물품 논리 삭제
     * DELETE /used-market/{usedIdx}
     */
    @DeleteMapping("/{usedIdx}")
    @ResponseBody
    public ApiResponse<?> deleteUsedItem(
            @PathVariable(name = "usedIdx") Integer usedIdx,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("[중고거래] 상품 삭제 요청 - ID: {}, 사용자: {}", usedIdx, userDetails.getUserIdx());
        usedService.deleteUsedItem(usedIdx, userDetails.getUserIdx());
        return ApiResponse.success("상품이 삭제되었습니다.", null);
    }

    /**
     * 중고 물품 수정
     * PUT /used-market/{usedIdx}
     */
    @PutMapping("/{usedIdx}")
    @ResponseBody
    public ApiResponse<?> updateUsedItem(
            @PathVariable(name = "usedIdx") Integer usedIdx,
            @ModelAttribute UsedUpdateReq req,
            @RequestParam(required = false) List<MultipartFile> newFiles,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("[중고거래] 상품 수정 요청 - ID: {}, 사용자: {}", usedIdx, userDetails.getUserIdx());
        usedService.updateUsedItem(usedIdx, req, userDetails.getUserIdx());

        if (newFiles != null && !newFiles.isEmpty()) {
            try {
                fileService.upload(FileRefType.USED, usedIdx, newFiles);
            } catch (Exception e) {
                log.warn("[중고거래] 수정 파일 업로드 실패: {}", e.getMessage());
            }
        }

        return ApiResponse.success("상품이 수정되었습니다.", null);
    }

    /**
     * 수정 시 기존 이미지 파일 목록 조회
     * GET /used-market/{usedIdx}/files
     */
    @GetMapping("/{usedIdx}/files")
    @ResponseBody
    public ApiResponse<List<FileRes>> getUsedFiles(
            @PathVariable(name = "usedIdx") Integer usedIdx) {

        return ApiResponse.success(usedService.getUsedFiles(usedIdx));
    }

    /**
     * 상품 상태 변경 (판매자 전용)
     * PATCH /used-market/{usedIdx}/status?state=SOLD
     */
    @PatchMapping("/{usedIdx}/status")
    @ResponseBody
    public ApiResponse<?> updateStatus(
            @PathVariable(name = "usedIdx") Integer usedIdx,
            @RequestParam UsedState state,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        usedService.updateState(usedIdx, state, userDetails.getUserIdx());
        return ApiResponse.success("상태가 변경되었습니다.", null);
    }

    /**
     * 찜(좋아요) 토글
     * POST /used-market/{usedIdx}/like
     */
    @PostMapping("/{usedIdx}/like")
    @ResponseBody
    public ApiResponse<Boolean> toggleLike(
            @PathVariable(name = "usedIdx") Integer usedIdx,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        boolean isLiked = usedService.toggleLike(usedIdx, userDetails.getUserIdx());
        return ApiResponse.success(isLiked);
    }

    // ==================== 키워드 관리 ====================

    /**
     * 내 키워드 목록 조회
     * GET /used-market/keywords
     */
    @GetMapping("/keywords")
    @ResponseBody
    public ApiResponse<List<KeywordDto>> getKeywords(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("[키워드] 목록 조회 - 사용자: {}", userDetails.getUserIdx());
        List<KeywordDto> keywords = usedService.getKeywords(userDetails.getUserIdx());
        return ApiResponse.success(keywords);
    }

    /**
     * 키워드 등록
     * POST /used-market/keywords
     */
    @PostMapping("/keywords")
    @ResponseBody
    public ApiResponse<KeywordDto> addKeyword(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String keyword = request.get("keyword");
        log.info("[키워드] 등록 요청 - 사용자: {}, 키워드: {}", userDetails.getUserIdx(), keyword);

        KeywordDto result = usedService.addKeyword(userDetails.getUserIdx(), keyword);
        return ApiResponse.success("키워드가 등록되었습니다.", result);
    }

    /**
     * 키워드 삭제
     * DELETE /used-market/keywords/{keywordIdx}
     */
    @DeleteMapping("/keywords/{keywordIdx}")
    @ResponseBody
    public ApiResponse<?> deleteKeyword(
            @PathVariable(name = "keywordIdx") Integer keywordIdx,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("[키워드] 삭제 요청 - 사용자: {}, 키워드ID: {}", userDetails.getUserIdx(), keywordIdx);
        usedService.deleteKeyword(userDetails.getUserIdx(), keywordIdx);
        return ApiResponse.success("키워드가 삭제되었습니다.", null);
    }
}
