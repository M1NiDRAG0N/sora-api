package com.scit.soragodong.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.scit.soragodong.domain.entity.LikeCount;
import com.scit.soragodong.domain.entity.LikeCountKey;

@Repository
public interface LikeCountRepository extends JpaRepository<LikeCount, LikeCountKey> {

}
