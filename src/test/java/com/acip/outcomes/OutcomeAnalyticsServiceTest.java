package com.acip.outcomes;

import com.acip.jira.EpicRepository;
import com.acip.jira.StoryRepository;
import com.acip.usage.AttributionStatus;
import com.acip.usage.UsageEvent;
import com.acip.usage.UsageEventRepository;
import com.acip.worktracking.WorkItem;
import com.acip.worktracking.WorkItemType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OutcomeAnalyticsServiceTest {

    @Autowired
    private EpicRepository epicRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private UsageEventRepository usageEventRepository;

    @Autowired
    private OutcomeAnalyticsService outcomeAnalyticsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearData() {
        deleteRows();
    }

    @AfterEach
    void clearDataAfterTest() {
        deleteRows();
    }

    private void deleteRows() {
        jdbcTemplate.update("DELETE FROM ai_usage_events");
        jdbcTemplate.update("DELETE FROM stories");
        jdbcTemplate.update("DELETE FROM epics");
    }

    @Test
    void buildsTeamOutcomeSnapshotsFromPersistedWorkAndUsage() {
        epicRepository.upsert(new WorkItem("PAY-1000", WorkItemType.EPIC, "Checkout", "In Progress", "payments", null, "UNKNOWN"));
        storyRepository.upsert(new WorkItem("PAY-1001", WorkItemType.STORY, "Tax", "Done", "payments", "PAY-1000", "CAPITALIZED"));
        storyRepository.upsert(new WorkItem("PAY-1002", WorkItemType.STORY, "Support", "Cancelled", "payments", "PAY-1000", "OPERATIONAL"));
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
                new BigDecimal("2.00"),
                20,
                OffsetDateTime.of(2026, 5, 31, 12, 0, 0, 0, ZoneOffset.UTC),
                "test",
                "CAPITALIZED",
                "SUCCEEDED",
                AttributionStatus.VALID,
                "a".repeat(64),
                "ai-cost-intelligence-platform",
                "feature/PAY-1001-tax",
                "abc123",
                null,
                null,
                false,
                null,
                null
        ));

        assertThat(outcomeAnalyticsService.teamSnapshots())
                .anySatisfy(snapshot -> {
                    assertThat(snapshot.teamKey()).isEqualTo("payments");
                    assertThat(snapshot.aiSpend()).isEqualByComparingTo("2.00");
                    assertThat(snapshot.storyCount()).isEqualTo(2);
                    assertThat(snapshot.completedStoryCount()).isEqualTo(1);
                    assertThat(snapshot.cancelledStoryCount()).isEqualTo(1);
                    assertThat(snapshot.storyCompletionRate()).isEqualTo(50.0);
                    assertThat(snapshot.cancelledStoryRate()).isEqualTo(50.0);
                    assertThat(snapshot.operationalWorkRate()).isEqualTo(50.0);
                    assertThat(snapshot.outcomeDataStatus()).isEqualTo(OutcomeDataStatus.PARTIAL);
                });
    }

    @Test
    void buildsRepositoryOutcomeSnapshotsFromUsageMetadata() {
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
                new BigDecimal("2.00"),
                20,
                OffsetDateTime.of(2026, 5, 31, 12, 0, 0, 0, ZoneOffset.UTC),
                "test",
                "CAPITALIZED",
                "SUCCEEDED",
                AttributionStatus.VALID,
                "b".repeat(64),
                "ai-cost-intelligence-platform",
                "feature/PAY-1001-tax",
                "abc123",
                null,
                null,
                false,
                null,
                null
        ));

        assertThat(outcomeAnalyticsService.repositorySnapshots())
                .singleElement()
                .satisfies(snapshot -> {
                    assertThat(snapshot.repository()).isEqualTo("ai-cost-intelligence-platform");
                    assertThat(snapshot.aiSpend()).isEqualByComparingTo("2.00");
                    assertThat(snapshot.aiRequestCount()).isEqualTo(1);
                    assertThat(snapshot.totalTokens()).isEqualTo(15);
                    assertThat(snapshot.attributionCoveragePercent()).isEqualTo(100.0);
                    assertThat(snapshot.prCount()).isNull();
                    assertThat(snapshot.outcomeDataStatus()).isEqualTo(OutcomeDataStatus.PARTIAL);
                });
    }
}
