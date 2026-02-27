package com.scit.soragodong.service;

import com.scit.soragodong.domain.dto.FinanceDto;
import com.scit.soragodong.domain.entity.Finance;
import com.scit.soragodong.domain.entity.MonthlyBudget;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.domain.enums.NotificationType;
import com.scit.soragodong.repository.FinanceRepository;
import com.scit.soragodong.repository.MonthlyBudgetRepository;
import com.scit.soragodong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import java.util.Optional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FinanceService {

    private final FinanceRepository financeRepository;
    private final UserRepository userRepository;
    private final MonthlyBudgetRepository monthlyBudgetRepository;

    // 🚨 [추가됨] 알림 발송을 위해 NotificationService 끌어오기
    private final NotificationService notificationService;

    /**
     * [저장 및 수정] 가계부 내역 쓰기
     */
    /**
     * [저장 및 수정] 가계부 내역 쓰기 + 70% 예산 초과 알림
     */
    public void write(FinanceDto dto, Integer userIdx) {
        Users user = userRepository.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 존재하지 않습니다. ID=" + userIdx));

        try {
            String dateTimeStr = dto.getDate() + " " + dto.getTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime financeAt = LocalDateTime.parse(dateTimeStr, formatter);

            String typeCode = "plus".equals(dto.getType()) ? "+" : "-";
            Finance savedFinance;

            if (dto.getFinanceIdx() != null) {
                Finance finance = financeRepository.findById(dto.getFinanceIdx())
                        .orElseThrow(() -> new IllegalArgumentException("수정할 내역이 없습니다. ID=" + dto.getFinanceIdx()));

                finance.updateFinance(
                        dto.getCategory(),
                        dto.getAmount(),
                        typeCode,
                        dto.getMemo(),
                        financeAt,
                        dto.getIsFixed(),
                        dto.getDuration());
                savedFinance = finance;
                log.info("가계부 수정 완료: ID={}, 기간={}개월", dto.getFinanceIdx(), dto.getDuration());
            } else {
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

                savedFinance = financeRepository.save(finance);
                log.info("가계부 신규 저장 완료: 기간={}개월", dto.getDuration());
            }

            // 🚨 [수정된 로직] 지출(-)일 경우 해당 월 예산 70% 초과 검사
            if ("-".equals(typeCode)) {
                String yearMonth = financeAt.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                int targetMonth = financeAt.getMonthValue(); // 💡 입력한 내역이 몇 월인지 추출
                Long budget = getBudget(userIdx, yearMonth);

                if (budget != null && budget > 0) {
                    // 해당 월 총 지출액 계산
                    long totalExpense = financeRepository.findAllByUser(user).stream()
                            .filter(f -> "-".equals(f.getFinanceType()))
                            .filter(f -> f.getFinanceAt().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                                    .equals(yearMonth))
                            .mapToLong(Finance::getFinanceAmount)
                            .sum();

                    // 70% 이상 썼는지 검사
                    if (totalExpense >= (budget * 0.7)) {
                        // 💡 "이번 달"이라는 고정 텍스트 대신, 추출한 %d월을 삽입
                        String msg = String.format("%d월 지출이 예산의 70%%를 초과했습니다. (현재 지출: %d원)", targetMonth, totalExpense);

                        // 알림 발송
                        notificationService.send(userIdx, NotificationType.BUDGET_WARNING, savedFinance.getFinanceIdx(),
                                msg);
                        log.info("예산 70% 초과 알림 발송 완료: 유저 ID = {}, 대상 월 = {}", userIdx, targetMonth);
                    }
                }
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
        Finance finance = financeRepository.findById(financeIdx)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 내역이 없습니다. ID=" + financeIdx));

        financeRepository.delete(finance);
        log.info("가계부 내역 삭제 완료: ID={}", financeIdx);
    }

    /**
     * [추가/수정] 특정 월의 예산 설정/업데이트
     */
    public void updateBudget(Integer userIdx, String yearMonth, Long amount) {
        Users user = userRepository.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 없습니다."));

        Optional<MonthlyBudget> existing = monthlyBudgetRepository.findByUserAndYearMonth(user, yearMonth);

        if (existing.isPresent()) {
            existing.get().updateAmount(amount);
            log.info("유저 예산 수정 완료: ID={}, 월={}, 예산={}", userIdx, yearMonth, amount);
        } else {
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
    public Long getBudget(Integer userIdx, String yearMonth) {
        Users user = userRepository.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 없습니다."));

        return monthlyBudgetRepository.findByUserAndYearMonth(user, yearMonth)
                .map(MonthlyBudget::getBudgetAmount)
                .orElse(0L);
    }

}