package com.acip.sourcecontrol;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class GitHubRepositoryClient {

    private final SourceControlProperties properties;
    private final RestClient restClient;

    public GitHubRepositoryClient(SourceControlProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.effectiveConnectTimeout());
        requestFactory.setReadTimeout(properties.effectiveReadTimeout());
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    public Optional<RepositoryOutcomeMetrics> metrics(SourceControlProperties.ConfiguredRepository repository) {
        if (!properties.isGitHubConfigured()) {
            return Optional.empty();
        }
        try {
            List<JsonNode> pullRequests = fetchArray("/repos/%s/%s/pulls?state=closed&per_page=100".formatted(repository.owner(), repository.name()));
            List<JsonNode> mergedPullRequests = pullRequests.stream()
                    .filter(pr -> !isBlank(pr.path("merged_at").asText(null)))
                    .toList();
            long commitCount = fetchArray("/repos/%s/%s/commits?per_page=100".formatted(repository.owner(), repository.name())).size();
            ReviewMetrics reviewMetrics = reviewMetrics(repository, mergedPullRequests);
            return Optional.of(new RepositoryOutcomeMetrics(
                    repository.name(),
                    repository.owner(),
                    repository.teamKey(),
                    mergedPullRequests.size(),
                    commitCount,
                    reviewMetrics.reviewCount(),
                    reviewMetrics.commentCount(),
                    averageMergeTimeHours(mergedPullRequests),
                    reviewMetrics.averageReviewTimeHours()
            ));
        } catch (ResourceAccessException exception) {
            return Optional.empty();
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private ReviewMetrics reviewMetrics(SourceControlProperties.ConfiguredRepository repository, List<JsonNode> pullRequests) {
        long reviewCount = 0;
        long commentCount = 0;
        List<Double> reviewDurations = new ArrayList<>();
        for (JsonNode pullRequest : pullRequests) {
            int number = pullRequest.path("number").asInt();
            commentCount += pullRequest.path("comments").asLong(0);
            commentCount += pullRequest.path("review_comments").asLong(0);
            List<JsonNode> reviews = fetchArray("/repos/%s/%s/pulls/%d/reviews?per_page=100".formatted(repository.owner(), repository.name(), number));
            reviewCount += reviews.size();
            earliestReviewDuration(pullRequest, reviews).ifPresent(reviewDurations::add);
        }
        return new ReviewMetrics(reviewCount, commentCount, average(reviewDurations));
    }

    private Optional<Double> earliestReviewDuration(JsonNode pullRequest, List<JsonNode> reviews) {
        OffsetDateTime createdAt = parseTimestamp(pullRequest.path("created_at").asText(null));
        if (createdAt == null) {
            return Optional.empty();
        }
        return reviews.stream()
                .map(review -> parseTimestamp(review.path("submitted_at").asText(null)))
                .filter(timestamp -> timestamp != null && !timestamp.isBefore(createdAt))
                .min(OffsetDateTime::compareTo)
                .map(timestamp -> hoursBetween(createdAt, timestamp));
    }

    private double averageMergeTimeHours(List<JsonNode> pullRequests) {
        List<Double> durations = pullRequests.stream()
                .map(pullRequest -> {
                    OffsetDateTime createdAt = parseTimestamp(pullRequest.path("created_at").asText(null));
                    OffsetDateTime mergedAt = parseTimestamp(pullRequest.path("merged_at").asText(null));
                    if (createdAt == null || mergedAt == null || mergedAt.isBefore(createdAt)) {
                        return null;
                    }
                    return hoursBetween(createdAt, mergedAt);
                })
                .filter(value -> value != null)
                .toList();
        return average(durations);
    }

    private List<JsonNode> fetchArray(String path) {
        JsonNode response = restClient.get()
                .uri(properties.effectiveApiBaseUrl() + path)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                .header(HttpHeaders.USER_AGENT, "acip-local")
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.isArray()) {
            return List.of();
        }
        List<JsonNode> items = new ArrayList<>();
        response.forEach(items::add);
        return items;
    }

    private OffsetDateTime parseTimestamp(String value) {
        if (isBlank(value)) {
            return null;
        }
        return OffsetDateTime.parse(value);
    }

    private double hoursBetween(OffsetDateTime start, OffsetDateTime end) {
        return ChronoUnit.MINUTES.between(start, end) / 60.0;
    }

    private double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        return Math.round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 100.0) / 100.0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ReviewMetrics(
            long reviewCount,
            long commentCount,
            double averageReviewTimeHours
    ) {
    }
}
