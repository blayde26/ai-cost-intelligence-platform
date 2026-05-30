package com.acip.usage;

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
class UsageEventRepositoryTest {

    @Autowired
    private UsageEventRepository repository;

    @Test
    void savesAndReturnsRecentUsageEvents() {
        UsageEvent event = new UsageEvent(
                UUID.randomUUID(),
                "OPENAI",
                "gpt-4o-mini",
                "ACIP-123",
                null,
                "PLATFORM",
                "brian",
                10,
                5,
                15,
                new BigDecimal("0.00000450"),
                120,
                OffsetDateTime.of(2026, 5, 29, 12, 0, 0, 0, ZoneOffset.UTC),
                "test",
                "UNKNOWN",
                "SUCCEEDED",
                "a".repeat(64)
        );

        repository.save(event);

        assertThat(repository.findRecent(10))
                .hasSize(1)
                .first()
                .satisfies(saved -> {
                    assertThat(saved.storyKey()).isEqualTo("ACIP-123");
                    assertThat(saved.estimatedCostUsd()).isEqualByComparingTo("0.00000450");
                    assertThat(saved.requestHash()).isEqualTo("a".repeat(64));
                });
    }
}
