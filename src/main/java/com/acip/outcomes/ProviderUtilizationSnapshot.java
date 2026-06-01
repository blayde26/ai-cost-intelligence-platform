package com.acip.outcomes;

import java.math.BigDecimal;

public record ProviderUtilizationSnapshot(
        String provider,
        BigDecimal totalCost,
        long totalTokens,
        long requestCount,
        long modelCount,
        double costPercent
) {
}
