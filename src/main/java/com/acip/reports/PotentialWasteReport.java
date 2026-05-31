package com.acip.reports;

import java.math.BigDecimal;

public record PotentialWasteReport(
        BigDecimal cancelledStorySpend,
        BigDecimal operationalSpend,
        BigDecimal unknownAttributionSpend,
        BigDecimal failedRequestSpend
) {
}
