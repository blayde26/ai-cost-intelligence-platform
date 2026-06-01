package com.acip.sourcecontrol;

public record RepositoryOutcomeMetrics(
        String repository,
        String owner,
        String teamKey,
        long prCount,
        long commitCount,
        long reviewCount,
        long commentCount,
        double averageMergeTimeHours,
        double averageReviewTimeHours
) {
}
