package com.acip.sourcecontrol;

public record SourceControlRepositoryDiagnostic(
        String repository,
        String owner,
        String teamKey,
        boolean configured,
        boolean metricsAvailable,
        Long prCount,
        Long commitCount,
        Long reviewCount,
        Long commentCount,
        Double averageMergeTimeHours,
        Double averageReviewTimeHours
) {
}
