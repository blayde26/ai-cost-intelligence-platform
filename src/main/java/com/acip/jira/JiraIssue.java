package com.acip.jira;

public record JiraIssue(
        String issueKey,
        String issueType,
        String summary,
        String status,
        String teamKey,
        String epicKey,
        String workType
) {

    public boolean isEpic() {
        return "Epic".equalsIgnoreCase(issueType);
    }
}
