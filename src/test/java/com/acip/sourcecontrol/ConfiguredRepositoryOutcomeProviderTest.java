package com.acip.sourcecontrol;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfiguredRepositoryOutcomeProviderTest {

    @Test
    void cachesGitHubMetricsWithinTtl() {
        GitHubRepositoryClient client = mock(GitHubRepositoryClient.class);
        SourceControlProperties properties = properties(Duration.ofMinutes(5));
        SourceControlProperties.ConfiguredRepository repository = properties.configuredRepositories().getFirst();
        when(client.metrics(repository)).thenReturn(Optional.of(metric(4)));
        ConfiguredRepositoryOutcomeProvider provider = new ConfiguredRepositoryOutcomeProvider(
                properties,
                client,
                Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC)
        );

        assertThat(provider.repositoryMetrics()).extracting(RepositoryOutcomeMetrics::prCount).containsExactly(4L);
        assertThat(provider.repositoryMetrics()).extracting(RepositoryOutcomeMetrics::prCount).containsExactly(4L);

        verify(client, times(1)).metrics(repository);
        assertThat(provider.cacheState().populated()).isTrue();
        assertThat(provider.cacheState().ttlSeconds()).isEqualTo(300);
    }

    @Test
    void refreshesGitHubMetricsWhenTtlIsExpired() {
        GitHubRepositoryClient client = mock(GitHubRepositoryClient.class);
        SourceControlProperties properties = properties(Duration.ZERO);
        SourceControlProperties.ConfiguredRepository repository = properties.configuredRepositories().getFirst();
        when(client.metrics(repository))
                .thenReturn(Optional.of(metric(4)))
                .thenReturn(Optional.of(metric(5)));
        ConfiguredRepositoryOutcomeProvider provider = new ConfiguredRepositoryOutcomeProvider(
                properties,
                client,
                Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC)
        );

        assertThat(provider.repositoryMetrics()).extracting(RepositoryOutcomeMetrics::prCount).containsExactly(4L);
        assertThat(provider.repositoryMetrics()).extracting(RepositoryOutcomeMetrics::prCount).containsExactly(5L);

        verify(client, times(2)).metrics(repository);
    }

    private SourceControlProperties properties(Duration cacheTtl) {
        return new SourceControlProperties(
                "github",
                "",
                "https://api.github.com",
                "token",
                "acme/api:platform",
                Duration.ofSeconds(5),
                Duration.ofSeconds(20),
                cacheTtl
        );
    }

    private RepositoryOutcomeMetrics metric(long prCount) {
        return new RepositoryOutcomeMetrics("api", "acme", "platform", prCount, 10, 2, 3, 4.0, 1.0);
    }
}
