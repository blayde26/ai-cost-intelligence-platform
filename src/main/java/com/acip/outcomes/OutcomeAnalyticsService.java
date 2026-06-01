package com.acip.outcomes;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutcomeAnalyticsService {

    private final OutcomeProvider outcomeProvider;

    public OutcomeAnalyticsService(OutcomeProvider outcomeProvider) {
        this.outcomeProvider = outcomeProvider;
    }

    public OutcomeAnalyticsReport report() {
        return new OutcomeAnalyticsReport(teamSnapshots(), repositorySnapshots());
    }

    public List<TeamAnalyticsSnapshot> teamSnapshots() {
        return outcomeProvider.teamSnapshots();
    }

    public List<RepositoryAnalyticsSnapshot> repositorySnapshots() {
        return outcomeProvider.repositorySnapshots();
    }
}
