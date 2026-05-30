package com.acip.usage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UsageEvent(
        UUID id,
        String provider,
        String model,
        String storyKey,
        String epicKey,
        String teamKey,
        String userKey,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        BigDecimal estimatedCostUsd,
        long latencyMs,
        OffsetDateTime requestTimestamp,
        String environment,
        String workType,
        String requestStatus,
        String requestHash
) {
}
