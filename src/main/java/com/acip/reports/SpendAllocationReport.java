package com.acip.reports;

import java.math.BigDecimal;
import java.util.List;

public record SpendAllocationReport(
        BigDecimal totalCost,
        List<SpendAllocationBucket> buckets
) {
}
