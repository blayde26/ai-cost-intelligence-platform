package com.acip.proxy;

import com.acip.pricing.PricingService;
import com.acip.usage.UsageEvent;
import com.acip.usage.UsageEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class OpenAiProxyService {

    private final OpenAiGateway openAiGateway;
    private final OpenAiProperties openAiProperties;
    private final OpenAiUsageParser usageParser;
    private final PricingService pricingService;
    private final UsageEventRepository usageEventRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public OpenAiProxyService(
            OpenAiGateway openAiGateway,
            OpenAiProperties openAiProperties,
            OpenAiUsageParser usageParser,
            PricingService pricingService,
            UsageEventRepository usageEventRepository,
            ObjectMapper objectMapper
    ) {
        this(openAiGateway, openAiProperties, usageParser, pricingService, usageEventRepository, objectMapper, Clock.systemUTC());
    }

    OpenAiProxyService(
            OpenAiGateway openAiGateway,
            OpenAiProperties openAiProperties,
            OpenAiUsageParser usageParser,
            PricingService pricingService,
            UsageEventRepository usageEventRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.openAiGateway = openAiGateway;
        this.openAiProperties = openAiProperties;
        this.usageParser = usageParser;
        this.pricingService = pricingService;
        this.usageEventRepository = usageEventRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public ResponseEntity<String> proxyChatCompletions(OpenAiProxyEnvelope envelope) {
        validateOpenAiRequest(envelope.request());

        String model = envelope.request().path("model").asText();
        String requestHash = hashRequest(envelope.request());
        long startNanos = System.nanoTime();
        OffsetDateTime requestTimestamp = OffsetDateTime.now(clock);

        UpstreamResponse upstreamResponse = openAiGateway.postChatCompletions(envelope.request());
        long latencyMs = elapsedMs(startNanos);
        OpenAiTokenUsage usage = upstreamResponse.statusCode().is2xxSuccessful()
                ? usageParser.parse(upstreamResponse.body())
                : OpenAiTokenUsage.empty();
        BigDecimal estimatedCostUsd = pricingService.estimateCostUsd(
                openAiProperties.provider(),
                model,
                usage.promptTokens(),
                usage.completionTokens()
        );

        usageEventRepository.save(new UsageEvent(
                UUID.randomUUID(),
                openAiProperties.provider(),
                model,
                envelope.attribution().storyKey(),
                null,
                envelope.attribution().teamKey(),
                envelope.attribution().userKey(),
                usage.promptTokens(),
                usage.completionTokens(),
                usage.totalTokens(),
                estimatedCostUsd,
                latencyMs,
                requestTimestamp,
                "local",
                "UNKNOWN",
                upstreamResponse.statusCode().is2xxSuccessful() ? "SUCCEEDED" : "FAILED",
                requestHash
        ));

        return ResponseEntity.status(upstreamResponse.statusCode())
                .headers(responseHeaders(upstreamResponse.headers()))
                .body(upstreamResponse.body());
    }

    private void validateOpenAiRequest(JsonNode request) {
        if (!request.hasNonNull("model") || request.path("model").asText().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "request.model is required.");
        }
        if (!request.has("messages") || !request.path("messages").isArray() || request.path("messages").isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "request.messages must be a non-empty array.");
        }
    }

    private String hashRequest(JsonNode request) {
        try {
            byte[] canonicalRequest = objectMapper.writeValueAsBytes(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonicalRequest));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash OpenAI request.", exception);
        }
    }

    private long elapsedMs(long startNanos) {
        return Math.max(0, (System.nanoTime() - startNanos) / 1_000_000);
    }

    private HttpHeaders responseHeaders(HttpHeaders upstreamHeaders) {
        HttpHeaders headers = new HttpHeaders();
        MediaType contentType = upstreamHeaders.getContentType();
        headers.setContentType(contentType == null ? MediaType.APPLICATION_JSON : contentType);
        return headers;
    }
}
