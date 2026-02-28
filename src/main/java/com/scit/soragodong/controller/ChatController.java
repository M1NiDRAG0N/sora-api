package com.scit.soragodong.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.scit.soragodong.domain.dto.ChatMessageDto;
import com.scit.soragodong.domain.dto.ChatRoomCreateDto;
import com.scit.soragodong.domain.entity.ChatRoom;
import com.scit.soragodong.domain.entity.Used;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.domain.enums.FileRefType;
import com.scit.soragodong.repository.ChatRoomRepository;
import com.scit.soragodong.repository.FileGrpRepository;
import com.scit.soragodong.repository.UsedRepository;
import com.scit.soragodong.repository.UserRepository;
import com.scit.soragodong.security.CustomUserDetails;
import com.scit.soragodong.service.ChatService;
import com.scit.soragodong.service.SseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    @Value("${google.maps.api-key:default-key}")
    private String googleMapsApiKey;

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final UsedRepository usedRepository;
    private final FileGrpRepository fileGrpRepository;
    private final SseService sseService;

    @GetMapping("/chatList")
    public String chatList(Model model) {
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        model.addAttribute("currentUri", "/chatList");
        return "common";
    }

    @ResponseBody
    @PostMapping("/api/chat/room")
    public Integer createOrGetChatRoom(@RequestBody ChatRoomCreateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer loginUserIdx = userDetails != null ? userDetails.getUserIdx() : 1;
        return chatService.createOrGetChatRoom(loginUserIdx, dto.otherUserIdx(), dto.type(), dto.usedIdx());
    }

    @GetMapping("/chat/room/{roomId}")
    public String chatRoom(@PathVariable("roomId") Integer roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        model.addAttribute("currentUri", "/chat/room");
        model.addAttribute("roomId", roomId);

        Integer loginUserIdx = userDetails != null ? userDetails.getUserIdx() : 1;
        String loginUserNickname = userDetails != null ? userDetails.getUserNickname() : "테스트유저";

        model.addAttribute("loginUserIdx", loginUserIdx);
        model.addAttribute("loginUserNickname", loginUserNickname);

        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        if (room != null) {
            model.addAttribute("chatRoomType", room.getRoomType());
            Integer otherUserIdx = room.getUser1Idx().equals(loginUserIdx) ? room.getUser2Idx() : room.getUser1Idx();
            Users otherUser = userRepository.findById(otherUserIdx).orElse(null);

            if (otherUser != null) {
                model.addAttribute("partnerNickname", otherUser.getUserNickname());
                model.addAttribute("partnerProfileImg", otherUser.getProfileIdx());
            }

            if ("USED".equals(room.getRoomType()) && room.getUsedIdx() != null) {
                Used used = usedRepository.findById(room.getUsedIdx()).orElse(null);
                if (used != null) {
                    model.addAttribute("usedIdx", used.getUsedIdx());
                    model.addAttribute("isOwner", used.getCreatedUser().equals(loginUserIdx));
                    model.addAttribute("productName", used.getUsedTitle());
                    model.addAttribute("productPrice", used.getUsedPrice());
                    model.addAttribute("productStatus", used.getUsedState().name());
                    String productImg = fileGrpRepository.findByRefTypeAndRefId(FileRefType.USED, used.getUsedIdx())
                            .map(grp -> "group/" + grp.getFileGrpIdx())
                            .orElse(null);
                    model.addAttribute("productImg", productImg);
                }
            }
        } else {
            model.addAttribute("chatRoomType", "NORMAL");
        }

        return "common";
    }

    @MessageMapping("/chat/message")
    public void sendMessage(ChatMessageDto message) {
        log.info("수신된 메시지: {}", message);
        ChatMessageDto messageToSave = message.withCreatedAt(
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        chatService.saveMessage(messageToSave);
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.roomId(), messageToSave);

        // 수신자에게 SSE 알림 전송
        ChatRoom room = chatRoomRepository.findById(message.roomId()).orElse(null);
        if (room != null) {
            Integer receiverIdx = room.getUser1Idx().equals(message.senderIdx()) ? room.getUser2Idx()
                    : room.getUser1Idx();
            sseService.notify(receiverIdx, "CHAT", message.roomId(), "새로운 채팅 메시지가 도착했습니다.");
        }
    }

    @ResponseBody
    @GetMapping("/api/chat/history/{roomId}")
    public List<Object> getChatHistory(@PathVariable("roomId") Integer roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            chatService.markAsRead(roomId, userDetails.getUserIdx());
        }
        return chatService.getChatHistory(roomId);
    }

    @ResponseBody
    @PostMapping("/api/chat/read/{roomId}")
    public void markAsRead(@PathVariable("roomId") Integer roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            chatService.markAsRead(roomId, userDetails.getUserIdx());
        }
    }

    @ResponseBody
    @GetMapping("/api/chat/list")
    public List<?> getChatList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer loginUserId = userDetails != null ? userDetails.getUserIdx() : 1;
        return chatService.getUserChatList(loginUserId);
    }
}
