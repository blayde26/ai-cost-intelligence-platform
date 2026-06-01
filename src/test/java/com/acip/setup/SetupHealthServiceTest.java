package com.acip.setup;

import com.acip.jira.JiraProperties;
import com.acip.proxy.OpenAiProperties;
import com.acip.sourcecontrol.ConfiguredRepositoryOutcomeProvider;
import com.acip.sourcecontrol.SourceControlProperties;
import com.acip.worktracking.WorkTrackingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SetupHealthServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @Test
    void reportsReadyLocalSetupWithMockWorkTracking() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_usage_events", Long.class)).thenReturn(10L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM provider_pricing", Long.class)).thenReturn(3L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stories", Long.class)).thenReturn(20L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM epics", Long.class)).thenReturn(5L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_usage_events WHERE capture_source = 'DEMO_DATA' OR environment = 'demo'", Long.class)).thenReturn(2000L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_usage_events WHERE capture_source = 'CSV_IMPORT'", Long.class)).thenReturn(2L);
        SetupHealthService service = new SetupHealthService(
                jdbcTemplate,
                new WorkTrackingProperties("mock"),
                new JiraProperties("", "", "", "project is not EMPTY", 50, "customfield_10014", "customfield_10015"),
                new OpenAiProperties("MOCK_LLM", "", false, "http://localhost:8090/v1/chat/completions", Duration.ofSeconds(5), Duration.ofSeconds(60)),
                new SourceControlProperties("mock", ""),
                new ConfiguredRepositoryOutcomeProvider(new SourceControlProperties("mock", ""))
        );

        SetupHealthReport report = service.health();

        assertThat(report.overallStatus()).isEqualTo(SetupHealthStatus.READY);
        assertThat(report.components()).anySatisfy(component -> {
            assertThat(component.key()).isEqualTo("csvImport");
            assertThat(component.message()).contains("2 imported");
        });
        assertThat(report.components()).anySatisfy(component -> {
            assertThat(component.key()).isEqualTo("sourceControl");
            assertThat(component.status()).isEqualTo(SetupHealthStatus.READY);
        });
    }

    @Test
    void warnsWhenConfiguredProviderRequiresMissingApiKey() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_usage_events", Long.class)).thenReturn(0L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM provider_pricing", Long.class)).thenReturn(3L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stories", Long.class)).thenReturn(20L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM epics", Long.class)).thenReturn(5L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_usage_events WHERE capture_source = 'DEMO_DATA' OR environment = 'demo'", Long.class)).thenReturn(0L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_usage_events WHERE capture_source = 'CSV_IMPORT'", Long.class)).thenReturn(0L);
        SetupHealthService service = new SetupHealthService(
                jdbcTemplate,
                new WorkTrackingProperties("jira"),
                new JiraProperties("", "", "", "project is not EMPTY", 50, "customfield_10014", "customfield_10015"),
                new OpenAiProperties("OPENAI", "", true, "https://api.openai.com/v1/chat/completions", Duration.ofSeconds(5), Duration.ofSeconds(60)),
                new SourceControlProperties("mock", ""),
                new ConfiguredRepositoryOutcomeProvider(new SourceControlProperties("mock", ""))
        );

        SetupHealthReport report = service.health();

        assertThat(report.overallStatus()).isEqualTo(SetupHealthStatus.WARNING);
        assertThat(report.components()).anySatisfy(component -> {
            assertThat(component.key()).isEqualTo("llmProxy");
            assertThat(component.status()).isEqualTo(SetupHealthStatus.WARNING);
        });
    }
}
