package com.acip.worktracking;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "acip.work-tracking", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockWorkTrackingProvider implements WorkTrackingProvider {

    private final List<WorkItem> epics = List.of(
            epic("PAY-1000", "Checkout Modernization", "payments"),
            epic("PAY-2000", "Payment Gateway Migration", "payments"),
            epic("CX-1000", "Support Automation", "customer-experience"),
            epic("CX-2000", "Search Improvements", "customer-experience"),
            epic("PLAT-1000", "Developer Platform Modernization", "platform")
    );

    private final List<WorkItem> stories = List.of(
            story("PAY-1001", "Refactor Tax Calculation Service", "payments", "PAY-1000", "CAPITALIZED", "In Progress"),
            story("PAY-1002", "Add Payment Retry Workflow", "payments", "PAY-1000", "CAPITALIZED", "Done"),
            story("PAY-1003", "Improve Checkout Error Handling", "payments", "PAY-1000", "OPERATIONAL", "In Progress"),
            story("PAY-2001", "Add Gateway Failover Adapter", "payments", "PAY-2000", "CAPITALIZED", "In Progress"),
            story("PAY-2002", "Migrate Refund Webhooks", "payments", "PAY-2000", "CAPITALIZED", "To Do"),
            story("PAY-2003", "Retire Legacy Gateway Toggle", "payments", "PAY-2000", "SUPPORT", "Cancelled"),
            story("CX-1001", "Add Support Bot Integration", "customer-experience", "CX-1000", "CAPITALIZED", "In Progress"),
            story("CX-1002", "Improve FAQ Search Results", "customer-experience", "CX-2000", "CAPITALIZED", "Done"),
            story("CX-1003", "Classify Escalation Reasons", "customer-experience", "CX-1000", "RESEARCH", "In Progress"),
            story("CX-1004", "Summarize Support Transcripts", "customer-experience", "CX-1000", "CAPITALIZED", "To Do"),
            story("CX-2001", "Tune Search Ranking Signals", "customer-experience", "CX-2000", "CAPITALIZED", "In Progress"),
            story("CX-2002", "Add Search Analytics Export", "customer-experience", "CX-2000", "OPERATIONAL", "Done"),
            story("PLAT-1001", "Add Internal Developer Portal API", "platform", "PLAT-1000", "CAPITALIZED", "In Progress"),
            story("PLAT-1002", "Create Golden Path Templates", "platform", "PLAT-1000", "CAPITALIZED", "Done"),
            story("PLAT-1003", "Automate Service Catalog Updates", "platform", "PLAT-1000", "OPERATIONAL", "In Progress"),
            story("PLAT-1004", "Improve Local Dev Bootstrap", "platform", "PLAT-1000", "SUPPORT", "In Progress"),
            story("PAY-1004", "Add Checkout Experiment Metrics", "payments", "PAY-1000", "RESEARCH", "To Do"),
            story("CX-2003", "Improve Query Understanding", "customer-experience", "CX-2000", "CAPITALIZED", "In Progress"),
            story("PLAT-1005", "Add Cost Attribution SDK", "platform", "PLAT-1000", "CAPITALIZED", "In Progress"),
            story("PAY-2004", "Gateway Migration Runbook", "payments", "PAY-2000", "OPERATIONAL", "Done")
    );

    @Override
    public List<WorkItem> fetchStories() {
        return stories;
    }

    @Override
    public List<WorkItem> fetchEpics() {
        return epics;
    }

    @Override
    public Optional<WorkItem> findStoryByKey(String storyKey) {
        return stories.stream().filter(story -> story.key().equalsIgnoreCase(storyKey)).findFirst();
    }

    @Override
    public Optional<WorkItem> findEpicByKey(String epicKey) {
        return epics.stream().filter(epic -> epic.key().equalsIgnoreCase(epicKey)).findFirst();
    }

    private WorkItem epic(String key, String summary, String teamKey) {
        return new WorkItem(key, WorkItemType.EPIC, summary, "In Progress", teamKey, null, "UNKNOWN");
    }

    private WorkItem story(String key, String summary, String teamKey, String epicKey, String workType, String status) {
        return new WorkItem(key, WorkItemType.STORY, summary, status, teamKey, epicKey, workType);
    }
}
