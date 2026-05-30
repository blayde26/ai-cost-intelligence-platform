package com.acip.jira;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JiraSyncService {

    private final JiraProperties properties;
    private final JiraClient jiraClient;
    private final EpicRepository epicRepository;
    private final StoryRepository storyRepository;

    public JiraSyncService(
            JiraProperties properties,
            JiraClient jiraClient,
            EpicRepository epicRepository,
            StoryRepository storyRepository
    ) {
        this.properties = properties;
        this.jiraClient = jiraClient;
        this.epicRepository = epicRepository;
        this.storyRepository = storyRepository;
    }

    public JiraSyncResult sync(JiraSyncRequest request) {
        String jql = request == null || request.jql() == null || request.jql().isBlank()
                ? properties.defaultJql()
                : request.jql();
        List<JiraIssue> issues = jiraClient.searchIssues(jql);
        int epics = 0;
        int stories = 0;
        for (JiraIssue issue : issues) {
            if (issue.isEpic()) {
                epicRepository.upsert(issue);
                epics++;
            } else {
                storyRepository.upsert(issue);
                stories++;
            }
        }
        return new JiraSyncResult(issues.size(), epics, stories);
    }
}
