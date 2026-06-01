package com.acip.outcomes;

import java.math.BigDecimal;

public record RepositoryAnalyticsSnapshot(
        String repository,
        BigDecimal aiSpend,
        long aiRequestCount,
        long totalTokens,
        long attributedEventCount,
        long unattributedEventCount,
        double attributionCoveragePercent,
        Long prCount,
        Double averageMergeTimeHours,
        Double averageReviewTimeHours,
        OutcomeDataStatus outcomeDataStatus,
        String interpretation
) {
}
