package com.acip.sourcecontrol;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "acip.source-control")
public record SourceControlProperties(
        String provider,
        String organization
) {
    public String effectiveProvider() {
        return isBlank(provider) ? "mock" : provider;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
