package com.acip.outcomes;

import java.math.BigDecimal;

public record RepositoryAnalyticsSnapshot(
        String repository,
        String owner,
        String teamKey,
        BigDecimal aiSpend,
        long aiRequestCount,
        long totalTokens,
        long attributedEventCount,
        long unattributedEventCount,
        double attributionCoveragePercent,
        Long prCount,
        Long commitCount,
        Long reviewCount,
        Long commentCount,
        Double averageMergeTimeHours,
        Double averageReviewTimeHours,
        OutcomeDataStatus outcomeDataStatus,
        String interpretation
) {
}
