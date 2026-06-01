package com.acip.jira;

import com.acip.setup.SetupHealthStatus;

public record JiraConnectionTestResult(
        SetupHealthStatus status,
        boolean configured,
        boolean reachable,
        boolean issuesReadable,
        int issuesFetched,
        String sampleIssueKey,
        String message
) {
}
