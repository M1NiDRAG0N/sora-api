package com.scit.soragodong.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scit.soragodong.domain.entity.ChatRoom;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Integer> {

    @Query("SELECT c FROM ChatRoom c WHERE c.roomType = 'NORMAL' AND ((c.user1Idx = :u1 AND c.user2Idx = :u2) OR (c.user1Idx = :u2 AND c.user2Idx = :u1))")
    Optional<ChatRoom> findNormalRoom(@Param("u1") Integer u1, @Param("u2") Integer u2);

    @Query("SELECT c FROM ChatRoom c WHERE c.roomType = 'USED' AND c.usedIdx = :usedIdx AND ((c.user1Idx = :u1 AND c.user2Idx = :u2) OR (c.user1Idx = :u2 AND c.user2Idx = :u1))")
    Optional<ChatRoom> findUsedRoom(@Param("u1") Integer u1, @Param("u2") Integer u2, @Param("usedIdx") Integer usedIdx);

    @Query("SELECT c FROM ChatRoom c WHERE c.user1Idx = :userId OR c.user2Idx = :userId")
    List<ChatRoom> findByUserId(@Param("userId") Integer userId);
}
