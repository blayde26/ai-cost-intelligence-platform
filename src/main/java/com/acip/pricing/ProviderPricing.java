package com.acip.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProviderPricing(
        String provider,
        String model,
        BigDecimal inputCostPerMillion,
        BigDecimal outputCostPerMillion,
        LocalDate effectiveDate
) {
}
