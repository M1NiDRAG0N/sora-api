package com.scit.soragodong.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.scit.soragodong.domain.entity.Board;
import com.scit.soragodong.domain.entity.BoardReply;

@Repository
public interface BoardReplyRepository extends JpaRepository<BoardReply, Integer> {
    List<BoardReply> findAllByBoard_BoardIdxAndIsUseTrueOrderByCreatedAtAsc(Integer boardIdx);

    List<BoardReply> findAllByBoard_BoardIdx(Integer boardIdx);

}
