package com.acip.usage;

public record AttributionInference(
        String storyKey,
        AttributionSource source,
        AttributionConfidence confidence,
        String reason
) {
}
