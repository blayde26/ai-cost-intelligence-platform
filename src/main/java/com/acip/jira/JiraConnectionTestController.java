package com.acip.jira;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JiraConnectionTestController {

    private final JiraConnectionTestService jiraConnectionTestService;

    public JiraConnectionTestController(JiraConnectionTestService jiraConnectionTestService) {
        this.jiraConnectionTestService = jiraConnectionTestService;
    }

    @GetMapping("/api/v1/jira/connection-test")
    public JiraConnectionTestResult testConnection() {
        return jiraConnectionTestService.testConnection();
    }
}
