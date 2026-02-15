package com.scit.soragodong.service;

import com.scit.soragodong.domain.dto.FinanceDto;
import com.scit.soragodong.domain.entity.Finance;
import com.scit.soragodong.domain.entity.Users;
import com.scit.soragodong.repository.FinanceRepository;
import com.scit.soragodong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FinanceService {

    private final FinanceRepository financeRepository;
    private final UserRepository userRepository;

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
                // (2) 신규 저장 모드 (이 부분은 이미 잘 짜여져 있습니다!)
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
     * [조회] 해당 유저의 모든 내역 가져오기 (최신순 정렬 등은 Repository에서 처리하거나 여기서 정렬)
     * 
     * @param userIdx 로그인한 유저 ID
     * @return DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<FinanceDto> findAll(Integer userIdx) {
        // 1. 유저 확인
        Users user = userRepository.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 없습니다."));

        // 2. DB 조회 및 DTO 변환
        // financeRepository.findAllByUser(user) -> List<Finance>
        // stream().map(FinanceDto::fromEntity) -> List<FinanceDto>
        return financeRepository.findAllByUser(user).stream()
                .map(FinanceDto::fromEntity)
                .collect(Collectors.toList());
    }

    // FinanceService.java 에 추가

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
    
}