package com.acip.reports;

import java.math.BigDecimal;

public record AttributionCoverageReport(
        BigDecimal totalCost,
        BigDecimal attributedCost,
        BigDecimal unattributedCost,
        double coveragePercent,
        long eventCount,
        long validEventCount,
        long invalidEventCount
) {
}
