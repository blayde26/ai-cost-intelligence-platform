package com.acip.outcomes;

import java.math.BigDecimal;
import java.util.List;

public record OutcomeCorrelationReport(
        BigDecimal totalAiSpend,
        long teamCount,
        long aiActiveTeamCount,
        long repositoryCount,
        long aiActiveRepositoryCount,
        long repositoriesWithOutcomeMetrics,
        double averageStoryCompletionRateForAiActiveTeams,
        Double averageMergeTimeHoursForAiActiveRepositories,
        List<OutcomeCorrelationSignal> signals,
        String interpretation
) {
}
