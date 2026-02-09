package com.scit.soragodong.controller;

import com.scit.soragodong.domain.dto.StoreDto;
import com.scit.soragodong.domain.dto.StoreProductDto;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.service.TimesaleService;
import com.scit.soragodong.util.FileUploadUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


@Controller
@RequestMapping("admin")
@Slf4j
@RequiredArgsConstructor
@Transactional

public class AdminController {

    private final TimesaleService timeSaleService;

    @GetMapping("store")
    public String store(@RequestParam(value = "path", required = false, defaultValue = "전체지역") String path
            ,@RequestParam(value = "storeIdx", required = false) Integer storeIdx
            , Model model) {

        model.addAttribute("currentPath", path);

        if (storeIdx != null) {
            // [상품 모드] 특정 점포 안으로 들어온 경우
            try {
                List<StoreProductDto> products = timeSaleService.getProductsByStore(storeIdx);
                model.addAttribute("productList", products);
                model.addAttribute("viewMode", "PRODUCT"); // 상품 보기 모드
                model.addAttribute("storeIdx", storeIdx);
            } catch (Exception e){
                e.printStackTrace();
                return String.valueOf(ErrorCode.INTERNAL_ERROR);
            }
        } else {
            // [기존 모드] 주소별 폴더/점포 목록 보기
            // (기존의 subFolders, currentStores 조회 로직 실행)
            model.addAttribute("viewMode", "STORE"); // 점포 보기 모드


            // 1. DB에서 모든 점포 정보 가져오기 (Service 호출)
            // "전체지역" 텍스트 자체는 주소 검색에서 제외하기 위해 처리
            String searchPath = path.equals("전체지역") ? "" : path.replace("전체지역", "").trim();
            List<StoreDto> allStores = timeSaleService.getActiveStores();

            // 2. 현재 경로에 해당하는 하위 폴더(주소 파트)와 점포(StoreName) 필터링
            Set<String> subFolders = new TreeSet<>(); // 하위 폴더 중복 방지 및 정렬
            List<StoreDto> currentStores = new ArrayList<>();

            for (StoreDto store : allStores) {
                String addr = store.storeAddress();

                if (searchPath.isEmpty()) {
                    // 루트(전체지역)일 때는 모든 주소의 첫 번째 마디(예: 서울시, 경기도)를 폴더로 노출
                    subFolders.add(addr.split(" ")[0]);
                } else if (addr.startsWith(searchPath)) {
                    // 현재 경로 이후의 문자열 추출
                    String remaining = addr.substring(searchPath.length()).trim();

                    if (remaining.isEmpty()) {
                        // 주소가 정확히 일치하는 경우 (점포 노출)
                        currentStores.add(store);
                    } else {
                        // 하위 주소가 더 있는 경우 (폴더 노출)
                        subFolders.add(remaining.split(" ")[0]);
                    }
                }
            }


            model.addAttribute("subFolders", subFolders);    // 하위 폴더 리스트
            model.addAttribute("currentStores", currentStores); // 현재 위치의 점포 리스트
        }
        return "admin/store";
    }
    @ResponseBody
    @PostMapping("api/store")
    public String registerStore(
            @ModelAttribute StoreDto storeDto, // @RequestBody 대신 @ModelAttribute 사용
            @RequestParam(value="uploadFile", required=false) MultipartFile uploadFile) {
			log.debug(storeDto.toString());
        if (uploadFile != null && !uploadFile.isEmpty()) {
            // 1. 파일 저장 로직 (예: 설정된 경로에 저장 후 저장된 파일명 반환)
            String savedFileName = null;
            try {
                savedFileName = FileUploadUtil.uploadFile(uploadFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            
        }

        timeSaleService.createStore(storeDto);
        return "success";
    }
    // 삭제 (isUse를 0으로 변경)
    @DeleteMapping("api/store/{idx}")
    @ResponseBody
    public String deleteStore(@PathVariable Integer idx) {
        timeSaleService.updateIsUse(idx, (byte)0);
        return "success";
    }

    // 이름 수정
    @PatchMapping("api/store/rename")
    @ResponseBody
    public String renameStore(@RequestBody StoreDto dto) {
        // dto.storeIdx와 dto.storeName만 사용하여 업데이트
        timeSaleService.updateStoreName(dto.storeIdx(), dto.storeName());
        return "success";
    }
	
	@GetMapping("api/store/{idx}")
	@ResponseBody
	public ResponseEntity<StoreDto> getStoreDetail(@PathVariable("idx") int idx) {
		// 1. DB에서 해당 ID로 점포를 찾습니다.
		// 서비스에서 해당 로직을 처리하도록 구현하는 것이 좋습니다.
		StoreDto storeDto = timeSaleService.getStoreById(idx);
		
		if (storeDto != null) {
			return ResponseEntity.ok(storeDto); // 200 OK와 함께 데이터 반환
		} else {
			return ResponseEntity.notFound().build(); // 404 에러 반환
		}
	}
	
    // 수정
    @PatchMapping("api/store/{idx}")
    @ResponseBody
    public ResponseEntity<?> updateStore(@PathVariable int idx, StoreDto dto, @RequestParam(required = false) MultipartFile uploadFile) {
        timeSaleService.updateStore(idx, dto, uploadFile);
        return ResponseEntity.ok().build();
    }


    @PostMapping("api/product")
    @ResponseBody
    public String insertProduct(@RequestBody StoreProductDto productDto) {
        // DB에 상품 정보 저장 (storeIdx 외래키 연결)
        log.debug(productDto.toString());
        timeSaleService.insertProduct(productDto);
        return "success";
    }

}
