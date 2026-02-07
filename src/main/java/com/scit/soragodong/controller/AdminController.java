package com.scit.soragodong.controller;

import com.scit.soragodong.domain.dto.StoreDto;
import com.scit.soragodong.service.TimesaleService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String store(@RequestParam(value = "path", required = false, defaultValue = "전체지역") String path, Model model) {
        // 1. DB에서 모든 점포 정보 가져오기 (Service 호출)
        // "전체지역" 텍스트 자체는 주소 검색에서 제외하기 위해 처리
        String searchPath = path.equals("전체지역") ? "" : path.replace("전체지역", "").trim();
        List<StoreDto> allStores = timeSaleService.getAllStores();

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

        model.addAttribute("currentPath", path);
        model.addAttribute("subFolders", subFolders);    // 하위 폴더 리스트
        model.addAttribute("currentStores", currentStores); // 현재 위치의 점포 리스트
        return "admin/store";
    }
    @ResponseBody
    @PostMapping("api/store")
    public ResponseEntity<String> registerStore(@RequestBody StoreDto storeDto) {
        try {
            // DB 저장 로직 수행
            boolean isSaved = timeSaleService.createStore(storeDto);

            if (isSaved) {
                return ResponseEntity.ok("Success");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fail");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
