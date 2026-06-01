package com.acip.outcomes;

import java.math.BigDecimal;

public record TeamAnalyticsSnapshot(
        String teamKey,
        BigDecimal aiSpend,
        long aiRequestCount,
        long storyCount,
        long completedStoryCount,
        long cancelledStoryCount,
        double storyCompletionRate,
        double cancelledStoryRate,
        double capitalizedWorkRate,
        double operationalWorkRate,
        double researchWorkRate,
        Long prCount,
        Double averageMergeTimeHours,
        Double averageReviewTimeHours,
        OutcomeDataStatus outcomeDataStatus,
        String interpretation
) {
}
