package com.acip.setup;

import com.acip.reports.AttributionCoverageReport;
import com.acip.reports.AttributionCoverageService;
import com.acip.sourcecontrol.SourceControlDiagnosticsReport;
import com.acip.sourcecontrol.SourceControlDiagnosticsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PilotReadinessService {

    private final SetupHealthService setupHealthService;
    private final AttributionCoverageService attributionCoverageService;
    private final SourceControlDiagnosticsService sourceControlDiagnosticsService;

    public PilotReadinessService(
            SetupHealthService setupHealthService,
            AttributionCoverageService attributionCoverageService,
            SourceControlDiagnosticsService sourceControlDiagnosticsService
    ) {
        this.setupHealthService = setupHealthService;
        this.attributionCoverageService = attributionCoverageService;
        this.sourceControlDiagnosticsService = sourceControlDiagnosticsService;
    }

    public PilotReadinessReport readiness() {
        SetupHealthReport setupHealth = setupHealthService.health();
        AttributionCoverageReport coverage = attributionCoverageService.coverage();
        SourceControlDiagnosticsReport sourceControl = sourceControlDiagnosticsService.diagnostics();
        List<PilotReadinessCheck> checks = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        int score = 0;

        score += setupCheck(setupHealth, checks, actions);
        score += usageCheck(coverage, checks, actions);
        score += coverageCheck(coverage, checks, actions);
        score += sourceControlCheck(sourceControl, checks, actions);

        SetupHealthComponent jira = component(setupHealth, "jira");
        score += jiraCheck(jira, checks, actions);

        SetupHealthStatus status = statusFor(score, checks);
        return new PilotReadinessReport(
                status,
                Math.min(100, score),
                summaryFor(status, score),
                List.copyOf(checks),
                List.copyOf(actions)
        );
    }

    private int setupCheck(SetupHealthReport setupHealth, List<PilotReadinessCheck> checks, List<String> actions) {
        if (setupHealth.overallStatus() == SetupHealthStatus.READY) {
            checks.add(new PilotReadinessCheck("setup", "Core setup", SetupHealthStatus.READY, "Core setup checks are passing."));
            return 25;
        }
        if (setupHealth.overallStatus() == SetupHealthStatus.WARNING) {
            checks.add(new PilotReadinessCheck("setup", "Core setup", SetupHealthStatus.WARNING, "Some setup checks need attention before a pilot."));
            actions.add("Review warning rows in Integration Health and expand the affected integration details.");
            return 12;
        }
        checks.add(new PilotReadinessCheck("setup", "Core setup", SetupHealthStatus.ERROR, "One or more setup checks are failing."));
        actions.add("Resolve setup errors before using ACIP with a design partner.");
        return 0;
    }

    private int usageCheck(AttributionCoverageReport coverage, List<PilotReadinessCheck> checks, List<String> actions) {
        if (coverage.eventCount() >= 100) {
            checks.add(new PilotReadinessCheck("usageVolume", "Usage volume", SetupHealthStatus.READY, coverage.eventCount() + " usage events are available for analysis."));
            return 20;
        }
        if (coverage.eventCount() > 0) {
            checks.add(new PilotReadinessCheck("usageVolume", "Usage volume", SetupHealthStatus.WARNING, coverage.eventCount() + " usage events are available. More real activity will improve pilot signal."));
            actions.add("Generate or import more representative AI usage before drawing pilot conclusions.");
            return 10;
        }
        checks.add(new PilotReadinessCheck("usageVolume", "Usage volume", SetupHealthStatus.NOT_CONFIGURED, "No usage events are available yet."));
        actions.add("Send a proxy request or import CSV usage so ACIP has pilot data to analyze.");
        return 0;
    }

    private int coverageCheck(AttributionCoverageReport coverage, List<PilotReadinessCheck> checks, List<String> actions) {
        if (coverage.eventCount() == 0) {
            checks.add(new PilotReadinessCheck("coverage", "Attribution coverage", SetupHealthStatus.NOT_CONFIGURED, "Coverage cannot be evaluated until usage exists."));
            return 0;
        }
        if (coverage.coveragePercent() >= 90.0) {
            checks.add(new PilotReadinessCheck("coverage", "Attribution coverage", SetupHealthStatus.READY, "Attribution coverage is " + coverage.coveragePercent() + "%."));
            return 25;
        }
        if (coverage.coveragePercent() >= 70.0) {
            checks.add(new PilotReadinessCheck("coverage", "Attribution coverage", SetupHealthStatus.WARNING, "Attribution coverage is " + coverage.coveragePercent() + "%. Manual cleanup may be needed."));
            actions.add("Review Attribution Health and correct high-cost unattributed events before pilot review.");
            return 12;
        }
        checks.add(new PilotReadinessCheck("coverage", "Attribution coverage", SetupHealthStatus.ERROR, "Attribution coverage is only " + coverage.coveragePercent() + "%."));
        actions.add("Improve story tagging, branch inference, or manual corrections before using reports for decisions.");
        return 0;
    }

    private int sourceControlCheck(SourceControlDiagnosticsReport sourceControl, List<PilotReadinessCheck> checks, List<String> actions) {
        if (sourceControl.configured() && sourceControl.metricsAvailableCount() > 0) {
            checks.add(new PilotReadinessCheck("sourceControl", "Source-control signal", SetupHealthStatus.READY, sourceControl.metricsAvailableCount() + " repository metric snapshots are available."));
            return 15;
        }
        if (sourceControl.configured()) {
            checks.add(new PilotReadinessCheck("sourceControl", "Source-control signal", SetupHealthStatus.WARNING, "Source control is configured but no repository metrics are available yet."));
            actions.add("Check source-control diagnostics and repository configuration.");
            return 8;
        }
        checks.add(new PilotReadinessCheck("sourceControl", "Source-control signal", SetupHealthStatus.WARNING, "Source control is not configured for live outcome metrics."));
        actions.add("Configure GitHub repositories or use mock source-control data for demos.");
        return 0;
    }

    private int jiraCheck(SetupHealthComponent jira, List<PilotReadinessCheck> checks, List<String> actions) {
        if (jira != null && jira.status() == SetupHealthStatus.READY) {
            checks.add(new PilotReadinessCheck("jira", "Work tracking confidence", SetupHealthStatus.READY, "Jira configuration is present. Use the Jira connection test before a live pilot."));
            return 15;
        }
        checks.add(new PilotReadinessCheck("jira", "Work tracking confidence", SetupHealthStatus.WARNING, "Jira is not configured. Mock work tracking is suitable for demos but not a live Jira pilot."));
        actions.add("Configure Jira and run the Jira connection test before testing with real team data.");
        return 5;
    }

    private SetupHealthComponent component(SetupHealthReport report, String key) {
        return report.components().stream()
                .filter(component -> component.key().equals(key))
                .findFirst()
                .orElse(null);
    }

    private SetupHealthStatus statusFor(int score, List<PilotReadinessCheck> checks) {
        if (checks.stream().anyMatch(check -> check.status() == SetupHealthStatus.ERROR)) {
            return SetupHealthStatus.ERROR;
        }
        if (score >= 80) {
            return SetupHealthStatus.READY;
        }
        if (score >= 50) {
            return SetupHealthStatus.WARNING;
        }
        return SetupHealthStatus.NOT_CONFIGURED;
    }

    private String summaryFor(SetupHealthStatus status, int score) {
        if (status == SetupHealthStatus.READY) {
            return "ACIP is ready for a guided technical pilot.";
        }
        if (status == SetupHealthStatus.WARNING) {
            return "ACIP is close to pilot-ready, but a few setup or data quality items need attention.";
        }
        if (status == SetupHealthStatus.ERROR) {
            return "ACIP has a blocking setup or attribution quality issue to fix before pilot use.";
        }
        return "ACIP needs setup and usage data before pilot readiness can be evaluated. Current score: " + score + ".";
    }
}
