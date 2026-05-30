package com.acip.jira;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JiraSyncController {

    private final JiraSyncService jiraSyncService;

    public JiraSyncController(JiraSyncService jiraSyncService) {
        this.jiraSyncService = jiraSyncService;
    }

    @PostMapping("/api/v1/jira/sync")
    public JiraSyncResult sync(@RequestBody(required = false) JiraSyncRequest request) {
        return jiraSyncService.sync(request);
    }
}
