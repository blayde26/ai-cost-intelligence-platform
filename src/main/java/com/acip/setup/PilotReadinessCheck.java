package com.acip.setup;

public record PilotReadinessCheck(
        String key,
        String label,
        SetupHealthStatus status,
        String message
) {
}
