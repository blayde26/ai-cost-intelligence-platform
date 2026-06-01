package com.acip.sourcecontrol;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "acip.source-control")
public record SourceControlProperties(
        String provider,
        String organization,
        String apiBaseUrl,
        String token,
        String repositories,
        Duration connectTimeout,
        Duration readTimeout,
        Duration cacheTtl
) {
    public String effectiveProvider() {
        return isBlank(provider) ? "mock" : provider;
    }

    public String effectiveApiBaseUrl() {
        return isBlank(apiBaseUrl) ? "https://api.github.com" : apiBaseUrl;
    }

    public boolean isGitHubConfigured() {
        return "github".equalsIgnoreCase(effectiveProvider()) && !isBlank(token) && !configuredRepositories().isEmpty();
    }

    public List<ConfiguredRepository> configuredRepositories() {
        if (isBlank(repositories)) {
            return List.of();
        }
        return java.util.Arrays.stream(repositories.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::parseRepository)
                .filter(repository -> !repository.owner().isBlank() && !repository.name().isBlank())
                .toList();
    }

    public Duration effectiveConnectTimeout() {
        return connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
    }

    public Duration effectiveReadTimeout() {
        return readTimeout == null ? Duration.ofSeconds(20) : readTimeout;
    }

    public Duration effectiveCacheTtl() {
        return cacheTtl == null ? Duration.ofMinutes(5) : cacheTtl;
    }

    private ConfiguredRepository parseRepository(String value) {
        String[] repositoryAndTeam = value.split(":", 2);
        String[] ownerAndName = repositoryAndTeam[0].split("/", 2);
        if (ownerAndName.length != 2) {
            return new ConfiguredRepository("", "", "", value);
        }
        String teamKey = repositoryAndTeam.length == 2 ? repositoryAndTeam[1].trim() : "";
        return new ConfiguredRepository(ownerAndName[0].trim(), ownerAndName[1].trim(), teamKey, value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ConfiguredRepository(
            String owner,
            String name,
            String teamKey,
            String rawValue
    ) {
    }
}
