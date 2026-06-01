package com.acip.jira;

import com.acip.setup.SetupHealthStatus;
import com.acip.worktracking.WorkItem;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class JiraConnectionTestService {

    private final JiraProperties jiraProperties;
    private final JiraClient jiraClient;

    public JiraConnectionTestService(JiraProperties jiraProperties, JiraClient jiraClient) {
        this.jiraProperties = jiraProperties;
        this.jiraClient = jiraClient;
    }

    public JiraConnectionTestResult testConnection() {
        if (!jiraProperties.isConfigured()) {
            return new JiraConnectionTestResult(
                    SetupHealthStatus.NOT_CONFIGURED,
                    false,
                    false,
                    false,
                    0,
                    null,
                    "Jira base URL, email, and API token must be configured before ACIP can test the connection."
            );
        }

        try {
            List<WorkItem> issues = jiraClient.searchFirstPage(testJql(), 5);
            String sampleIssueKey = issues.stream().findFirst().map(WorkItem::key).orElse(null);
            String message = issues.isEmpty()
                    ? "Jira credentials are valid, but the configured JQL did not return any readable issues."
                    : "Jira connection succeeded and ACIP can read " + issues.size() + " issues.";
            return new JiraConnectionTestResult(
                    SetupHealthStatus.READY,
                    true,
                    true,
                    true,
                    issues.size(),
                    sampleIssueKey,
                    message
            );
        } catch (ResponseStatusException exception) {
            return new JiraConnectionTestResult(
                    SetupHealthStatus.ERROR,
                    true,
                    false,
                    false,
                    0,
                    null,
                    exception.getReason() == null ? "Jira connection test failed." : exception.getReason()
            );
        } catch (RuntimeException exception) {
            return new JiraConnectionTestResult(
                    SetupHealthStatus.ERROR,
                    true,
                    false,
                    false,
                    0,
                    null,
                    "Jira connection test failed: " + exception.getMessage()
            );
        }
    }

    private String testJql() {
        if (jiraProperties.defaultJql() == null || jiraProperties.defaultJql().isBlank()) {
            return "project is not EMPTY ORDER BY updated DESC";
        }
        return jiraProperties.defaultJql();
    }
}
