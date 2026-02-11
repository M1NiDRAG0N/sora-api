package com.scit.soragodong.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.scit.soragodong.domain.entity.Board;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface BoardRepository extends JpaRepository<Board, Integer> {
    Page<Board> findByIsUseTrue(Pageable pageable);
}
