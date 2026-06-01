package com.acip.setup;

import java.util.List;

public record PilotReadinessReport(
        SetupHealthStatus status,
        int score,
        String summary,
        List<PilotReadinessCheck> checks,
        List<String> recommendedActions
) {
}
