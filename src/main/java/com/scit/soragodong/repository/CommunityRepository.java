package com.scit.soragodong.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.scit.soragodong.domain.entity.Board;

@Repository
public interface CommunityRepository extends JpaRepository<Board, Integer> {

}
