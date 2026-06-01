package com.acip.sourcecontrol;

import java.time.OffsetDateTime;

public record SourceControlMetricsCacheState(
        boolean enabled,
        boolean populated,
        OffsetDateTime lastLoadedAt,
        OffsetDateTime expiresAt,
        long ttlSeconds
) {
}
