package com.acip.jira;

import com.fasterxml.jackson.databind.JsonNode;
import com.acip.worktracking.WorkItem;
import com.acip.worktracking.WorkItemType;
import org.springframework.stereotype.Component;

@Component
public class JiraIssueMapper {

    private final JiraProperties properties;

    public JiraIssueMapper(JiraProperties properties) {
        this.properties = properties;
    }

    public WorkItem map(JsonNode issue) {
        JsonNode fields = issue.path("fields");
        String issueKey = issue.path("key").asText();
        String issueType = fields.path("issuetype").path("name").asText("Unknown");
        String summary = fields.path("summary").asText("");
        String status = fields.path("status").path("name").asText("UNKNOWN");
        String teamKey = textFrom(fields.path("project").path("key"));
        String epicKey = epicKey(fields);
        String workType = workType(fields);
        WorkItemType type = "Epic".equalsIgnoreCase(issueType) ? WorkItemType.EPIC : WorkItemType.STORY;
        return new WorkItem(issueKey, type, summary, status, teamKey, epicKey, workType);
    }

    private String epicKey(JsonNode fields) {
        String parentKey = textFrom(fields.path("parent").path("key"));
        if (parentKey != null) {
            return parentKey;
        }
        return textFrom(fields.path(properties.epicLinkField()));
    }

    private String workType(JsonNode fields) {
        String value = textFrom(fields.path(properties.workTypeField()));
        if (value == null) {
            return "UNKNOWN";
        }
        String normalized = value.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return switch (normalized) {
            case "CAPITALIZED", "OPERATIONAL", "SUPPORT", "RESEARCH" -> normalized;
            default -> "UNKNOWN";
        };
    }

    private String textFrom(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            String value = node.asText();
            return value.isBlank() ? null : value;
        }
        if (node.hasNonNull("value")) {
            return textFrom(node.path("value"));
        }
        if (node.hasNonNull("name")) {
            return textFrom(node.path("name"));
        }
        if (node.hasNonNull("key")) {
            return textFrom(node.path("key"));
        }
        return null;
    }
}
