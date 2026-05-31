package com.acip.reports;

import java.math.BigDecimal;

public record SpendAllocationBucket(
        String category,
        BigDecimal totalCost,
        double percent
) {
}
