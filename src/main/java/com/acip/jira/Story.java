package com.acip.jira;

public record Story(
        String storyKey,
        String summary,
        String status,
        String epicKey,
        String workType
) {
}
