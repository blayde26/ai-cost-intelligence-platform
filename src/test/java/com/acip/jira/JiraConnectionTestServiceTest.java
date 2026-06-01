package com.acip.jira;

import com.acip.setup.SetupHealthStatus;
import com.acip.worktracking.WorkItem;
import com.acip.worktracking.WorkItemType;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

class JiraConnectionTestServiceTest {

    private final JiraClient jiraClient = mock(JiraClient.class);

    @Test
    void returnsNotConfiguredWithoutCallingJiraWhenCredentialsAreMissing() {
        JiraConnectionTestService service = new JiraConnectionTestService(properties("", "", "", "project = KAN"), jiraClient);

        JiraConnectionTestResult result = service.testConnection();

        assertThat(result.status()).isEqualTo(SetupHealthStatus.NOT_CONFIGURED);
        assertThat(result.configured()).isFalse();
        assertThat(result.reachable()).isFalse();
        assertThat(result.issuesReadable()).isFalse();
        verifyNoInteractions(jiraClient);
    }

    @Test
    void reportsReadyWhenJiraReturnsIssues() {
        JiraConnectionTestService service = new JiraConnectionTestService(properties("https://example.atlassian.net", "svc@example.com", "token", "project = KAN"), jiraClient);
        WorkItem issue = new WorkItem("KAN-9", WorkItemType.STORY, "Test story", "To Do", "KAN", null, "CAPITALIZED");
        when(jiraClient.searchFirstPage("project = KAN", 5)).thenReturn(List.of(issue));

        JiraConnectionTestResult result = service.testConnection();

        assertThat(result.status()).isEqualTo(SetupHealthStatus.READY);
        assertThat(result.configured()).isTrue();
        assertThat(result.reachable()).isTrue();
        assertThat(result.issuesReadable()).isTrue();
        assertThat(result.issuesFetched()).isEqualTo(1);
        assertThat(result.sampleIssueKey()).isEqualTo("KAN-9");
        verify(jiraClient).searchFirstPage("project = KAN", 5);
    }

    @Test
    void reportsErrorWhenJiraClientFails() {
        JiraConnectionTestService service = new JiraConnectionTestService(properties("https://example.atlassian.net", "svc@example.com", "token", "project = KAN"), jiraClient);
        when(jiraClient.searchFirstPage("project = KAN", 5)).thenThrow(new ResponseStatusException(BAD_GATEWAY, "Jira search request failed."));

        JiraConnectionTestResult result = service.testConnection();

        assertThat(result.status()).isEqualTo(SetupHealthStatus.ERROR);
        assertThat(result.configured()).isTrue();
        assertThat(result.reachable()).isFalse();
        assertThat(result.issuesReadable()).isFalse();
        assertThat(result.message()).isEqualTo("Jira search request failed.");
    }

    private JiraProperties properties(String baseUrl, String email, String apiToken, String defaultJql) {
        return new JiraProperties(baseUrl, email, apiToken, defaultJql, 50, "customfield_10014", "customfield_10015");
    }
}
