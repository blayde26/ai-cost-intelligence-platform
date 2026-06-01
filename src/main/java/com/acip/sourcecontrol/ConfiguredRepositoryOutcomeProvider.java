package com.acip.sourcecontrol;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class ConfiguredRepositoryOutcomeProvider implements RepositoryOutcomeProvider {

    private final SourceControlProperties sourceControlProperties;
    private final GitHubRepositoryClient gitHubRepositoryClient;
    private final Clock clock;
    private List<RepositoryOutcomeMetrics> cachedMetrics = List.of();
    private OffsetDateTime cacheLoadedAt;

    @Autowired
    public ConfiguredRepositoryOutcomeProvider(SourceControlProperties sourceControlProperties, GitHubRepositoryClient gitHubRepositoryClient) {
        this(sourceControlProperties, gitHubRepositoryClient, Clock.systemUTC());
    }

    ConfiguredRepositoryOutcomeProvider(SourceControlProperties sourceControlProperties, GitHubRepositoryClient gitHubRepositoryClient, Clock clock) {
        this.sourceControlProperties = sourceControlProperties;
        this.gitHubRepositoryClient = gitHubRepositoryClient;
        this.clock = clock;
    }

    @Override
    public String providerKey() {
        return sourceControlProperties.effectiveProvider().toUpperCase();
    }

    @Override
    public synchronized List<RepositoryOutcomeMetrics> repositoryMetrics() {
        if ("github".equalsIgnoreCase(sourceControlProperties.effectiveProvider())) {
            if (cacheFresh()) {
                return cachedMetrics;
            }
            cachedMetrics = sourceControlProperties.configuredRepositories().stream()
                    .map(gitHubRepositoryClient::metrics)
                    .flatMap(java.util.Optional::stream)
                    .toList();
            cacheLoadedAt = now();
            return cachedMetrics;
        }
        if ("mock".equalsIgnoreCase(sourceControlProperties.effectiveProvider())) {
            return mockMetrics();
        }
        return List.of();
    }

    @Override
    public synchronized SourceControlMetricsCacheState cacheState() {
        if (!"github".equalsIgnoreCase(sourceControlProperties.effectiveProvider())) {
            return RepositoryOutcomeProvider.super.cacheState();
        }
        return new SourceControlMetricsCacheState(
                true,
                cacheLoadedAt != null,
                cacheLoadedAt,
                cacheLoadedAt == null ? null : cacheLoadedAt.plus(sourceControlProperties.effectiveCacheTtl()),
                sourceControlProperties.effectiveCacheTtl().toSeconds()
        );
    }

    private boolean cacheFresh() {
        return cacheLoadedAt != null && now().isBefore(cacheLoadedAt.plus(sourceControlProperties.effectiveCacheTtl()));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private List<RepositoryOutcomeMetrics> mockMetrics() {
        return List.of(
                new RepositoryOutcomeMetrics("ai-cost-intelligence-platform", "platform-engineering", "platform", 18, 126, 44, 137, 14.5, 5.2),
                new RepositoryOutcomeMetrics("checkout-service", "payments", "payments", 24, 188, 61, 203, 10.2, 3.8),
                new RepositoryOutcomeMetrics("support-automation", "customer-experience", "customer-experience", 15, 92, 38, 118, 18.0, 6.4),
                new RepositoryOutcomeMetrics("developer-portal", "platform-engineering", "platform", 21, 151, 53, 176, 12.8, 4.7)
        );
    }
}
