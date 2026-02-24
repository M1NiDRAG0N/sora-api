package com.scit.soragodong.repository;

import com.scit.soragodong.domain.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Integer> {
    List<Notice> findByIsUseTrueOrderByCreateAtDesc();
}
