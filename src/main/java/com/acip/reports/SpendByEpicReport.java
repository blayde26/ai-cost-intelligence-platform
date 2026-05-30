package com.acip.reports;

import java.math.BigDecimal;

public record SpendByEpicReport(
        String epicKey,
        String epicSummary,
        String teamKey,
        long requestCount,
        long totalTokens,
        BigDecimal estimatedCostUsd
) {
}
