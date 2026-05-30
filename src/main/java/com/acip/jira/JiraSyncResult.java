package com.acip.jira;

public record JiraSyncResult(
        int issuesFetched,
        int epicsUpserted,
        int storiesUpserted
) {
}
