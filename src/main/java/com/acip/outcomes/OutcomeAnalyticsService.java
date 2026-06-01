package com.acip.outcomes;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class OutcomeAnalyticsService {

    private final OutcomeProvider outcomeProvider;

    public OutcomeAnalyticsService(OutcomeProvider outcomeProvider) {
        this.outcomeProvider = outcomeProvider;
    }

    public OutcomeAnalyticsReport report() {
        List<TeamAnalyticsSnapshot> teams = teamSnapshots();
        List<RepositoryAnalyticsSnapshot> repositories = repositorySnapshots();
        return new OutcomeAnalyticsReport(teams, repositories, correlations(teams, repositories));
    }

    public List<TeamAnalyticsSnapshot> teamSnapshots() {
        return outcomeProvider.teamSnapshots();
    }

    public List<RepositoryAnalyticsSnapshot> repositorySnapshots() {
        return outcomeProvider.repositorySnapshots();
    }

    public OutcomeCorrelationReport correlations() {
        return correlations(teamSnapshots(), repositorySnapshots());
    }

    private OutcomeCorrelationReport correlations(List<TeamAnalyticsSnapshot> teams, List<RepositoryAnalyticsSnapshot> repositories) {
        List<TeamAnalyticsSnapshot> aiActiveTeams = teams.stream()
                .filter(team -> isPositive(team.aiSpend()))
                .toList();
        List<RepositoryAnalyticsSnapshot> aiActiveRepositories = repositories.stream()
                .filter(repository -> isPositive(repository.aiSpend()))
                .toList();
        List<RepositoryAnalyticsSnapshot> repositoriesWithOutcomeMetrics = repositories.stream()
                .filter(repository -> repository.prCount() != null || repository.commitCount() != null)
                .toList();
        BigDecimal totalAiSpend = teams.stream()
                .map(TeamAnalyticsSnapshot::aiSpend)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<OutcomeCorrelationSignal> signals = Stream.concat(
                        aiActiveTeams.stream().map(this::teamSignal),
                        repositories.stream().map(this::repositorySignal)
                )
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(OutcomeCorrelationSignal::aiSpend).reversed()
                        .thenComparing(OutcomeCorrelationSignal::subjectKey))
                .limit(8)
                .toList();
        return new OutcomeCorrelationReport(
                totalAiSpend,
                teams.size(),
                aiActiveTeams.size(),
                repositories.size(),
                aiActiveRepositories.size(),
                repositoriesWithOutcomeMetrics.size(),
                average(aiActiveTeams.stream().map(TeamAnalyticsSnapshot::storyCompletionRate).toList()),
                nullableAverage(aiActiveRepositories.stream()
                        .map(RepositoryAnalyticsSnapshot::averageMergeTimeHours)
                        .filter(Objects::nonNull)
                        .toList()),
                signals,
                "Correlation signals compare AI spend with delivery and source-control outcomes. They are directional diagnostics, not causal claims."
        );
    }

    private OutcomeCorrelationSignal teamSignal(TeamAnalyticsSnapshot team) {
        String signal = team.cancelledStoryRate() >= 20.0
                ? "AI spend appears alongside elevated cancelled work."
                : "AI spend appears alongside story outcome data.";
        return new OutcomeCorrelationSignal(
                "TEAM",
                team.teamKey(),
                team.aiSpend(),
                "storyCompletionRate",
                team.storyCompletionRate(),
                signal,
                "Use this to decide where to inspect work mix, attribution quality, and delivery context before drawing conclusions."
        );
    }

    private OutcomeCorrelationSignal repositorySignal(RepositoryAnalyticsSnapshot repository) {
        if (repository.prCount() == null && repository.commitCount() == null) {
            return null;
        }
        String metric = repository.averageMergeTimeHours() == null ? "prCount" : "averageMergeTimeHours";
        double value = repository.averageMergeTimeHours() == null ? nullToZero(repository.prCount()) : repository.averageMergeTimeHours();
        String signal = isPositive(repository.aiSpend()) && repository.prCount() != null
                ? "AI spend and source-control activity are both visible for this repository."
                : "Source-control activity is visible before AI spend has been attached.";
        return new OutcomeCorrelationSignal(
                "REPOSITORY",
                repository.repository(),
                repository.aiSpend(),
                metric,
                value,
                signal,
                "Compare spend, attribution coverage, PR activity, and review timing together rather than using one metric alone."
        );
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        return round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
    }

    private Double nullableAverage(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        return average(values);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double nullToZero(Long value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}
