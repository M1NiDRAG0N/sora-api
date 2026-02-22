package com.scit.soragodong.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.scit.soragodong.domain.entity.Finance;
import com.scit.soragodong.domain.entity.Users; // 유저 정보를 쓰기 위해 필요
import java.util.List;

@Repository
public interface FinanceRepository extends JpaRepository<Finance, Integer> {

    // [필수 기능] 특정 유저의 가계부 내역만 가져오는 메서드
    // (이게 없으면 남의 가계부까지 다 보이니까 꼭 있어야 해요!)
    List<Finance> findAllByUser(Users user);

    // (선택) 날짜 최신순으로 정렬해서 가져오고 싶다면 이걸 쓰면 됩니다.
    // List<Finance> findAllByUserOrderByFinanceAtDesc(Users user);
}