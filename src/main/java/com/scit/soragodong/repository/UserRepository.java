package com.scit.soragodong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    
    /**
     * 닉네임 중복 확인
     * @param nickname
     * @return
     */
    boolean existsByUserNickname(String nickname);
    
    /**
     * 비밀번호 업데이트
     * @param email
     * @param password
     */
    @Modifying
    @Query("UPDATE Users u SET u.password = :password WHERE u.userEmail = :email")
    void updatePassword(@Param("email") String email, @Param("password") String password);
}
