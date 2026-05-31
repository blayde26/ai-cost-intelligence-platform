package com.acip.setup;

import java.util.List;

public record SetupHealthReport(
        SetupHealthStatus overallStatus,
        List<SetupHealthComponent> components
) {
}
