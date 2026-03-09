package com.scit.soragodong.service;

import com.scit.soragodong.domain.dto.ChatMessageDto;
import com.scit.soragodong.domain.dto.ChatRoomListDto;
import com.scit.soragodong.domain.entity.ChatRoom;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.domain.enums.FileRefType;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.ChatRoomRepository;
import com.scit.soragodong.repository.FileGrpRepository;
import com.scit.soragodong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final FileGrpRepository fileGrpRepository;

    @Transactional
    public Integer createOrGetChatRoom(Integer loginUserIdx, Integer otherUserIdx, String type, Integer usedIdx) {
        if ("NORMAL".equals(type)) {
            return chatRoomRepository.findNormalRoom(loginUserIdx, otherUserIdx)
                    .map(ChatRoom::getChatRoomIdx)
                    .orElseGet(() -> {
                        ChatRoom newRoom = ChatRoom.builder()
                                .roomType(type)
                                .user1Idx(loginUserIdx)
                                .user2Idx(otherUserIdx)
                                .build();
                        return chatRoomRepository.save(newRoom).getChatRoomIdx();
                    });
        } else if ("USED".equals(type)) {
            return chatRoomRepository.findUsedRoom(loginUserIdx, otherUserIdx, usedIdx)
                    .map(ChatRoom::getChatRoomIdx)
                    .orElseGet(() -> {
                        ChatRoom newRoom = ChatRoom.builder()
                                .roomType(type)
                                .user1Idx(loginUserIdx)
                                .user2Idx(otherUserIdx)
                                .usedIdx(usedIdx)
                                .build();
                        return chatRoomRepository.save(newRoom).getChatRoomIdx();
                    });
        }
        throw new IllegalArgumentException("Invalid chat room type");
    }

    @Transactional
    public void leaveChatRoom(Integer roomId, Integer userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (!room.getUser1Idx().equals(userId) && !room.getUser2Idx().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        chatRoomRepository.delete(room);
        String roomKey = "chat:room:" + roomId;
        redisTemplate.delete(roomKey);
        redisTemplate.delete(roomKey + ":read:" + room.getUser1Idx());
        redisTemplate.delete(roomKey + ":read:" + room.getUser2Idx());
    }

    public void saveMessage(ChatMessageDto message) {
        String roomKey = "chat:room:" + message.roomId();
        redisTemplate.opsForList().rightPush(roomKey, message);
    }

    public List<Object> getChatHistory(Integer roomId) {
        String roomKey = "chat:room:" + roomId;
        return redisTemplate.opsForList().range(roomKey, 0, -1);
    }

    public void markAsRead(Integer roomId, Integer userId) {
        String roomKey = "chat:room:" + roomId;
        Long totalMessages = redisTemplate.opsForList().size(roomKey);
        if (totalMessages != null) {
            String readKey = roomKey + ":read:" + userId;
            redisTemplate.opsForValue().set(readKey, totalMessages);
        }
    }

    public long getPartnerReadCount(Integer roomId, Integer loginUserIdx) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        if (room == null) return 0;
        Integer partnerIdx = room.getUser1Idx().equals(loginUserIdx) ? room.getUser2Idx() : room.getUser1Idx();
        String readKey = "chat:room:" + roomId + ":read:" + partnerIdx;
        Object readCountObj = redisTemplate.opsForValue().get(readKey);
        if (readCountObj == null) return 0;
        try {
            if (readCountObj instanceof Integer i) return i.longValue();
            else if (readCountObj instanceof Long l) return l;
            else return Long.parseLong(readCountObj.toString());
        } catch (Exception e) { return 0; }
    }

    public Integer getTotalUnreadCount(Integer userId) {
        List<ChatRoom> rooms = chatRoomRepository.findByUserId(userId);
        int totalUnread = 0;
        for (ChatRoom room : rooms) {
            String roomKey = "chat:room:" + room.getChatRoomIdx();
            Long totalMessages = redisTemplate.opsForList().size(roomKey);
            if (totalMessages == null || totalMessages == 0) continue;

            String readKey = roomKey + ":read:" + userId;
            Object readCountObj = redisTemplate.opsForValue().get(readKey);
            long readCount = 0;
            if (readCountObj != null) {
                try {
                    if (readCountObj instanceof Integer) readCount = ((Integer) readCountObj).longValue();
                    else if (readCountObj instanceof Long) readCount = (Long) readCountObj;
                    else readCount = Long.parseLong(readCountObj.toString());
                } catch (Exception e) {}
            }
            if (totalMessages > readCount) {
                totalUnread += (int) (totalMessages - readCount);
            }
        }
        return totalUnread;
    }

    public List<ChatRoomListDto> getUserChatList(Integer userId) {
        List<ChatRoom> rooms = chatRoomRepository.findByUserId(userId);
        List<ChatRoomListDto> result = new ArrayList<>();

        for (ChatRoom room : rooms) {
            Integer otherUserIdx = room.getUser1Idx().equals(userId) ? room.getUser2Idx() : room.getUser1Idx();
            Users otherUser = userRepository.findById(otherUserIdx).orElse(null);

            if (otherUser != null) {
                String roomKey = "chat:room:" + room.getChatRoomIdx();
                Object lastMsgObj = redisTemplate.opsForList().index(roomKey, -1);
                String lastMessage = "";
                String lastMessageAt = "";

                if (lastMsgObj != null) {
                    try {
                        if (lastMsgObj instanceof java.util.Map map) {
                            lastMessage = String.valueOf(map.get("message"));
                            Object createdAtObj = map.get("createdAt");
                            if (createdAtObj != null) {
                                if (createdAtObj instanceof java.util.List list && list.size() >= 5) {
                                    // [2026, 2, 25, 14, 30] 등 Jackson 배열로 직렬화된 경우
                                    lastMessageAt = String.format("%04d-%02d-%02dT%02d:%02d:%02d",
                                            ((Number)list.get(0)).intValue(),
                                            ((Number)list.get(1)).intValue(),
                                            ((Number)list.get(2)).intValue(),
                                            ((Number)list.get(3)).intValue(),
                                            ((Number)list.get(4)).intValue(),
                                            list.size() > 5 ? ((Number)list.get(5)).intValue() : 0);
                                } else {
                                    lastMessageAt = String.valueOf(createdAtObj);
                                }
                            }
                        } else if (lastMsgObj instanceof ChatMessageDto dto) {
                            lastMessage = dto.message();
                            lastMessageAt = dto.createdAt();
                        }
                    } catch (Exception e) {}
                }

                Long totalMessages = redisTemplate.opsForList().size(roomKey);
                String readKey = roomKey + ":read:" + userId;
                Object readCountObj = redisTemplate.opsForValue().get(readKey);
                long readCount = 0;
                if (readCountObj != null) {
                    try {
                        if (readCountObj instanceof Integer) readCount = ((Integer) readCountObj).longValue();
                        else if (readCountObj instanceof Long) readCount = (Long) readCountObj;
                        else readCount = Long.parseLong(readCountObj.toString());
                    } catch (Exception e) {}
                }

                int unreadCount = 0;
                if (totalMessages != null && totalMessages > readCount) {
                    unreadCount = (int) (totalMessages - readCount);
                }

                String productImg = null;
                if ("USED".equals(room.getRoomType()) && room.getUsedIdx() != null) {
                    productImg = fileGrpRepository.findByRefTypeAndRefId(FileRefType.USED, room.getUsedIdx())
                            .map(grp -> "group/" + grp.getFileGrpIdx())
                            .orElse(null);
                }

                result.add(new ChatRoomListDto(
                        room.getChatRoomIdx(),
                        room.getRoomType(),
                        otherUser.getProfileIdx(),
                        otherUser.getUserNickname(),
                        lastMessage,
                        lastMessageAt,
                        unreadCount,
                        productImg
                ));
            }
        }
        return result;
    }
}
