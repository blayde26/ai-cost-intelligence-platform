package com.acip.jira;

import com.fasterxml.jackson.databind.JsonNode;
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

    public List<JiraIssue> searchIssues(String jql) {
        if (!properties.isConfigured()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Jira is not configured.");
        }

        List<JiraIssue> issues = new ArrayList<>();
        int startAt = 0;
        int total;
        do {
            JsonNode page = fetchPage(jql, startAt);
            JsonNode issueNodes = page.path("issues");
            if (!issueNodes.isArray()) {
                throw new ResponseStatusException(BAD_GATEWAY, "Jira search response did not include issues.");
            }
            issueNodes.forEach(issue -> issues.add(issueMapper.map(issue)));
            total = page.path("total").asInt(issues.size());
            startAt += issueNodes.size();
        } while (startAt < total && startAt > 0);

        return issues;
    }

    private JsonNode fetchPage(String jql, int startAt) {
        try {
            return restClient.post()
                    .uri(properties.baseUrl() + "/rest/api/3/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuthToken())
                    .body(Map.of(
                            "jql", jql,
                            "startAt", startAt,
                            "maxResults", properties.pageSize()
                    ))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Jira request failed or timed out.", exception);
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "Jira search request failed.", exception);
        }
    }

    private String basicAuthToken() {
        String credentials = properties.email() + ":" + properties.apiToken();
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
