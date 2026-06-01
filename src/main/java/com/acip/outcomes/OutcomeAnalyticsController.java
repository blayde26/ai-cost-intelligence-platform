package com.acip.outcomes;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OutcomeAnalyticsController {

    private final OutcomeAnalyticsService outcomeAnalyticsService;

    public OutcomeAnalyticsController(OutcomeAnalyticsService outcomeAnalyticsService) {
        this.outcomeAnalyticsService = outcomeAnalyticsService;
    }

    @GetMapping("/api/v1/analytics/outcomes")
    public OutcomeAnalyticsReport report() {
        return outcomeAnalyticsService.report();
    }

    @GetMapping("/api/v1/analytics/team-effectiveness")
    public List<TeamAnalyticsSnapshot> teamEffectiveness() {
        return outcomeAnalyticsService.teamSnapshots();
    }

    @GetMapping("/api/v1/analytics/repositories")
    public List<RepositoryAnalyticsSnapshot> repositories() {
        return outcomeAnalyticsService.repositorySnapshots();
    }

    @GetMapping("/api/v1/analytics/correlations")
    public OutcomeCorrelationReport correlations() {
        return outcomeAnalyticsService.correlations();
    }
}
