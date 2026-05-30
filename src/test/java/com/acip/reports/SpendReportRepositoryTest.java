package com.acip.reports;

import com.acip.jira.JiraIssue;
import com.acip.jira.EpicRepository;
import com.acip.jira.StoryRepository;
import com.acip.usage.UsageEvent;
import com.acip.usage.UsageEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    @Test
    void aggregatesSpendByStoryAndEpic() {
        epicRepository.upsert(new JiraIssue("ACIP-1", "Epic", "Cost visibility", "In Progress", "PLATFORM", null, "UNKNOWN"));
        storyRepository.upsert(new JiraIssue("ACIP-123", "Story", "Proxy capture", "Done", "PLATFORM", "ACIP-1", "CAPITALIZED"));
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
    }
}
