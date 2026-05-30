package com.acip.reports;

import java.math.BigDecimal;

public record SpendByStoryReport(
        String storyKey,
        String storySummary,
        String epicKey,
        String teamKey,
        long requestCount,
        long totalTokens,
        BigDecimal estimatedCostUsd
) {
}
