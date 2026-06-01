package com.acip.sourcecontrol;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceControlDiagnosticsServiceTest {

    @Test
    void reportsGitHubConfigurationAndMetricsWithoutExposingToken() {
        SourceControlProperties properties = new SourceControlProperties(
                "github",
                "",
                "https://api.github.com",
                "secret-token",
                "acme/api:platform,acme/web:payments",
                Duration.ofSeconds(5),
                Duration.ofSeconds(20),
                Duration.ofMinutes(5)
        );
        RepositoryOutcomeProvider provider = new StaticRepositoryOutcomeProvider(List.of(
                new RepositoryOutcomeMetrics("api", "acme", "platform", 4, 20, 8, 14, 6.5, 2.0)
        ));
        SourceControlDiagnosticsService service = new SourceControlDiagnosticsService(properties, provider);

        SourceControlDiagnosticsReport report = service.diagnostics();

        assertThat(report.provider()).isEqualTo("github");
        assertThat(report.configured()).isTrue();
        assertThat(report.tokenPresent()).isTrue();
        assertThat(report.configuredRepositoryCount()).isEqualTo(2);
        assertThat(report.metricsAvailableCount()).isEqualTo(1);
        assertThat(report.message()).doesNotContain("secret-token");
        assertThat(report.repositories()).anySatisfy(repository -> {
            assertThat(repository.repository()).isEqualTo("api");
            assertThat(repository.configured()).isTrue();
            assertThat(repository.metricsAvailable()).isTrue();
            assertThat(repository.prCount()).isEqualTo(4);
        });
        assertThat(report.repositories()).anySatisfy(repository -> {
            assertThat(repository.repository()).isEqualTo("web");
            assertThat(repository.configured()).isTrue();
            assertThat(repository.metricsAvailable()).isFalse();
        });
    }

    @Test
    void reportsMockProviderAsConfigured() {
        SourceControlProperties properties = new SourceControlProperties(
                "mock",
                "",
                "https://api.github.com",
                "",
                "",
                Duration.ofSeconds(5),
                Duration.ofSeconds(20),
                Duration.ofMinutes(5)
        );
        RepositoryOutcomeProvider provider = new StaticRepositoryOutcomeProvider(List.of(
                new RepositoryOutcomeMetrics("checkout-service", "payments", "payments", 24, 188, 61, 203, 10.2, 3.8)
        ));
        SourceControlDiagnosticsReport report = new SourceControlDiagnosticsService(properties, provider).diagnostics();

        assertThat(report.provider()).isEqualTo("mock");
        assertThat(report.configured()).isTrue();
        assertThat(report.tokenPresent()).isFalse();
        assertThat(report.metricsAvailableCount()).isEqualTo(1);
    }

    private record StaticRepositoryOutcomeProvider(List<RepositoryOutcomeMetrics> metrics) implements RepositoryOutcomeProvider {
        @Override
        public String providerKey() {
            return "STATIC";
        }

        @Override
        public List<RepositoryOutcomeMetrics> repositoryMetrics() {
            return metrics;
        }
    }
}
