package com.acip.sourcecontrol;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SourceControlDiagnosticsService {

    private final SourceControlProperties sourceControlProperties;
    private final RepositoryOutcomeProvider repositoryOutcomeProvider;

    public SourceControlDiagnosticsService(SourceControlProperties sourceControlProperties, RepositoryOutcomeProvider repositoryOutcomeProvider) {
        this.sourceControlProperties = sourceControlProperties;
        this.repositoryOutcomeProvider = repositoryOutcomeProvider;
    }

    public SourceControlDiagnosticsReport diagnostics() {
        String provider = sourceControlProperties.effectiveProvider();
        List<SourceControlProperties.ConfiguredRepository> configuredRepositories = sourceControlProperties.configuredRepositories();
        List<RepositoryOutcomeMetrics> metrics = repositoryOutcomeProvider.repositoryMetrics();
        Map<String, SourceControlRepositoryDiagnostic> diagnostics = new LinkedHashMap<>();

        configuredRepositories.forEach(repository -> diagnostics.put(key(repository.owner(), repository.name()), new SourceControlRepositoryDiagnostic(
                repository.name(),
                repository.owner(),
                repository.teamKey(),
                true,
                false,
                null,
                null,
                null,
                null,
                null,
                null
        )));
        metrics.forEach(metric -> diagnostics.put(key(metric.owner(), metric.repository()), new SourceControlRepositoryDiagnostic(
                metric.repository(),
                metric.owner(),
                metric.teamKey(),
                diagnostics.containsKey(key(metric.owner(), metric.repository())),
                true,
                metric.prCount(),
                metric.commitCount(),
                metric.reviewCount(),
                metric.commentCount(),
                metric.averageMergeTimeHours(),
                metric.averageReviewTimeHours()
        )));

        return new SourceControlDiagnosticsReport(
                provider,
                configured(provider),
                !isBlank(sourceControlProperties.token()),
                configuredRepositories.size(),
                metrics.size(),
                repositoryOutcomeProvider.cacheState(),
                List.copyOf(diagnostics.values()),
                message(provider, configuredRepositories.size(), metrics.size())
        );
    }

    private boolean configured(String provider) {
        if ("github".equalsIgnoreCase(provider)) {
            return sourceControlProperties.isGitHubConfigured();
        }
        return "mock".equalsIgnoreCase(provider);
    }

    private String message(String provider, int configuredCount, int metricsCount) {
        if ("github".equalsIgnoreCase(provider)) {
            if (!sourceControlProperties.isGitHubConfigured()) {
                return "GitHub provider is selected, but token or repository configuration is missing.";
            }
            return "GitHub provider is configured for " + configuredCount + " repositories with " + metricsCount + " metric snapshots available.";
        }
        if ("mock".equalsIgnoreCase(provider)) {
            return "Mock source-control provider is active with " + metricsCount + " demo repository metric snapshots.";
        }
        return "Unknown source-control provider: " + provider + ".";
    }

    private String key(String owner, String repository) {
        return (owner == null ? "" : owner) + "/" + repository;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
