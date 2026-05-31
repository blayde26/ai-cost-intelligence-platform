package com.acip.reports;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AttributionCoverageService {

    private final AttributionReportRepository attributionReportRepository;

    public AttributionCoverageService(AttributionReportRepository attributionReportRepository) {
        this.attributionReportRepository = attributionReportRepository;
    }

    public AttributionCoverageReport coverage() {
        return finalizeCoverage(attributionReportRepository.coverage());
    }

    public AttributionCoverageReport coverage(ReportDateRange range) {
        return finalizeCoverage(attributionReportRepository.coverage(range));
    }

    private AttributionCoverageReport finalizeCoverage(AttributionCoverageReport raw) {
        double coveragePercent = BigDecimal.ZERO.compareTo(raw.totalCost()) == 0
                ? 0.0
                : raw.attributedCost()
                .multiply(new BigDecimal("100"))
                .divide(raw.totalCost(), 2, RoundingMode.HALF_UP)
                .doubleValue();
        return new AttributionCoverageReport(
                raw.totalCost(),
                raw.attributedCost(),
                raw.unattributedCost(),
                coveragePercent,
                raw.eventCount(),
                raw.validEventCount(),
                raw.invalidEventCount()
        );
    }
}
