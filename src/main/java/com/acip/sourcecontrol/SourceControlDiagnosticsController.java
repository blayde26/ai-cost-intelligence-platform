package com.acip.sourcecontrol;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SourceControlDiagnosticsController {

    private final SourceControlDiagnosticsService sourceControlDiagnosticsService;

    public SourceControlDiagnosticsController(SourceControlDiagnosticsService sourceControlDiagnosticsService) {
        this.sourceControlDiagnosticsService = sourceControlDiagnosticsService;
    }

    @GetMapping("/api/v1/source-control/diagnostics")
    public SourceControlDiagnosticsReport diagnostics() {
        return sourceControlDiagnosticsService.diagnostics();
    }
}
