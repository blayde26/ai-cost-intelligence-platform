package com.acip.reports;

import java.math.BigDecimal;

public record SpendByTeamReport(
        String teamKey,
        BigDecimal totalCost,
        long totalTokens,
        long requestCount,
        long epicCount,
        long storyCount,
        BigDecimal estimatedCostUsd
) {
    public SpendByTeamReport(
            String teamKey,
            BigDecimal totalCost,
            long totalTokens,
            long requestCount,
            long epicCount,
            long storyCount
    ) {
        this(teamKey, totalCost, totalTokens, requestCount, epicCount, storyCount, totalCost);
    }
}
