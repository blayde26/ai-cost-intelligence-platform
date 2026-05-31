package com.acip.setup;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SetupHealthController {

    private final SetupHealthService setupHealthService;

    public SetupHealthController(SetupHealthService setupHealthService) {
        this.setupHealthService = setupHealthService;
    }

    @GetMapping("/api/v1/setup/health")
    public SetupHealthReport health() {
        return setupHealthService.health();
    }
}
