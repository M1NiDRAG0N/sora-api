package com.scit.soragodong.controller;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scit.soragodong.domain.dto.AiChatRequest;
import com.scit.soragodong.security.CustomUserDetails;
import com.scit.soragodong.service.AiChatService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @RequestBody AiChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return aiChatService.streamChat(request.message(), userDetails);
    }
}
