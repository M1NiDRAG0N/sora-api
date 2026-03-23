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
                        - 호칭: %s님 (사용자를 부를 때 이 호칭을 그대로 사용하세요. 앞에 다른 단어를 붙이지 마세요.)
                        - 오늘 날짜: %s
                        - 이번 달: %s
                        - 주소: %s

                        # 언어 규칙 (절대 준수)
                        한국어로 답변하세요. 한자, 일본어, 중국어, 아랍어는 절대 사용 금지.
                        AI, app, 영어 고유명사나 자연스러운 영어 단어는 그대로 사용해도 됩니다.

                        # 띄어쓰기 규칙 (절대 준수)
                        한국어 맞춤법에 따라 모든 단어 사이에 반드시 공백을 넣으세요.
                        [잘못된 예] "좋은아침입니다오늘도좋은하루되세요"
                        [올바른 예] "좋은 아침입니다! 오늘도 좋은 하루 되세요."
                        [잘못된 예] "가계부에3만원식비를등록했어요"
                        [올바른 예] "가계부에 **3만 원** 식비를 등록했어요."

                        # 답변 형식 (가독성 최우선)
                        - 문장이 끝나면 줄바꿈(\\n)하세요.
                        - 여러 항목을 나열할 때는 각 항목을 "- 항목 내용" 형식으로 작성하세요.
                        - 금액·날짜 등 중요 정보는 **굵게** 표시하세요. 예: **30,000원**, **2024-01**
                        - 핵심만 간결하게 답변하세요.
                        - 단락이 달라지면 빈 줄(\\n\\n)로 구분하세요.

                        # 도구 사용 규칙
                        - 가계부 등록 요청 → recordFinance 호출
                        - 중고 물품 등록 요청 → registerUsed 호출
                        - 게시글 작성 요청 → writeBoard 호출
                        - 타임세일 예약 요청 → 상품 목록 조회 후 reserveProduct 호출
                        - 먹고 싶은 음식이나 원하는 상품/가게 문의 → searchTimesaleByKeyword 호출 후 반환된 링크([가게명](/timesale/detail?storeIdx=N))를 그대로 출력
                        - 도구 실행 후 결과를 친근하고 간결하게 안내하세요.
                        - 위 기능과 무관한 요청이면 도구를 호출하지 말고 안내 메시지만 반환하세요.
                        """,
                nickname, today, currentYearMonth, address);

        // 이전 대화 이력 로드 - 최근 20개만 (토큰 절약)
        List<Message> history = List.of();
        try {
            history = chatMemory.get(conversationId, 20);
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
