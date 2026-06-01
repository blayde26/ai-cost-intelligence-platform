package com.acip.sourcecontrol;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SourceControlPropertiesTest {

    @Test
    void parsesConfiguredRepositoriesWithOptionalTeamKeys() {
        SourceControlProperties properties = properties("github", "token", "acme/api:platform, acme/web:payments, bad-entry");

        assertThat(properties.isGitHubConfigured()).isTrue();
        assertThat(properties.configuredRepositories())
                .extracting(SourceControlProperties.ConfiguredRepository::owner)
                .containsExactly("acme", "acme");
        assertThat(properties.configuredRepositories())
                .extracting(SourceControlProperties.ConfiguredRepository::name)
                .containsExactly("api", "web");
        assertThat(properties.configuredRepositories())
                .extracting(SourceControlProperties.ConfiguredRepository::teamKey)
                .containsExactly("platform", "payments");
    }

    @Test
    void requiresTokenAndRepositoriesForGitHubProvider() {
        assertThat(properties("github", "", "acme/api").isGitHubConfigured()).isFalse();
        assertThat(properties("github", "token", "").isGitHubConfigured()).isFalse();
        assertThat(properties("mock", "", "").isGitHubConfigured()).isFalse();
    }

    private SourceControlProperties properties(String provider, String token, String repositories) {
        return new SourceControlProperties(provider, "", "", token, repositories, Duration.ofSeconds(5), Duration.ofSeconds(20), Duration.ofMinutes(5));
    }
}
