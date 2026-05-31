package com.acip.jira;

import com.acip.worktracking.WorkItem;
import com.acip.worktracking.WorkTrackingProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JiraSyncService {

    private final WorkTrackingProvider workTrackingProvider;
    private final EpicRepository epicRepository;
    private final StoryRepository storyRepository;

    public JiraSyncService(
            WorkTrackingProvider workTrackingProvider,
            EpicRepository epicRepository,
            StoryRepository storyRepository
    ) {
        this.workTrackingProvider = workTrackingProvider;
        this.epicRepository = epicRepository;
        this.storyRepository = storyRepository;
    }

    public JiraSyncResult sync(JiraSyncRequest request) {
        List<WorkItem> issues = workItemsFor(request);
        int epics = 0;
        int stories = 0;
        for (WorkItem issue : issues) {
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

    private List<WorkItem> workItemsFor(JiraSyncRequest request) {
        if (request != null && request.jql() != null && !request.jql().isBlank()) {
            return workTrackingProvider.fetchWorkItems(request.jql());
        }
        return workTrackingProvider.fetchWorkItems(null);
    }
}
