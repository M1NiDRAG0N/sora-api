package com.scit.soragodong.service;

import com.scit.soragodong.domain.dto.FinanceDto;
import com.scit.soragodong.domain.entity.Finance;
import com.scit.soragodong.domain.entity.MonthlyBudget; // [추가됨] 월별 예산 엔티티
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.repository.FinanceRepository;
import com.scit.soragodong.repository.MonthlyBudgetRepository; // [추가됨] 월별 예산 레포지토리
import com.scit.soragodong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FinanceService {

    private final FinanceRepository financeRepository;
    private final UserRepository userRepository;
    private final MonthlyBudgetRepository monthlyBudgetRepository; // [추가됨] 월별 예산 DB 연결

    /**
     * [저장 및 수정] 가계부 내역 쓰기
     */
    public void write(FinanceDto dto, Integer userIdx) {
        // 1. 유저 정보 조회
        Users user = userRepository.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 존재하지 않습니다. ID=" + userIdx));

        try {
            // 2. 날짜와 시간 합치기
            String dateTimeStr = dto.getDate() + " " + dto.getTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime financeAt = LocalDateTime.parse(dateTimeStr, formatter);

            // 3. 타입 변환 (plus/minus -> +/-)
            String typeCode = "plus".equals(dto.getType()) ? "+" : "-";

            // 4. [분기 처리] 수정인가? 신규인가?
            if (dto.getFinanceIdx() != null) {
                // (1) 수정 모드
                Finance finance = financeRepository.findById(dto.getFinanceIdx())
                        .orElseThrow(() -> new IllegalArgumentException("수정할 내역이 없습니다. ID=" + dto.getFinanceIdx()));

                // ★ 이 부분의 순서와 인자를 정확히 맞춰주세요
                finance.updateFinance(
                        dto.getCategory(),
                        dto.getAmount(),
                        typeCode,
                        dto.getMemo(),
                        financeAt, // finance.getFinanceAt() 대신 financeAt을 넣어야 날짜 수정이 반영됩니다.
                        dto.getIsFixed(), // 순서: isFixed
                        dto.getDuration() // 순서: duration (Entity 메서드 정의에 맞게 추가)
                );

                log.info("가계부 수정 완료: ID={}, 기간={}개월", dto.getFinanceIdx(), dto.getDuration());
            } else {
                // (2) 신규 저장 모드
                Finance finance = Finance.builder()
                        .user(user)
                        .financeCategory(dto.getCategory())
                        .financeAmount(dto.getAmount())
                        .financeType(typeCode)
                        .financeMemo(dto.getMemo())
                        .financeAt(financeAt)
                        .isFixed(dto.getIsFixed())
                        .duration(dto.getDuration())
                        .build();

                financeRepository.save(finance);
                log.info("가계부 신규 저장 완료: 기간={}개월", dto.getDuration());
            }
        } catch (Exception e) {
            log.error("가계부 처리 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("가계부 처리 실패", e);
        }
    }

    /**
     * [조회] 해당 유저의 모든 내역 가져오기
     */
    @Transactional(readOnly = true)
    public List<FinanceDto> findAll(Integer userIdx) {
        // 1. 유저 확인
        Users user = userRepository.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 없습니다."));

        return financeRepository.findAllByUser(user).stream()
                .map(FinanceDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * [삭제] 가계부 내역 삭제
     */
    public void delete(Integer financeIdx) {
        // 1. 해당 내역이 존재하는지 확인
        Finance finance = financeRepository.findById(financeIdx)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 내역이 없습니다. ID=" + financeIdx));

        // 2. 삭제 실행
        financeRepository.delete(finance);
        log.info("가계부 내역 삭제 완료: ID={}", financeIdx);
    }

    // ========================================================
    // [아래부터 월별 예산 시스템으로 완전히 교체된 부분입니다]
    // ========================================================

    /**
     * [추가/수정] 특정 월의 예산 설정/업데이트
     */
    public void updateBudget(Integer userIdx, String yearMonth, Integer amount) {
        Users user = userRepository.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 없습니다."));

        // 해당 월의 예산이 DB에 이미 있는지 확인
        Optional<MonthlyBudget> existing = monthlyBudgetRepository.findByUserAndYearMonth(user, yearMonth);

        if (existing.isPresent()) {
            // 이미 설정한 적이 있다면 금액만 수정 (Dirty Checking으로 자동 UPDATE 됨)
            existing.get().updateAmount(amount);
            log.info("유저 예산 수정 완료: ID={}, 월={}, 예산={}", userIdx, yearMonth, amount);
        } else {
            // 처음 설정하는 달이라면 새로 생성해서 DB에 저장 (INSERT)
            MonthlyBudget newBudget = MonthlyBudget.builder()
                    .user(user)
                    .yearMonth(yearMonth)
                    .budgetAmount(amount)
                    .build();
            monthlyBudgetRepository.save(newBudget);
            log.info("유저 예산 신규 생성 완료: ID={}, 월={}, 예산={}", userIdx, yearMonth, amount);
        }
    }

    /**
     * [조회] 특정 월의 예산 가져오기
     */
    @Transactional(readOnly = true)
    public Integer getBudget(Integer userIdx, String yearMonth) {
        Users user = userRepository.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 없습니다."));

        // DB에서 해당 월의 예산을 찾아서 리턴, 없으면 기본값 0 리턴
        return monthlyBudgetRepository.findByUserAndYearMonth(user, yearMonth)
                .map(MonthlyBudget::getBudgetAmount)
                .orElse(0);
    }

}