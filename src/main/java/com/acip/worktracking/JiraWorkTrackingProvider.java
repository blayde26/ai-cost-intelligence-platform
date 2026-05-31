package com.acip.worktracking;

import com.acip.jira.JiraClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "acip.work-tracking", name = "provider", havingValue = "jira")
public class JiraWorkTrackingProvider implements WorkTrackingProvider {

    private final JiraClient jiraClient;

    public JiraWorkTrackingProvider(JiraClient jiraClient) {
        this.jiraClient = jiraClient;
    }

    @Override
    public List<WorkItem> fetchStories() {
        return jiraClient.fetchStories();
    }

    @Override
    public List<WorkItem> fetchEpics() {
        return jiraClient.fetchEpics();
    }

    @Override
    public List<WorkItem> fetchWorkItems(String query) {
        return jiraClient.searchIssues(query);
    }

    @Override
    public Optional<WorkItem> findStoryByKey(String storyKey) {
        return jiraClient.findStoryByKey(storyKey);
    }

    @Override
    public Optional<WorkItem> findEpicByKey(String epicKey) {
        return jiraClient.findEpicByKey(epicKey);
    }
}
