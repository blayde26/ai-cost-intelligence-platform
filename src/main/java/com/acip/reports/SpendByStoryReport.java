package com.acip.reports;

import java.math.BigDecimal;

public record SpendByStoryReport(
        String storyKey,
        String storyName,
        String epicKey,
        String teamKey,
        BigDecimal totalCost,
        long totalTokens,
        long requestCount,
        String storySummary,
        BigDecimal estimatedCostUsd
) {
    public SpendByStoryReport(
            String storyKey,
            String storyName,
            String epicKey,
            String teamKey,
            BigDecimal totalCost,
            long totalTokens,
            long requestCount
    ) {
        this(storyKey, storyName, epicKey, teamKey, totalCost, totalTokens, requestCount, storyName, totalCost);
    }
}
