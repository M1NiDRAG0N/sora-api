package com.scit.soragodong.repository;

import com.scit.soragodong.domain.entity.MonthlyBudget;
import com.scit.soragodong.domain.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {
    // 유저와 해당 월(yearMonth)로 예산 찾기
    Optional<MonthlyBudget> findByUserAndYearMonth(Users user, String yearMonth);
}