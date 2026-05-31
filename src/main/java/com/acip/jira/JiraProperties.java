package com.acip.jira;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "acip.jira")
public record JiraProperties(
        String baseUrl,
        String email,
        String apiToken,
        String defaultJql,
        int pageSize,
        String epicLinkField,
        String workTypeField
) {

    public boolean isConfigured() {
        return !isBlank(baseUrl) && !isBlank(email) && !isBlank(apiToken);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
