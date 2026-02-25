package com.scit.soragodong.domain.entity;

import com.scit.soragodong.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CHAT_ROOM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHAT_ROOM_IDX")
    private Integer chatRoomIdx;

    @Column(name = "ROOM_TYPE", length = 20, nullable = false)
    private String roomType; // "NORMAL" or "USED"

    @Column(name = "USER1_IDX", nullable = false)
    private Integer user1Idx;

    @Column(name = "USER2_IDX", nullable = false)
    private Integer user2Idx;

    @Column(name = "USED_IDX")
    private Integer usedIdx;
}
