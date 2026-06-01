package com.acip.outcomes;

import java.math.BigDecimal;

public record ModelUtilizationSnapshot(
        String provider,
        String model,
        BigDecimal totalCost,
        long totalTokens,
        long requestCount,
        long teamCount,
        long workTypeCount,
        double costPercent
) {
}
