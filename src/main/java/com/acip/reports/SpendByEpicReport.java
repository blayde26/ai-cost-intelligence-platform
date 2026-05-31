package com.acip.reports;

import java.math.BigDecimal;

public record SpendByEpicReport(
        String epicKey,
        String epicName,
        String teamKey,
        BigDecimal totalCost,
        long totalTokens,
        long requestCount,
        long storyCount,
        String epicSummary,
        BigDecimal estimatedCostUsd
) {
    public SpendByEpicReport(
            String epicKey,
            String epicName,
            String teamKey,
            BigDecimal totalCost,
            long totalTokens,
            long requestCount,
            long storyCount
    ) {
        this(epicKey, epicName, teamKey, totalCost, totalTokens, requestCount, storyCount, epicName, totalCost);
    }
}
