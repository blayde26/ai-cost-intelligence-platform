package com.acip.outcomes;

import com.acip.usage.AttributionStatus;
import com.acip.usage.UsageEvent;
import com.acip.usage.UsageEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ModelUtilizationServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsageEventRepository usageEventRepository;

    @Autowired
    private ModelUtilizationService modelUtilizationService;

    @BeforeEach
    void clearEvents() {
        jdbcTemplate.update("DELETE FROM ai_usage_events");
    }

    @Test
    void returnsEmptySnapshotsWhenNoUsageExists() {
        assertThat(modelUtilizationService.providerSnapshots()).isEmpty();
        assertThat(modelUtilizationService.modelSnapshots()).isEmpty();
    }

    @Test
    void aggregatesProviderUtilizationByCostTokensRequestsAndModels() {
        saveEvent("OPENAI", "gpt-4o-mini", "payments", "CAPITALIZED", 1000, "3.00", "a");
        saveEvent("OPENAI", "gpt-4o", "platform", "OPERATIONAL", 500, "1.00", "b");
        saveEvent("OLLAMA", "llama3.2", "payments", "RESEARCH", 2000, "1.00", "c");

        List<ProviderUtilizationSnapshot> snapshots = modelUtilizationService.providerSnapshots();

        assertThat(snapshots).hasSize(2);
        assertThat(snapshots.get(0).provider()).isEqualTo("OPENAI");
        assertThat(snapshots.get(0).totalCost()).isEqualByComparingTo("4.00");
        assertThat(snapshots.get(0).totalTokens()).isEqualTo(1500);
        assertThat(snapshots.get(0).requestCount()).isEqualTo(2);
        assertThat(snapshots.get(0).modelCount()).isEqualTo(2);
        assertThat(snapshots.get(0).costPercent()).isEqualTo(80.0);
        assertThat(snapshots.get(1).provider()).isEqualTo("OLLAMA");
        assertThat(snapshots.get(1).costPercent()).isEqualTo(20.0);
    }

    @Test
    void aggregatesModelUtilizationByProviderAndModel() {
        saveEvent("OPENAI", "gpt-4o-mini", "payments", "CAPITALIZED", 1000, "3.00", "d");
        saveEvent("OPENAI", "gpt-4o-mini", "platform", "OPERATIONAL", 1500, "2.00", "e");
        saveEvent("OLLAMA", "llama3.2", "payments", "RESEARCH", 500, "0.00", "f");

        List<ModelUtilizationSnapshot> snapshots = modelUtilizationService.modelSnapshots();

        assertThat(snapshots)
                .anySatisfy(snapshot -> {
                    assertThat(snapshot.provider()).isEqualTo("OPENAI");
                    assertThat(snapshot.model()).isEqualTo("gpt-4o-mini");
                    assertThat(snapshot.totalCost()).isEqualByComparingTo("5.00");
                    assertThat(snapshot.totalTokens()).isEqualTo(2500);
                    assertThat(snapshot.requestCount()).isEqualTo(2);
                    assertThat(snapshot.teamCount()).isEqualTo(2);
                    assertThat(snapshot.workTypeCount()).isEqualTo(2);
                    assertThat(snapshot.costPercent()).isEqualTo(100.0);
                })
                .anySatisfy(snapshot -> {
                    assertThat(snapshot.provider()).isEqualTo("OLLAMA");
                    assertThat(snapshot.model()).isEqualTo("llama3.2");
                    assertThat(snapshot.costPercent()).isEqualTo(0.0);
                });
    }

    private void saveEvent(
            String provider,
            String model,
            String teamKey,
            String workType,
            int tokens,
            String cost,
            String hashPrefix
    ) {
        usageEventRepository.save(new UsageEvent(
                UUID.randomUUID(),
                provider,
                model,
                "PAY-1001",
                "PAY-1000",
                teamKey,
                "brian",
                tokens / 2,
                tokens / 2,
                tokens,
                new BigDecimal(cost),
                20,
                OffsetDateTime.of(2026, 5, 31, 12, 0, 0, 0, ZoneOffset.UTC),
                "test",
                workType,
                "SUCCEEDED",
                AttributionStatus.VALID,
                hashPrefix.repeat(64)
        ));
    }
}
