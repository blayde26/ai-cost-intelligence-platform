package com.acip.reports;

import com.acip.usage.AttributionStatus;

import java.math.BigDecimal;
import java.util.Map;

public record UnattributedSpendSummary(
        BigDecimal totalCost,
        long totalTokens,
        long eventCount,
        Map<AttributionStatus, BigDecimal> breakdown
) {
}
