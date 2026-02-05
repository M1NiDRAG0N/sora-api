package com.scit.soragodong.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.scit.soragodong.domain.response.ApiResponse;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MainController {

    @GetMapping("/main")
    public String mainPage(Model model) {
        model.addAttribute("currentUri", "/main");
        return "common";
    }

    @GetMapping("/finance")
    public String financePage(Model model) {
        model.addAttribute("currentUri", "/finance");
        return "common";
    }

    /**
     * 세션 정보 확인 (테스트용)
     */
    @GetMapping("/session-info")
    @ResponseBody
    public ApiResponse<?> sessionInfo(HttpSession session) {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", session.getId());
        sessionData.put("userEmail", session.getAttribute("userEmail"));
        sessionData.put("creationTime", session.getCreationTime());
        sessionData.put("lastAccessedTime", session.getLastAccessedTime());
        sessionData.put("maxInactiveInterval", session.getMaxInactiveInterval());

        return ApiResponse.success(sessionData);
    }
}
