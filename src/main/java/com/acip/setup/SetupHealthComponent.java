package com.acip.setup;

public record SetupHealthComponent(
        String key,
        String label,
        SetupHealthStatus status,
        String message
) {
}
