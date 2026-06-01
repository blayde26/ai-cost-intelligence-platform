package com.acip.outcomes;

import java.util.List;

public record OutcomeAnalyticsReport(
        List<TeamAnalyticsSnapshot> teams,
        List<RepositoryAnalyticsSnapshot> repositories,
        OutcomeCorrelationReport correlations
) {
}
