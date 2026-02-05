package com.scit.soragodong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scit.soragodong.domain.entity.Users;

public interface UserRepository extends JpaRepository<Users, Integer> {
    /**
     * 
     * @param email
     * @return
     */
    Optional<Users> findByUserEmail(String email);
    
    /**
     * 이메일 중복 확인
     * @param email
     * @return
     */
    boolean existsByUserEmail(String email);
}
