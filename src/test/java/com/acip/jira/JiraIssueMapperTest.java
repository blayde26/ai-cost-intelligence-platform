package com.acip.jira;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JiraIssueMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JiraIssueMapper mapper = new JiraIssueMapper(new JiraProperties(
            "",
            "",
            "",
            "project is not EMPTY",
            50,
            "customfield_10014",
            "customfield_10015"
    ));

    @Test
    void mapsStoryWithParentEpicAndWorkType() throws Exception {
        JiraIssue issue = mapper.map(objectMapper.readTree("""
                {
                  "key": "ACIP-123",
                  "fields": {
                    "summary": "Build attribution",
                    "status": { "name": "In Progress" },
                    "issuetype": { "name": "Story" },
                    "project": { "key": "ACIP" },
                    "parent": { "key": "ACIP-1" },
                    "customfield_10015": { "value": "Capitalized" }
                  }
                }
                """));

        assertThat(issue.issueKey()).isEqualTo("ACIP-123");
        assertThat(issue.issueType()).isEqualTo("Story");
        assertThat(issue.summary()).isEqualTo("Build attribution");
        assertThat(issue.status()).isEqualTo("In Progress");
        assertThat(issue.teamKey()).isEqualTo("ACIP");
        assertThat(issue.epicKey()).isEqualTo("ACIP-1");
        assertThat(issue.workType()).isEqualTo("CAPITALIZED");
    }

    @Test
    void fallsBackToConfiguredEpicField() throws Exception {
        JiraIssue issue = mapper.map(objectMapper.readTree("""
                {
                  "key": "ACIP-124",
                  "fields": {
                    "summary": "Add reports",
                    "status": { "name": "To Do" },
                    "issuetype": { "name": "Task" },
                    "project": { "key": "ACIP" },
                    "customfield_10014": "ACIP-2"
                  }
                }
                """));

        assertThat(issue.epicKey()).isEqualTo("ACIP-2");
        assertThat(issue.workType()).isEqualTo("UNKNOWN");
    }
}
