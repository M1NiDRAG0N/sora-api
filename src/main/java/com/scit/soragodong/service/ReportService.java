package com.scit.soragodong.service;

import com.scit.soragodong.domain.dto.ReportRequest;
import com.scit.soragodong.domain.entity.Report;
import com.scit.soragodong.domain.enums.ReportStatus;
import com.scit.soragodong.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;

    public void submitReport(int reporterIdx, ReportRequest request) {
        Report report = Report.builder()
                .reporterIdx(reporterIdx)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PROCESSING.name())
                .build();
        
        reportRepository.save(report);
    }
}
