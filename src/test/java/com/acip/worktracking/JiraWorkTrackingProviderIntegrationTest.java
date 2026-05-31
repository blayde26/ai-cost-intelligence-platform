package com.acip.worktracking;

import com.acip.jira.JiraClient;
import com.acip.jira.JiraIssueMapper;
import com.acip.jira.JiraProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "JIRA_API_TOKEN", matches = ".+")
@EnabledIfEnvironmentVariable(named = "JIRA_EMAIL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "JIRA_BASE_URL", matches = ".+")
class JiraWorkTrackingProviderIntegrationTest {

    @Test
    void connectsToConfiguredJiraCloudSite() {
        JiraProperties properties = new JiraProperties(
                System.getenv("JIRA_BASE_URL"),
                System.getenv("JIRA_EMAIL"),
                System.getenv("JIRA_API_TOKEN"),
                System.getenv().getOrDefault("JIRA_DEFAULT_JQL", "project = KAN ORDER BY updated DESC"),
                50,
                System.getenv().getOrDefault("JIRA_EPIC_LINK_FIELD", "customfield_10014"),
                System.getenv().getOrDefault("JIRA_WORK_TYPE_FIELD", "customfield_10015")
        );
        JiraClient jiraClient = new JiraClient(
                properties,
                new JiraIssueMapper(properties),
                RestClient.builder()
        );
        JiraWorkTrackingProvider provider = new JiraWorkTrackingProvider(jiraClient);

        assertThat(provider.fetchStories())
                .as("configured Jira site should be reachable and return a list")
                .isNotNull();

        String expectedStoryKey = System.getenv("JIRA_EXPECTED_STORY_KEY");
        if (expectedStoryKey != null && !expectedStoryKey.isBlank()) {
            assertThat(provider.findStoryByKey(expectedStoryKey))
                    .as("configured Jira site should resolve expected story key")
                    .isPresent();
        }
    }
}
