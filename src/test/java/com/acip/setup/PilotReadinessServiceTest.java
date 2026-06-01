package com.acip.setup;

import com.acip.reports.AttributionCoverageReport;
import com.acip.reports.AttributionCoverageService;
import com.acip.sourcecontrol.SourceControlDiagnosticsReport;
import com.acip.sourcecontrol.SourceControlDiagnosticsService;
import com.acip.sourcecontrol.SourceControlMetricsCacheState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PilotReadinessServiceTest {

    private final SetupHealthService setupHealthService = mock(SetupHealthService.class);
    private final AttributionCoverageService attributionCoverageService = mock(AttributionCoverageService.class);
    private final SourceControlDiagnosticsService sourceControlDiagnosticsService = mock(SourceControlDiagnosticsService.class);
    private final PilotReadinessService service = new PilotReadinessService(setupHealthService, attributionCoverageService, sourceControlDiagnosticsService);

    @Test
    void reportsReadyForHealthyPilotSignals() {
        when(setupHealthService.health()).thenReturn(new SetupHealthReport(SetupHealthStatus.READY, List.of(
                new SetupHealthComponent("jira", "Jira Configuration", SetupHealthStatus.READY, "Configured")
        )));
        when(attributionCoverageService.coverage()).thenReturn(new AttributionCoverageReport(
                new BigDecimal("100.00"),
                new BigDecimal("95.00"),
                new BigDecimal("5.00"),
                95.0,
                250,
                240,
                10
        ));
        when(sourceControlDiagnosticsService.diagnostics()).thenReturn(sourceControl(true, 2));

        PilotReadinessReport report = service.readiness();

        assertThat(report.status()).isEqualTo(SetupHealthStatus.READY);
        assertThat(report.score()).isEqualTo(100);
        assertThat(report.recommendedActions()).isEmpty();
    }

    @Test
    void reportsNotConfiguredWhenUsageDataIsMissing() {
        when(setupHealthService.health()).thenReturn(new SetupHealthReport(SetupHealthStatus.READY, List.of(
                new SetupHealthComponent("jira", "Jira Configuration", SetupHealthStatus.NOT_CONFIGURED, "Missing")
        )));
        when(attributionCoverageService.coverage()).thenReturn(new AttributionCoverageReport(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0.0,
                0,
                0,
                0
        ));
        when(sourceControlDiagnosticsService.diagnostics()).thenReturn(sourceControl(false, 0));

        PilotReadinessReport report = service.readiness();

        assertThat(report.status()).isEqualTo(SetupHealthStatus.NOT_CONFIGURED);
        assertThat(report.checks()).anySatisfy(check -> {
            assertThat(check.key()).isEqualTo("usageVolume");
            assertThat(check.status()).isEqualTo(SetupHealthStatus.NOT_CONFIGURED);
        });
        assertThat(report.recommendedActions()).anyMatch(action -> action.contains("Send a proxy request"));
    }

    private SourceControlDiagnosticsReport sourceControl(boolean configured, int metricsAvailable) {
        return new SourceControlDiagnosticsReport(
                configured ? "github" : "mock",
                configured,
                configured,
                configured ? 1 : 0,
                metricsAvailable,
                new SourceControlMetricsCacheState(false, false, null, null, Duration.ofMinutes(5).toSeconds()),
                List.of(),
                "source-control diagnostics"
        );
    }
}
