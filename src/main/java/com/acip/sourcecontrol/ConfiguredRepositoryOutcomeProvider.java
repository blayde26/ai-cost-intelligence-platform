package com.acip.sourcecontrol;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConfiguredRepositoryOutcomeProvider implements RepositoryOutcomeProvider {

    private final SourceControlProperties sourceControlProperties;

    public ConfiguredRepositoryOutcomeProvider(SourceControlProperties sourceControlProperties) {
        this.sourceControlProperties = sourceControlProperties;
    }

    @Override
    public String providerKey() {
        return sourceControlProperties.effectiveProvider().toUpperCase();
    }

    @Override
    public List<RepositoryOutcomeMetrics> repositoryMetrics() {
        if (!"mock".equalsIgnoreCase(sourceControlProperties.effectiveProvider())) {
            return List.of();
        }
        return List.of(
                new RepositoryOutcomeMetrics("ai-cost-intelligence-platform", "platform-engineering", "platform", 18, 126, 44, 137, 14.5, 5.2),
                new RepositoryOutcomeMetrics("checkout-service", "payments", "payments", 24, 188, 61, 203, 10.2, 3.8),
                new RepositoryOutcomeMetrics("support-automation", "customer-experience", "customer-experience", 15, 92, 38, 118, 18.0, 6.4),
                new RepositoryOutcomeMetrics("developer-portal", "platform-engineering", "platform", 21, 151, 53, 176, 12.8, 4.7)
        );
    }
}
