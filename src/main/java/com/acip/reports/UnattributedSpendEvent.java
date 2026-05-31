package com.acip.reports;

import com.acip.usage.AttributionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UnattributedSpendEvent(
        UUID eventId,
        String storyKey,
        String teamKey,
        String userKey,
        String provider,
        String model,
        BigDecimal cost,
        long tokens,
        AttributionStatus attributionStatus,
        OffsetDateTime timestamp
) {
}
