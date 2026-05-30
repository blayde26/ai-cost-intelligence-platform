package com.acip.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Component
public class OpenAiGateway {

    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiGateway(OpenAiProperties properties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    public UpstreamResponse postChatCompletions(JsonNode request) {
        if (properties.requireApiKey() && isBlank(properties.apiKey())) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "OpenAI API key is not configured.");
        }
        try {
            RestClient.RequestBodySpec requestSpec = restClient.post()
                    .uri(properties.chatCompletionsUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(toJson(request));
            if (!isBlank(properties.apiKey())) {
                requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey());
            }
            return requestSpec
                    .exchange((clientRequest, clientResponse) -> new UpstreamResponse(
                            clientResponse.getStatusCode(),
                            readBody(clientResponse),
                            HttpHeaders.readOnlyHttpHeaders(clientResponse.getHeaders())
                    ));
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "OpenAI request failed or timed out.", exception);
        }
    }

    private String toJson(JsonNode request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize OpenAI request.", exception);
        }
    }

    private String readBody(ClientHttpResponse response) throws IOException {
        return StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
