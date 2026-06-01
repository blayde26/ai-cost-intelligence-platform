package com.acip.setup;

import com.acip.jira.JiraProperties;
import com.acip.proxy.OpenAiProperties;
import com.acip.sourcecontrol.RepositoryOutcomeProvider;
import com.acip.sourcecontrol.SourceControlProperties;
import com.acip.worktracking.WorkTrackingProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SetupHealthService {

    private final JdbcTemplate jdbcTemplate;
    private final WorkTrackingProperties workTrackingProperties;
    private final JiraProperties jiraProperties;
    private final OpenAiProperties openAiProperties;
    private final SourceControlProperties sourceControlProperties;
    private final RepositoryOutcomeProvider repositoryOutcomeProvider;

    public SetupHealthService(
            JdbcTemplate jdbcTemplate,
            WorkTrackingProperties workTrackingProperties,
            JiraProperties jiraProperties,
            OpenAiProperties openAiProperties,
            SourceControlProperties sourceControlProperties,
            RepositoryOutcomeProvider repositoryOutcomeProvider
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.workTrackingProperties = workTrackingProperties;
        this.jiraProperties = jiraProperties;
        this.openAiProperties = openAiProperties;
        this.sourceControlProperties = sourceControlProperties;
        this.repositoryOutcomeProvider = repositoryOutcomeProvider;
    }

    public SetupHealthReport health() {
        List<SetupHealthComponent> components = List.of(
                database(),
                workTracking(),
                jira(),
                llmProxy(),
                pricing(),
                demoData(),
                csvImport(),
                sourceControl(),
                outcomeAnalytics()
        );
        return new SetupHealthReport(overallStatus(components), components);
    }

    private SetupHealthComponent database() {
        try {
            Long usageEvents = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_usage_events", Long.class);
            return new SetupHealthComponent("database", "Database", SetupHealthStatus.READY, "PostgreSQL connection is available with " + safeCount(usageEvents) + " usage events.");
        } catch (RuntimeException exception) {
            return new SetupHealthComponent("database", "Database", SetupHealthStatus.ERROR, "Database check failed: " + exception.getMessage());
        }
    }

    private SetupHealthComponent workTracking() {
        String provider = workTrackingProperties.provider() == null ? "mock" : workTrackingProperties.provider();
        if ("mock".equalsIgnoreCase(provider)) {
            return new SetupHealthComponent("workTracking", "Work Tracking", SetupHealthStatus.READY, "Mock work tracking provider is active.");
        }
        if ("jira".equalsIgnoreCase(provider)) {
            SetupHealthStatus status = jiraProperties.isConfigured() ? SetupHealthStatus.READY : SetupHealthStatus.WARNING;
            String message = jiraProperties.isConfigured()
                    ? "Jira work tracking provider is active and credentials are configured."
                    : "Jira provider is active but Jira credentials are incomplete.";
            return new SetupHealthComponent("workTracking", "Work Tracking", status, message);
        }
        return new SetupHealthComponent("workTracking", "Work Tracking", SetupHealthStatus.WARNING, "Unknown work tracking provider: " + provider + ".");
    }

    private SetupHealthComponent jira() {
        if (jiraProperties.isConfigured()) {
            return new SetupHealthComponent("jira", "Jira Configuration", SetupHealthStatus.READY, "Jira base URL, email, and API token are configured.");
        }
        return new SetupHealthComponent("jira", "Jira Configuration", SetupHealthStatus.NOT_CONFIGURED, "Jira is not configured. Mock work tracking can still be used for demos and local testing.");
    }

    private SetupHealthComponent llmProxy() {
        boolean apiKeyReady = !openAiProperties.requireApiKey() || !isBlank(openAiProperties.apiKey());
        if (apiKeyReady) {
            return new SetupHealthComponent("llmProxy", "LLM Proxy", SetupHealthStatus.READY, "Provider " + openAiProperties.provider() + " is configured at " + openAiProperties.chatCompletionsUrl() + ".");
        }
        return new SetupHealthComponent("llmProxy", "LLM Proxy", SetupHealthStatus.WARNING, "Provider " + openAiProperties.provider() + " requires an API key but none is configured.");
    }

    private SetupHealthComponent pricing() {
        try {
            Long pricingRows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM provider_pricing", Long.class);
            SetupHealthStatus status = safeCount(pricingRows) > 0 ? SetupHealthStatus.READY : SetupHealthStatus.WARNING;
            return new SetupHealthComponent("pricing", "Pricing", status, safeCount(pricingRows) + " provider pricing rows are available.");
        } catch (RuntimeException exception) {
            return new SetupHealthComponent("pricing", "Pricing", SetupHealthStatus.ERROR, "Pricing check failed: " + exception.getMessage());
        }
    }

    private SetupHealthComponent demoData() {
        try {
            Long stories = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stories", Long.class);
            Long epics = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM epics", Long.class);
            Long events = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_usage_events WHERE capture_source = 'DEMO_DATA' OR environment = 'demo'", Long.class);
            SetupHealthStatus status = safeCount(stories) > 0 && safeCount(epics) > 0 ? SetupHealthStatus.READY : SetupHealthStatus.WARNING;
            return new SetupHealthComponent("demoData", "Demo Data", status, safeCount(stories) + " stories, " + safeCount(epics) + " epics, and " + safeCount(events) + " demo usage events are loaded.");
        } catch (RuntimeException exception) {
            return new SetupHealthComponent("demoData", "Demo Data", SetupHealthStatus.ERROR, "Demo data check failed: " + exception.getMessage());
        }
    }

    private SetupHealthComponent csvImport() {
        try {
            Long importedEvents = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_usage_events WHERE capture_source = 'CSV_IMPORT'", Long.class);
            return new SetupHealthComponent("csvImport", "CSV Import", SetupHealthStatus.READY, "CSV import endpoint is available with " + safeCount(importedEvents) + " imported usage events.");
        } catch (RuntimeException exception) {
            return new SetupHealthComponent("csvImport", "CSV Import", SetupHealthStatus.ERROR, "CSV import check failed: " + exception.getMessage());
        }
    }

    private SetupHealthComponent sourceControl() {
        String provider = sourceControlProperties.effectiveProvider();
        if ("mock".equalsIgnoreCase(provider)) {
            int repositoryCount = repositoryOutcomeProvider.repositoryMetrics().size();
            return new SetupHealthComponent("sourceControl", "Source Control", SetupHealthStatus.READY, "Mock source-control outcome provider is active with " + repositoryCount + " repository metric snapshots.");
        }
        if ("github".equalsIgnoreCase(provider)) {
            return new SetupHealthComponent("sourceControl", "Source Control", SetupHealthStatus.WARNING, "GitHub source-control provider is selected, but live GitHub outcome sync is not implemented yet.");
        }
        return new SetupHealthComponent("sourceControl", "Source Control", SetupHealthStatus.WARNING, "Unknown source-control provider: " + provider + ".");
    }

    private SetupHealthComponent outcomeAnalytics() {
        return new SetupHealthComponent("outcomeAnalytics", "Outcome Analytics", SetupHealthStatus.READY, "Team, repository, and source-control outcome snapshots are available for correlation analysis.");
    }

    private SetupHealthStatus overallStatus(List<SetupHealthComponent> components) {
        if (components.stream().anyMatch(component -> component.status() == SetupHealthStatus.ERROR)) {
            return SetupHealthStatus.ERROR;
        }
        if (components.stream().anyMatch(component -> component.status() == SetupHealthStatus.WARNING)) {
            return SetupHealthStatus.WARNING;
        }
        return SetupHealthStatus.READY;
    }

    private long safeCount(Long count) {
        return count == null ? 0 : count;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
