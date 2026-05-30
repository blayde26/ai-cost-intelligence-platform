package com.acip.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiGatewayTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void allowsLocalProvidersWithoutApiKeyWhenNotRequired() throws Exception {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            writeJson(exchange, 200, "{\"id\":\"mock\",\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1,\"total_tokens\":2}}");
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();

        OpenAiGateway gateway = gateway(false, "", server.getAddress().getPort());

        UpstreamResponse response = gateway.postChatCompletions(objectMapper.readTree("""
                {"model":"mock-gpt-4o-mini","messages":[{"role":"user","content":"hello"}]}
                """));

        assertThat(response.statusCode().value()).isEqualTo(200);
        assertThat(authorizationHeader.get()).isNull();
        assertThat(objectMapper.readTree(requestBody.get()).path("messages").isArray()).isTrue();
        assertThat(objectMapper.readTree(requestBody.get()).path("request").isMissingNode()).isTrue();
    }

    @Test
    void rejectsMissingApiKeyWhenRequired() throws Exception {
        OpenAiGateway gateway = gateway(true, "", 65535);

        assertThatThrownBy(() -> gateway.postChatCompletions(objectMapper.readTree("""
                {"model":"gpt-4o-mini","messages":[{"role":"user","content":"hello"}]}
                """)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("OpenAI API key is not configured");
    }

    private OpenAiGateway gateway(boolean requireApiKey, String apiKey, int port) {
        OpenAiProperties properties = new OpenAiProperties(
                "MOCK_LLM",
                apiKey,
                requireApiKey,
                "http://localhost:" + port + "/v1/chat/completions",
                Duration.ofSeconds(5),
                Duration.ofSeconds(5)
        );
        return new OpenAiGateway(properties, objectMapper, RestClient.builder());
    }

    private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
