package com.scit.soragodong.service;

import com.scit.soragodong.ai.CommunityTool;
import com.scit.soragodong.ai.FinanceTool;
import com.scit.soragodong.ai.RedisChatMemory;
import com.scit.soragodong.ai.TimesaleTool;
import com.scit.soragodong.ai.UsedMarketTool;
import com.scit.soragodong.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final FinanceService financeService;
    private final UsedService usedService;
    private final CommunityService communityService;
    private final TimesaleService timesaleService;
    private final RedisChatMemory chatMemory;

    public Flux<String> streamChat(String message, CustomUserDetails user) {
        String conversationId = "user:" + user.getUserIdx();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        String nickname = user.getUserNickname();
        String address = user.getUserAddress() != null ? user.getUserAddress() : "미설정";

        String systemPrompt = String.format(
                """
                        # 역할
                        당신은 소라고동 앱의 AI 도우미입니다.
                        사용자의 가계부, 중고거래, 커뮤니티, 타임세일 활동을 도구(tool)로 직접 처리합니다.

                        # 사용자 정보
                        - 이름: %s
                        - 오늘 날짜: %s
                        - 이번 달: %s
                        - 주소: %s

                        # 언어 규칙 (절대 준수)
                        반드시 한국어로만 답변하세요. 한자, 일본어, 중국어, 아랍어 등 비한글 문자는 한 글자도 사용하지 마세요.
                        영어 단어가 필요하면 한글 외래어로 표기하세요.

                        # 답변 형식
                        - 띄어쓰기를 반드시 지키세요. 단어 사이는 항상 공백으로 구분합니다.
                        - 문장마다 \\n으로 줄바꿈하여 말풍선에서 읽기 편하게 작성하세요.
                        - 사용자를 부를 때는 "%s님"으로 부르세요.

                        # 도구 사용 규칙
                        - 가계부 등록 요청 → recordFinance 호출
                        - 중고 물품 등록 요청 → registerUsed 호출
                        - 게시글 작성 요청 → writeBoard 호출
                        - 타임세일 예약 요청 → 상품 목록 조회 후 reserveProduct 호출
                        - 먹고 싶은 음식이나 원하는 상품/가게 문의 → searchTimesaleByKeyword 호출 후 반환된 링크([가게명](/timesale/detail?storeIdx=N))를 그대로 출력
                        - 도구 실행 후 결과를 친근하게 안내하세요.
                        - 위 기능과 무관한 요청이면 도구를 호출하지 말고 안내 메시지만 반환하세요.
                        """,
                nickname, today, currentYearMonth, address, nickname);

        // 이전 대화 이력 로드 (Tomcat 스레드에서 동기 호출 — 블로킹 허용)
        List<Message> history = List.of();
        try {
            history = chatMemory.get(conversationId);
        } catch (Exception e) {
            log.warn("[AI] 대화 이력 로드 실패 - conversationId={}, error={}", conversationId, e.getMessage());
        }

        // 전체 메시지: System + 이전 대화 이력 + 현재 사용자 입력
        List<Message> allMessages = new ArrayList<>();
        allMessages.add(new SystemMessage(systemPrompt));
        allMessages.addAll(history);
        allMessages.add(new UserMessage(message));

        // 스트리밍 응답 누적 버퍼
        StringBuilder responseBuffer = new StringBuilder();

        return chatClientBuilder.build()
                .prompt(new Prompt(allMessages))
                .tools(
                        new FinanceTool(financeService, user.getUserIdx()),
                        new UsedMarketTool(usedService, user.getUserIdx(),
                                user.getUserLat(), user.getUserLng(), user.getUserAddress()),
                        new CommunityTool(communityService, user.getUserIdx()),
                        new TimesaleTool(timesaleService, user.getUserIdx(),
                                user.getUserLat(), user.getUserLng()))
                .stream()
                .content()
                .doOnNext(responseBuffer::append)
                .doOnComplete(() ->
                // 비동기 저장 — Netty IO 스레드 블로킹 방지
                CompletableFuture.runAsync(() -> {
                    try {
                        String assistantText = responseBuffer.toString();
                        if (!assistantText.isBlank()) {
                            chatMemory.add(conversationId, List.of(
                                    new UserMessage(message),
                                    new AssistantMessage(assistantText)));
                        }
                    } catch (Exception e) {
                        log.warn("[AI] 대화 저장 실패 - conversationId={}, error={}", conversationId, e.getMessage());
                    }
                }))
                .timeout(Duration.ofSeconds(60))
                .onErrorResume(e -> {
                    boolean isRateLimit = e instanceof WebClientResponseException wce
                            && wce.getStatusCode().value() == 429
                            || e.getMessage() != null && e.getMessage().contains("429");
                    if (isRateLimit) {
                        log.warn("[AI] 요청 초과(429) - userId={}", user.getUserIdx());
                        return Flux.just("요청이 너무 많아요.\n잠시 후 다시 시도해 주세요. 🐚");
                    }
                    log.error("[AI] 스트리밍 오류 - userId={}, error={}", user.getUserIdx(), e.getMessage());
                    return Flux.just("죄송해요, 응답을 생성하는 중에 문제가 생겼어요. 잠시 후 다시 시도해 주세요. 😅");
                });
    }

    public List<Map<String, Object>> getHistory(Integer userIdx) {
        return chatMemory.getChatHistory("user:" + userIdx);
    }

    public void clearHistory(Integer userIdx) {
        chatMemory.clear("user:" + userIdx);
    }
}
