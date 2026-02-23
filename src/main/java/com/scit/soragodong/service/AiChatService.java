package com.scit.soragodong.service;

import com.scit.soragodong.ai.CommunityTool;
import com.scit.soragodong.ai.FinanceTool;
import com.scit.soragodong.ai.TimesaleTool;
import com.scit.soragodong.ai.UsedMarketTool;
import com.scit.soragodong.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final FinanceService financeService;
    private final UsedService usedService;
    private final CommunityService communityService;
    private final TimesaleService timesaleService;

    public Flux<String> streamChat(String message, CustomUserDetails user) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        String systemPrompt = String.format("""
                당신은 소라고동 앱의 AI 도우미 '소라고동'입니다.
                사용자의 가계부, 중고거래, 커뮤니티, 타임세일 활동을 도구(tool)를 사용하여 직접 처리합니다.
                답변은 항상 한국어로, 간결하고 친근하게 해주세요.

                오늘 날짜: %s
                이번 달: %s
                사용자 닉네임: %s
                사용자 주소: %s

                중요 규칙:
                - 가계부 등록 요청이 오면 반드시 recordFinance 도구를 호출하여 실제로 저장하세요.
                - 중고 물품 등록 요청이 오면 registerUsed 도구를 호출하세요.
                - 게시글 작성 요청이 오면 writeBoard 도구를 호출하세요.
                - 타임세일 예약 요청이 오면 먼저 상품 목록을 조회한 후 reserveProduct 도구를 호출하세요.
                - 도구 실행 후 결과를 사용자에게 친근하게 알려주세요.
                """,
                today, currentYearMonth,
                user.getUserNickname(),
                user.getUserAddress() != null ? user.getUserAddress() : "미설정");

        return chatClientBuilder.build()
                .prompt()
                .system(systemPrompt)
                .user(message)
                .tools(
                        new FinanceTool(financeService, user.getUserIdx()),
                        new UsedMarketTool(usedService, user.getUserIdx(),
                                user.getUserLat(), user.getUserLng(), user.getUserAddress()),
                        new CommunityTool(communityService, user.getUserIdx()),
                        new TimesaleTool(timesaleService, user.getUserIdx())
                )
                .stream()
                .content();
    }
}
