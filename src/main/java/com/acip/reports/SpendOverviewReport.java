package com.acip.reports;

import java.math.BigDecimal;

public record SpendOverviewReport(
        BigDecimal totalSpend,
        long totalTokens,
        long totalRequests
) {
}
