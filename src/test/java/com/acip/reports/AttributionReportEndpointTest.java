package com.acip.reports;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttributionReportEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsageEventRepository usageEventRepository;

    @Autowired
    private AttributionReportRepository attributionReportRepository;

    @BeforeEach
    void clearEvents() {
        jdbcTemplate.update("DELETE FROM ai_usage_events");
    }

    @Test
    void coverageEndpointCalculatesPersistedEventCoverage() throws Exception {
        for (int i = 0; i < 5; i++) {
            saveEvent("PAY-1001", "payments", "brian", "10.00", 100, AttributionStatus.VALID, minutes(i));
        }
        for (int i = 0; i < 3; i++) {
            saveEvent("BAD-" + i, "payments", "brian", "5.00", 50, AttributionStatus.UNKNOWN_STORY, minutes(10 + i));
        }

        mockMvc.perform(get("/api/v1/reports/attribution-coverage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(65.00))
                .andExpect(jsonPath("$.attributedCost").value(50.00))
                .andExpect(jsonPath("$.unattributedCost").value(15.00))
                .andExpect(jsonPath("$.coveragePercent").value(76.92))
                .andExpect(jsonPath("$.eventCount").value(8))
                .andExpect(jsonPath("$.validEventCount").value(5))
                .andExpect(jsonPath("$.invalidEventCount").value(3));
    }

    @Test
    void coverageEndpointAppliesDateRangeAndRejectsInvalidRanges() throws Exception {
        saveEvent("PAY-1001", "payments", "brian", "10.00", 100, AttributionStatus.VALID, minutes(1));
        saveEvent("BAD-1", "payments", "brian", "5.00", 50, AttributionStatus.UNKNOWN_STORY, minutes(120));

        mockMvc.perform(get("/api/v1/reports/attribution-coverage")
                        .param("startDate", "2026-05-30")
                        .param("endDate", "2026-05-30T13:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(10.00))
                .andExpect(jsonPath("$.coveragePercent").value(100.0));

        mockMvc.perform(get("/api/v1/reports/attribution-coverage")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-05-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allocationAndPotentialWasteEndpointsExposeDashboardDtos() throws Exception {
        saveEvent("PAY-1001", "payments", "brian", "10.00", 100, AttributionStatus.VALID, minutes(1));
        saveEvent(null, "payments", "brian", "5.00", 50, AttributionStatus.MISSING_STORY_KEY, minutes(2));

        mockMvc.perform(get("/api/v1/reports/allocation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(15.00))
                .andExpect(jsonPath("$.buckets[?(@.category == 'Revenue Features')].totalCost").value(10.00))
                .andExpect(jsonPath("$.buckets[?(@.category == 'Unattributed')].totalCost").value(5.00));

        mockMvc.perform(get("/api/v1/reports/potential-waste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unknownAttributionSpend").value(5.00));
    }

    @Test
    void spendByTeamEndpointReturnsStableDtoFields() throws Exception {
        saveEvent("PAY-1001", "payments", "brian", "10.00", 100, AttributionStatus.VALID, minutes(1));

        mockMvc.perform(get("/api/v1/reports/spend/by-team")
                        .param("startDate", "2026-05-30")
                        .param("endDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teamKey").value("payments"))
                .andExpect(jsonPath("$[0].totalCost").value(10.00))
                .andExpect(jsonPath("$[0].totalTokens").value(100))
                .andExpect(jsonPath("$[0].requestCount").value(1))
                .andExpect(jsonPath("$[0].epicCount").value(1))
                .andExpect(jsonPath("$[0].storyCount").value(1));
    }


    @Test
    void unattributedEndpointReturnsOnlyInvalidEventsAndSorts() throws Exception {
        saveEvent("PAY-1001", "payments", "brian", "99.00", 9999, AttributionStatus.VALID, minutes(1));
        saveEvent("BAD-1", "payments", "brian", "1.00", 900, AttributionStatus.UNKNOWN_STORY, minutes(2));
        saveEvent(null, "platform", "alex", "3.00", 100, AttributionStatus.MISSING_STORY_KEY, minutes(3));
        saveEvent("BAD-2", "platform", "alex", "2.00", 500, AttributionStatus.UNKNOWN_STORY, minutes(4));

        mockMvc.perform(get("/api/v1/reports/unattributed").param("sort", "cost_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].cost").value(3.00))
                .andExpect(jsonPath("$[0].attributionStatus").value("MISSING_STORY_KEY"));

        mockMvc.perform(get("/api/v1/reports/unattributed").param("sort", "tokens_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tokens").value(900));

        mockMvc.perform(get("/api/v1/reports/unattributed").param("sort", "recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storyKey").value("BAD-2"));
    }

    @Test
    void unattributedRepositoryAppliesFiltersAndSummarizesCosts() {
        saveEvent("BAD-1", "payments", "brian", "4.00", 400, AttributionStatus.UNKNOWN_STORY, minutes(1));
        saveEvent(null, "payments", "alex", "3.00", 300, AttributionStatus.MISSING_STORY_KEY, minutes(2));
        saveEvent("BAD-2", "platform", "brian", "2.00", 200, AttributionStatus.UNKNOWN_STORY, minutes(3));
        saveEvent("PAY-1001", "payments", "brian", "10.00", 1000, AttributionStatus.VALID, minutes(4));

        UnattributedSpendFilter filter = new UnattributedSpendFilter(
                minutes(0),
                minutes(5),
                "payments",
                null,
                null,
                "cost_desc"
        );

        assertThat(attributionReportRepository.unattributedEvents(filter))
                .extracting(UnattributedSpendEvent::storyKey)
                .containsExactly("BAD-1", null);

        UnattributedSpendSummary summary = attributionReportRepository.unattributedSummary(filter);

        assertThat(summary.totalCost()).isEqualByComparingTo("7.00");
        assertThat(summary.totalTokens()).isEqualTo(700);
        assertThat(summary.eventCount()).isEqualTo(2);
        assertThat(summary.breakdown().get(AttributionStatus.UNKNOWN_STORY)).isEqualByComparingTo("4.00");
        assertThat(summary.breakdown().get(AttributionStatus.MISSING_STORY_KEY)).isEqualByComparingTo("3.00");
    }

    private void saveEvent(
            String storyKey,
            String teamKey,
            String userKey,
            String cost,
            int totalTokens,
            AttributionStatus status,
            OffsetDateTime timestamp
    ) {
        usageEventRepository.save(new UsageEvent(
                UUID.randomUUID(),
                "MOCK_LLM",
                "mock-gpt-4o-mini",
                storyKey,
                "PAY-1000",
                teamKey,
                userKey,
                totalTokens / 2,
                totalTokens / 2,
                totalTokens,
                new BigDecimal(cost),
                20,
                timestamp,
                "test",
                "CAPITALIZED",
                "SUCCEEDED",
                status,
                UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "")
        ));
    }

    private OffsetDateTime minutes(int minutes) {
        return OffsetDateTime.of(2026, 5, 30, 12, 0, 0, 0, ZoneOffset.UTC).plusMinutes(minutes);
    }
}
