package com.acip.capture;

public record UsageCaptureMetadata(
        UsageCaptureSource source,
        String provider,
        UsageCaptureMethod method,
        UsageCaptureConfidence confidence
) {
}
