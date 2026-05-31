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
        AttributionStatus attributionStatus,
        String requestHash,
        String repository,
        String branch,
        String commitHash,
        String initiativeKey,
        String initiativeName,
        boolean attributionCorrected,
        OffsetDateTime correctedTimestamp,
        String correctedBy
) {
    public UsageEvent(
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
            AttributionStatus attributionStatus,
            String requestHash
    ) {
        this(
                id,
                provider,
                model,
                storyKey,
                epicKey,
                teamKey,
                userKey,
                promptTokens,
                completionTokens,
                totalTokens,
                estimatedCostUsd,
                latencyMs,
                requestTimestamp,
                environment,
                workType,
                requestStatus,
                attributionStatus,
                requestHash,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null
        );
    }
}
