package com.acip.reports;

import com.acip.jira.EpicRepository;
import com.acip.jira.StoryRepository;
import com.acip.usage.AttributionStatus;
import com.acip.usage.UsageEvent;
import com.acip.usage.UsageEventRepository;
import com.acip.worktracking.WorkItem;
import com.acip.worktracking.WorkItemType;
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
class SpendReportRepositoryTest {

    @Autowired
    private EpicRepository epicRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private UsageEventRepository usageEventRepository;

    @Autowired
    private SpendReportRepository spendReportRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearEvents() {
        jdbcTemplate.update("DELETE FROM ai_usage_events");
    }

    @Test
    void aggregatesSpendByStoryAndEpic() {
        epicRepository.upsert(new WorkItem("ACIP-1", WorkItemType.EPIC, "Cost visibility", "In Progress", "PLATFORM", null, "UNKNOWN"));
        storyRepository.upsert(new WorkItem("ACIP-123", WorkItemType.STORY, "Proxy capture", "Done", "PLATFORM", "ACIP-1", "CAPITALIZED"));
        usageEventRepository.save(new UsageEvent(
                UUID.randomUUID(),
                "MOCK_LLM",
                "mock-gpt-4o-mini",
                "ACIP-123",
                "ACIP-1",
                "PLATFORM",
                "brian",
                3,
                6,
                9,
                new BigDecimal("0.00000021"),
                20,
                OffsetDateTime.of(2026, 5, 30, 12, 0, 0, 0, ZoneOffset.UTC),
                "test",
                "CAPITALIZED",
                "SUCCEEDED",
                AttributionStatus.VALID,
                "b".repeat(64)
        ));

        assertThat(spendReportRepository.spendByStory())
                .anySatisfy(row -> {
                    assertThat(row.storyKey()).isEqualTo("ACIP-123");
                    assertThat(row.storySummary()).isEqualTo("Proxy capture");
                    assertThat(row.epicKey()).isEqualTo("ACIP-1");
                    assertThat(row.requestCount()).isEqualTo(1);
                    assertThat(row.totalTokens()).isEqualTo(9);
                    assertThat(row.estimatedCostUsd()).isEqualByComparingTo("0.00000021");
                });

        assertThat(spendReportRepository.spendByEpic())
                .anySatisfy(row -> {
                    assertThat(row.epicKey()).isEqualTo("ACIP-1");
                    assertThat(row.epicSummary()).isEqualTo("Cost visibility");
                    assertThat(row.requestCount()).isEqualTo(1);
                    assertThat(row.totalTokens()).isEqualTo(9);
                    assertThat(row.estimatedCostUsd()).isEqualByComparingTo("0.00000021");
                });

        assertThat(spendReportRepository.overview())
                .satisfies(row -> {
                    assertThat(row.totalRequests()).isGreaterThanOrEqualTo(1);
                    assertThat(row.totalTokens()).isGreaterThanOrEqualTo(9);
                    assertThat(row.totalSpend()).isGreaterThanOrEqualTo(new BigDecimal("0.00000021"));
                });

        assertThat(spendReportRepository.spendByTeam())
                .anySatisfy(row -> {
                    assertThat(row.teamKey()).isEqualTo("PLATFORM");
                    assertThat(row.requestCount()).isEqualTo(1);
                    assertThat(row.totalTokens()).isEqualTo(9);
                    assertThat(row.estimatedCostUsd()).isEqualByComparingTo("0.00000021");
                });
    }

    @Test
    void appliesDateRangeToSpendReports() {
        usageEventRepository.save(new UsageEvent(
                UUID.randomUUID(),
                "MOCK_LLM",
                "mock-gpt-4o-mini",
                "ACIP-OLD",
                "ACIP-1",
                "PLATFORM",
                "brian",
                50,
                50,
                100,
                new BigDecimal("1.00"),
                20,
                OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                "test",
                "CAPITALIZED",
                "SUCCEEDED",
                AttributionStatus.VALID,
                "c".repeat(64)
        ));
        usageEventRepository.save(new UsageEvent(
                UUID.randomUUID(),
                "MOCK_LLM",
                "mock-gpt-4o-mini",
                "ACIP-NEW",
                "ACIP-1",
                "PLATFORM",
                "brian",
                100,
                100,
                200,
                new BigDecimal("2.00"),
                20,
                OffsetDateTime.of(2026, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                "test",
                "CAPITALIZED",
                "SUCCEEDED",
                AttributionStatus.VALID,
                "d".repeat(64)
        ));

        ReportDateRange june = ReportDateRange.parse("2026-06-01", "2026-06-30");

        assertThat(spendReportRepository.overview(june))
                .satisfies(row -> {
                    assertThat(row.totalRequests()).isEqualTo(1);
                    assertThat(row.totalTokens()).isEqualTo(200);
                    assertThat(row.totalSpend()).isEqualByComparingTo("2.00");
                });
        assertThat(spendReportRepository.spendByStory(june))
                .extracting(SpendByStoryReport::storyKey)
                .containsExactly("ACIP-NEW");
    }

    @Test
    void calculatesAllocationAndPotentialWasteReports() {
        usageEventRepository.save(new UsageEvent(
                UUID.randomUUID(),
                "MOCK_LLM",
                "mock-gpt-4o-mini",
                "ACIP-VALID",
                "ACIP-1",
                "PLATFORM",
                "brian",
                50,
                50,
                100,
                new BigDecimal("10.00"),
                20,
                OffsetDateTime.of(2026, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                "test",
                "CAPITALIZED",
                "SUCCEEDED",
                AttributionStatus.VALID,
                "e".repeat(64)
        ));
        usageEventRepository.save(new UsageEvent(
                UUID.randomUUID(),
                "MOCK_LLM",
                "mock-gpt-4o-mini",
                null,
                null,
                "PLATFORM",
                "brian",
                50,
                50,
                100,
                new BigDecimal("5.00"),
                20,
                OffsetDateTime.of(2026, 6, 1, 13, 0, 0, 0, ZoneOffset.UTC),
                "test",
                "UNKNOWN",
                "SUCCEEDED",
                AttributionStatus.MISSING_STORY_KEY,
                "f".repeat(64)
        ));

        assertThat(spendReportRepository.potentialWaste(ReportDateRange.parse(null, null)).unknownAttributionSpend())
                .isEqualByComparingTo("5.00");
        assertThat(spendReportRepository.allocation(ReportDateRange.parse(null, null)).buckets())
                .anySatisfy(bucket -> {
                    assertThat(bucket.category()).isEqualTo("Revenue Features");
                    assertThat(bucket.totalCost()).isEqualByComparingTo("10.00");
                })
                .anySatisfy(bucket -> {
                    assertThat(bucket.category()).isEqualTo("Unattributed");
                    assertThat(bucket.totalCost()).isEqualByComparingTo("5.00");
                });
    }
}
