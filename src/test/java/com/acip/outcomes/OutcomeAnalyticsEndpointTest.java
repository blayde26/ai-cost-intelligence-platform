package com.acip.outcomes;

import com.acip.usage.AttributionStatus;
import com.acip.usage.UsageEvent;
import com.acip.usage.UsageEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OutcomeAnalyticsEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsageEventRepository usageEventRepository;

    @BeforeEach
    void clearEvents() {
        jdbcTemplate.update("DELETE FROM ai_usage_events");
        jdbcTemplate.update("DELETE FROM stories");
        jdbcTemplate.update("DELETE FROM epics");
    }

    @Test
    void correlationEndpointReturnsStableContract() throws Exception {
        usageEventRepository.save(new UsageEvent(
                UUID.randomUUID(),
                "OLLAMA",
                "llama3.2",
                "PAY-1001",
                "PAY-1000",
                "payments",
                "brian",
                10,
                5,
                15,
                new BigDecimal("4.25"),
                20,
                OffsetDateTime.of(2026, 5, 31, 12, 0, 0, 0, ZoneOffset.UTC),
                "test",
                "CAPITALIZED",
                "SUCCEEDED",
                AttributionStatus.VALID,
                "d".repeat(64),
                "checkout-service",
                "feature/PAY-1001-tax",
                "abc123",
                null,
                null,
                false,
                null,
                null
        ));

        mockMvc.perform(get("/api/v1/analytics/correlations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAiSpend").value(4.25))
                .andExpect(jsonPath("$.aiActiveRepositoryCount").value(1))
                .andExpect(jsonPath("$.repositoriesWithOutcomeMetrics").value(4))
                .andExpect(jsonPath("$.averageMergeTimeHoursForAiActiveRepositories").value(10.2))
                .andExpect(jsonPath("$.signals[?(@.subjectKey == 'checkout-service')].subjectType").value("REPOSITORY"))
                .andExpect(jsonPath("$.interpretation").value("Correlation signals compare AI spend with delivery and source-control outcomes. They are directional diagnostics, not causal claims."));
    }

    @Test
    void combinedOutcomeEndpointIncludesCorrelations() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/outcomes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teams").isArray())
                .andExpect(jsonPath("$.repositories").isArray())
                .andExpect(jsonPath("$.correlations.signals").isArray());
    }

    @Test
    void modelUtilizationEndpointsReturnStableContracts() throws Exception {
        usageEventRepository.save(new UsageEvent(
                UUID.randomUUID(),
                "OPENAI",
                "gpt-4o-mini",
                "PAY-1001",
                "PAY-1000",
                "payments",
                "brian",
                10,
                5,
                15,
                new BigDecimal("1.50"),
                20,
                OffsetDateTime.of(2026, 5, 31, 12, 0, 0, 0, ZoneOffset.UTC),
                "test",
                "CAPITALIZED",
                "SUCCEEDED",
                AttributionStatus.VALID,
                "e".repeat(64)
        ));

        mockMvc.perform(get("/api/v1/analytics/model-utilization/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("OPENAI"))
                .andExpect(jsonPath("$[0].totalCost").value(1.50))
                .andExpect(jsonPath("$[0].totalTokens").value(15))
                .andExpect(jsonPath("$[0].requestCount").value(1))
                .andExpect(jsonPath("$[0].modelCount").value(1))
                .andExpect(jsonPath("$[0].costPercent").value(100.0));

        mockMvc.perform(get("/api/v1/analytics/model-utilization/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("OPENAI"))
                .andExpect(jsonPath("$[0].model").value("gpt-4o-mini"))
                .andExpect(jsonPath("$[0].teamCount").value(1))
                .andExpect(jsonPath("$[0].workTypeCount").value(1));
    }
}
