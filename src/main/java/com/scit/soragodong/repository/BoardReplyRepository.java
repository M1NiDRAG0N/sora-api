package com.scit.soragodong.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.scit.soragodong.domain.entity.BoardReply;

@Repository
public interface BoardReplyRepository extends JpaRepository<BoardReply, Integer> {
    List<BoardReply> findAllByBoard_BoardIdxOrderByCreatedAtAsc(Integer boardIdx);

}
