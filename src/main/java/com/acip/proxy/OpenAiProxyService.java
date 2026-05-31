package com.acip.proxy;

import com.acip.capture.ProxyCaptureProvider;
import com.acip.capture.UsageCaptureMetadata;
import com.acip.pricing.PricingService;
import com.acip.usage.AttributionInference;
import com.acip.usage.AttributionInferenceService;
import com.acip.usage.AttributionSource;
import com.acip.usage.AttributionStatus;
import com.acip.usage.AttributionStatusService;
import com.acip.usage.UsageEvent;
import com.acip.usage.UsageEventRepository;
import com.acip.worktracking.WorkItem;
import com.acip.worktracking.WorkTrackingProvider;
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
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class OpenAiProxyService {

    private final OpenAiGateway openAiGateway;
    private final OpenAiProperties openAiProperties;
    private final OpenAiUsageParser usageParser;
    private final PricingService pricingService;
    private final UsageEventRepository usageEventRepository;
    private final ProxyCaptureProvider proxyCaptureProvider;
    private final WorkTrackingProvider workTrackingProvider;
    private final AttributionInferenceService attributionInferenceService;
    private final AttributionStatusService attributionStatusService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public OpenAiProxyService(
            OpenAiGateway openAiGateway,
            OpenAiProperties openAiProperties,
            OpenAiUsageParser usageParser,
            PricingService pricingService,
            UsageEventRepository usageEventRepository,
            ProxyCaptureProvider proxyCaptureProvider,
            WorkTrackingProvider workTrackingProvider,
            AttributionInferenceService attributionInferenceService,
            AttributionStatusService attributionStatusService,
            ObjectMapper objectMapper
    ) {
        this(openAiGateway, openAiProperties, usageParser, pricingService, usageEventRepository, proxyCaptureProvider, workTrackingProvider, attributionInferenceService, attributionStatusService, objectMapper, Clock.systemUTC());
    }

    OpenAiProxyService(
            OpenAiGateway openAiGateway,
            OpenAiProperties openAiProperties,
            OpenAiUsageParser usageParser,
            PricingService pricingService,
            UsageEventRepository usageEventRepository,
            ProxyCaptureProvider proxyCaptureProvider,
            WorkTrackingProvider workTrackingProvider,
            AttributionInferenceService attributionInferenceService,
            AttributionStatusService attributionStatusService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.openAiGateway = openAiGateway;
        this.openAiProperties = openAiProperties;
        this.usageParser = usageParser;
        this.pricingService = pricingService;
        this.usageEventRepository = usageEventRepository;
        this.proxyCaptureProvider = proxyCaptureProvider;
        this.workTrackingProvider = workTrackingProvider;
        this.attributionInferenceService = attributionInferenceService;
        this.attributionStatusService = attributionStatusService;
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
        ProxyAttribution attribution = envelope.attribution();
        AttributionInference inference = attributionInferenceService.infer(attribution.storyKey(), attribution.branch());
        WorkItem story = findStory(inference.storyKey()).orElse(null);
        AttributionStatus attributionStatus = attributionStatusService.classify(inference.storyKey());
        UsageCaptureMetadata captureMetadata = proxyCaptureProvider.metadata();

        usageEventRepository.save(new UsageEvent(
                UUID.randomUUID(),
                openAiProperties.provider(),
                model,
                inference.storyKey(),
                story == null ? null : story.epicKey(),
                attribution.teamKey(),
                attribution.userKey(),
                usage.promptTokens(),
                usage.completionTokens(),
                usage.totalTokens(),
                estimatedCostUsd,
                latencyMs,
                requestTimestamp,
                "local",
                story == null ? "UNKNOWN" : story.workType(),
                upstreamResponse.statusCode().is2xxSuccessful() ? "SUCCEEDED" : "FAILED",
                attributionStatus,
                requestHash,
                captureMetadata.source(),
                captureMetadata.provider(),
                captureMetadata.method(),
                captureMetadata.confidence(),
                inference.source(),
                inference.confidence(),
                inference.source() == AttributionSource.INFERRED_BRANCH ? inference.storyKey() : null,
                inference.reason(),
                normalize(attribution.repository()),
                normalize(attribution.branch()),
                normalize(attribution.commitHash()),
                null,
                null,
                false,
                null,
                null
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

    private Optional<WorkItem> findStory(String storyKey) {
        try {
            return workTrackingProvider.findStoryByKey(storyKey);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
