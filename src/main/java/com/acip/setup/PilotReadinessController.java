package com.acip.setup;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PilotReadinessController {

    private final PilotReadinessService pilotReadinessService;

    public PilotReadinessController(PilotReadinessService pilotReadinessService) {
        this.pilotReadinessService = pilotReadinessService;
    }

    @GetMapping("/api/v1/setup/pilot-readiness")
    public PilotReadinessReport readiness() {
        return pilotReadinessService.readiness();
    }
}
