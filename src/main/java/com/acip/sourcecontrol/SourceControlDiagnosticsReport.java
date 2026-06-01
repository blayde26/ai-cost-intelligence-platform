package com.acip.sourcecontrol;

import java.util.List;

public record SourceControlDiagnosticsReport(
        String provider,
        boolean configured,
        boolean tokenPresent,
        int configuredRepositoryCount,
        int metricsAvailableCount,
        SourceControlMetricsCacheState cache,
        List<SourceControlRepositoryDiagnostic> repositories,
        String message
) {
}
