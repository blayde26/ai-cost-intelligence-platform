package com.acip.reports;

import com.acip.usage.AttributionStatus;

import java.time.OffsetDateTime;

public record UnattributedSpendFilter(
        OffsetDateTime fromDate,
        OffsetDateTime toDate,
        String teamKey,
        String userKey,
        AttributionStatus attributionStatus,
        String sort
) {
}
