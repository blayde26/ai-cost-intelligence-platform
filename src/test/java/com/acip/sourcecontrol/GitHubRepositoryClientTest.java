package com.acip.sourcecontrol;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubRepositoryClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void calculatesRepositoryMetricsFromGitHubResponses() throws Exception {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/repos/acme/api/pulls", exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            writeJson(exchange, 200, """
                    [
                      {
                        "number": 7,
                        "created_at": "2026-05-31T10:00:00Z",
                        "merged_at": "2026-05-31T16:00:00Z",
                        "comments": 3,
                        "review_comments": 4
                      },
                      {
                        "number": 8,
                        "created_at": "2026-05-31T09:00:00Z",
                        "merged_at": null,
                        "comments": 1,
                        "review_comments": 1
                      }
                    ]
                    """);
        });
        server.createContext("/repos/acme/api/commits", exchange -> writeJson(exchange, 200, "[{\"sha\":\"a\"},{\"sha\":\"b\"},{\"sha\":\"c\"}]"));
        server.createContext("/repos/acme/api/pulls/7/reviews", exchange -> writeJson(exchange, 200, """
                [
                  {"submitted_at": "2026-05-31T12:00:00Z"},
                  {"submitted_at": "2026-05-31T13:00:00Z"}
                ]
                """));
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        SourceControlProperties properties = new SourceControlProperties(
                "github",
                "",
                "http://localhost:" + server.getAddress().getPort(),
                "test-token",
                "acme/api:platform",
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                Duration.ofMinutes(5)
        );
        GitHubRepositoryClient client = new GitHubRepositoryClient(properties, RestClient.builder());

        Optional<RepositoryOutcomeMetrics> metrics = client.metrics(properties.configuredRepositories().getFirst());

        assertThat(metrics).isPresent();
        assertThat(metrics.get().repository()).isEqualTo("api");
        assertThat(metrics.get().owner()).isEqualTo("acme");
        assertThat(metrics.get().teamKey()).isEqualTo("platform");
        assertThat(metrics.get().prCount()).isEqualTo(1);
        assertThat(metrics.get().commitCount()).isEqualTo(3);
        assertThat(metrics.get().reviewCount()).isEqualTo(2);
        assertThat(metrics.get().commentCount()).isEqualTo(7);
        assertThat(metrics.get().averageMergeTimeHours()).isEqualTo(6.0);
        assertThat(metrics.get().averageReviewTimeHours()).isEqualTo(2.0);
        assertThat(authorizationHeader.get()).isEqualTo("Bearer test-token");
    }

    @Test
    void returnsEmptyWhenGitHubIsNotConfigured() {
        SourceControlProperties properties = new SourceControlProperties("github", "", "http://localhost:1", "", "", Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofMinutes(5));
        GitHubRepositoryClient client = new GitHubRepositoryClient(properties, RestClient.builder());

        assertThat(client.metrics(new SourceControlProperties.ConfiguredRepository("acme", "api", "platform", "acme/api:platform"))).isEmpty();
    }

    private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
