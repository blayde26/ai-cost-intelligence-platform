package com.acip.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PricingServiceTest {

    private final ProviderPricingRepository repository = mock(ProviderPricingRepository.class);
    private final PricingService pricingService = new PricingService(
            repository,
            Clock.fixed(Instant.parse("2026-05-29T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void estimatesCostFromPromptAndCompletionTokens() {
        when(repository.findEffectivePricing("OPENAI", "gpt-4o-mini", LocalDate.of(2026, 5, 29)))
                .thenReturn(Optional.of(new ProviderPricing(
                        "OPENAI",
                        "gpt-4o-mini",
                        new BigDecimal("0.150000"),
                        new BigDecimal("0.600000"),
                        LocalDate.of(2024, 7, 18)
                )));

        BigDecimal cost = pricingService.estimateCostUsd("OPENAI", "gpt-4o-mini", 1000, 500);

        assertThat(cost).isEqualByComparingTo("0.00045000");
    }

    @Test
    void returnsZeroWhenPricingIsUnknown() {
        when(repository.findEffectivePricing("OPENAI", "unknown-model", LocalDate.of(2026, 5, 29)))
                .thenReturn(Optional.empty());

        BigDecimal cost = pricingService.estimateCostUsd("OPENAI", "unknown-model", 1000, 500);

        assertThat(cost).isEqualByComparingTo("0.00000000");
    }
}
