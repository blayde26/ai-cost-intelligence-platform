package com.acip.jira;

import com.fasterxml.jackson.databind.JsonNode;
import com.acip.worktracking.WorkItem;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Component
public class JiraClient {

    private final JiraProperties properties;
    private final JiraIssueMapper issueMapper;
    private final RestClient restClient;

    public JiraClient(JiraProperties properties, JiraIssueMapper issueMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.issueMapper = issueMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    public List<WorkItem> searchIssues(String jql) {
        if (!properties.isConfigured()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Jira is not configured.");
        }

        List<WorkItem> issues = new ArrayList<>();
        String nextPageToken = null;
        boolean isLast;
        do {
            JsonNode page = fetchPage(jql, properties.pageSize(), nextPageToken);
            JsonNode issueNodes = page.path("issues");
            if (!issueNodes.isArray()) {
                throw new ResponseStatusException(BAD_GATEWAY, "Jira search response did not include issues.");
            }
            issueNodes.forEach(issue -> issues.add(issueMapper.map(issue)));
            isLast = page.path("isLast").asBoolean(true);
            nextPageToken = page.path("nextPageToken").asText(null);
        } while (!isLast && nextPageToken != null && !nextPageToken.isBlank());

        return issues;
    }

    public List<WorkItem> searchFirstPage(String jql, int maxResults) {
        if (!properties.isConfigured()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Jira is not configured.");
        }
        JsonNode page = fetchPage(jql, Math.max(1, Math.min(maxResults, properties.pageSize())), null);
        JsonNode issueNodes = page.path("issues");
        if (!issueNodes.isArray()) {
            throw new ResponseStatusException(BAD_GATEWAY, "Jira search response did not include issues.");
        }
        List<WorkItem> issues = new ArrayList<>();
        issueNodes.forEach(issue -> issues.add(issueMapper.map(issue)));
        return issues;
    }

    public List<WorkItem> fetchStories() {
        return searchIssues(properties.defaultJql()).stream().filter(item -> !item.isEpic()).toList();
    }

    public List<WorkItem> fetchEpics() {
        return searchIssues(properties.defaultJql()).stream().filter(WorkItem::isEpic).toList();
    }

    public java.util.Optional<WorkItem> findStoryByKey(String storyKey) {
        return searchIssues("key = " + storyKey).stream().filter(item -> !item.isEpic()).findFirst();
    }

    public java.util.Optional<WorkItem> findEpicByKey(String epicKey) {
        return searchIssues("key = " + epicKey).stream().filter(WorkItem::isEpic).findFirst();
    }

    private JsonNode fetchPage(String jql, int maxResults, String nextPageToken) {
        try {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("jql", jql);
            body.put("maxResults", maxResults);
            body.put("fields", fields());
            if (nextPageToken != null && !nextPageToken.isBlank()) {
                body.put("nextPageToken", nextPageToken);
            }
            return restClient.post()
                    .uri(properties.baseUrl() + "/rest/api/3/search/jql")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuthToken())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Jira request failed or timed out.", exception);
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "Jira search request failed.", exception);
        }
    }

    private List<String> fields() {
        return List.of(
                "summary",
                "status",
                "issuetype",
                "project",
                "parent",
                properties.epicLinkField(),
                properties.workTypeField()
        );
    }

    private String basicAuthToken() {
        String credentials = properties.email() + ":" + properties.apiToken();
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
