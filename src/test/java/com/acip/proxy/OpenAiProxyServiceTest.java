package com.acip.proxy;

import com.acip.capture.ProxyCaptureProvider;
import com.acip.capture.UsageCaptureConfidence;
import com.acip.capture.UsageCaptureMethod;
import com.acip.capture.UsageCaptureSource;
import com.acip.pricing.PricingService;
import com.acip.usage.AttributionConfidence;
import com.acip.usage.AttributionInferenceService;
import com.acip.usage.AttributionSource;
import com.acip.usage.AttributionStatus;
import com.acip.usage.AttributionStatusService;
import com.acip.usage.BranchStoryKeyParser;
import com.acip.usage.UsageEvent;
import com.acip.usage.UsageEventRepository;
import com.acip.worktracking.WorkItem;
import com.acip.worktracking.WorkItemType;
import com.acip.worktracking.WorkTrackingProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAiProxyServiceTest {

    private final OpenAiGateway gateway = mock(OpenAiGateway.class);
    private final OpenAiProperties properties = new OpenAiProperties(
            "MOCK_LLM",
            "test-key",
            true,
            "http://localhost/openai/chat/completions",
            java.time.Duration.ofSeconds(5),
            java.time.Duration.ofSeconds(60)
    );
    private final OpenAiUsageParser usageParser = mock(OpenAiUsageParser.class);
    private final PricingService pricingService = mock(PricingService.class);
    private final UsageEventRepository usageEventRepository = mock(UsageEventRepository.class);
    private final ProxyCaptureProvider proxyCaptureProvider = new ProxyCaptureProvider();
    private final WorkTrackingProvider workTrackingProvider = mock(WorkTrackingProvider.class);
    private final AttributionInferenceService attributionInferenceService = new AttributionInferenceService(new BranchStoryKeyParser());
    private final AttributionStatusService attributionStatusService = new AttributionStatusService(workTrackingProvider);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAiProxyService proxyService = new OpenAiProxyService(
            gateway,
            properties,
            usageParser,
            pricingService,
            usageEventRepository,
            proxyCaptureProvider,
            workTrackingProvider,
            attributionInferenceService,
            attributionStatusService,
            objectMapper,
            Clock.fixed(Instant.parse("2026-05-29T12:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void proxiesSuccessfulResponseAndPersistsUsageEvent() throws Exception {
        JsonNode request = objectMapper.readTree("""
                {"model":"gpt-4o-mini","messages":[{"role":"user","content":"hello"}]}
                """);
        OpenAiProxyEnvelope envelope = new OpenAiProxyEnvelope(
                new ProxyAttribution("ACIP-123", "PLATFORM", "brian"),
                request
        );
        String responseBody = """
                {"id":"chatcmpl_123","usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}
                """;

        when(gateway.postChatCompletions(request))
                .thenReturn(new UpstreamResponse(HttpStatus.OK, responseBody, new HttpHeaders()));
        when(usageParser.parse(responseBody)).thenReturn(new OpenAiTokenUsage(10, 5, 15));
        when(pricingService.estimateCostUsd("MOCK_LLM", "gpt-4o-mini", 10, 5))
                .thenReturn(new BigDecimal("0.00000450"));
        when(workTrackingProvider.findStoryByKey("ACIP-123"))
                .thenReturn(Optional.of(new WorkItem("ACIP-123", WorkItemType.STORY, "Build proxy", "In Progress", "PLATFORM", "ACIP-1", "CAPITALIZED")));
        when(workTrackingProvider.findEpicByKey("ACIP-1"))
                .thenReturn(Optional.of(new WorkItem("ACIP-1", WorkItemType.EPIC, "Cost visibility", "In Progress", "PLATFORM", null, "UNKNOWN")));

        var response = proxyService.proxyChatCompletions(envelope);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseBody);

        ArgumentCaptor<UsageEvent> eventCaptor = ArgumentCaptor.forClass(UsageEvent.class);
        verify(usageEventRepository).save(eventCaptor.capture());
        UsageEvent event = eventCaptor.getValue();
        assertThat(event.storyKey()).isEqualTo("ACIP-123");
        assertThat(event.epicKey()).isEqualTo("ACIP-1");
        assertThat(event.teamKey()).isEqualTo("PLATFORM");
        assertThat(event.userKey()).isEqualTo("brian");
        assertThat(event.model()).isEqualTo("gpt-4o-mini");
        assertThat(event.provider()).isEqualTo("MOCK_LLM");
        assertThat(event.promptTokens()).isEqualTo(10);
        assertThat(event.completionTokens()).isEqualTo(5);
        assertThat(event.totalTokens()).isEqualTo(15);
        assertThat(event.estimatedCostUsd()).isEqualByComparingTo("0.00000450");
        assertThat(event.requestStatus()).isEqualTo("SUCCEEDED");
        assertThat(event.workType()).isEqualTo("CAPITALIZED");
        assertThat(event.captureSource()).isEqualTo(UsageCaptureSource.PROXY);
        assertThat(event.captureProvider()).isEqualTo("OPENAI_COMPATIBLE_PROXY");
        assertThat(event.captureMethod()).isEqualTo(UsageCaptureMethod.REAL_TIME_PROXY);
        assertThat(event.captureConfidence()).isEqualTo(UsageCaptureConfidence.HIGH);
        assertThat(event.attributionStatus()).isEqualTo(AttributionStatus.VALID);
        assertThat(event.attributionSource()).isEqualTo(AttributionSource.EXPLICIT);
        assertThat(event.attributionConfidence()).isEqualTo(AttributionConfidence.HIGH);
        assertThat(event.requestHash()).hasSize(64);
    }

    @Test
    void infersStoryKeyFromBranchWhenStoryKeyIsMissing() throws Exception {
        JsonNode request = objectMapper.readTree("""
                {"model":"gpt-4o-mini","messages":[{"role":"user","content":"hello"}]}
                """);
        OpenAiProxyEnvelope envelope = new OpenAiProxyEnvelope(
                new ProxyAttribution(null, "payments", "brian", "ai-cost-intelligence-platform", "feature/pay-1001-checkout", "abc123"),
                request
        );
        String responseBody = """
                {"id":"chatcmpl_123","usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}
                """;

        when(gateway.postChatCompletions(request))
                .thenReturn(new UpstreamResponse(HttpStatus.OK, responseBody, new HttpHeaders()));
        when(usageParser.parse(responseBody)).thenReturn(new OpenAiTokenUsage(10, 5, 15));
        when(pricingService.estimateCostUsd("MOCK_LLM", "gpt-4o-mini", 10, 5))
                .thenReturn(new BigDecimal("0.00000450"));
        when(workTrackingProvider.findStoryByKey("PAY-1001"))
                .thenReturn(Optional.of(new WorkItem("PAY-1001", WorkItemType.STORY, "Checkout", "In Progress", "payments", "PAY-1000", "CAPITALIZED")));
        when(workTrackingProvider.findEpicByKey("PAY-1000"))
                .thenReturn(Optional.of(new WorkItem("PAY-1000", WorkItemType.EPIC, "Checkout modernization", "In Progress", "payments", null, "UNKNOWN")));

        proxyService.proxyChatCompletions(envelope);

        ArgumentCaptor<UsageEvent> eventCaptor = ArgumentCaptor.forClass(UsageEvent.class);
        verify(usageEventRepository).save(eventCaptor.capture());
        UsageEvent event = eventCaptor.getValue();
        assertThat(event.storyKey()).isEqualTo("PAY-1001");
        assertThat(event.epicKey()).isEqualTo("PAY-1000");
        assertThat(event.repository()).isEqualTo("ai-cost-intelligence-platform");
        assertThat(event.branch()).isEqualTo("feature/pay-1001-checkout");
        assertThat(event.commitHash()).isEqualTo("abc123");
        assertThat(event.attributionSource()).isEqualTo(AttributionSource.INFERRED_BRANCH);
        assertThat(event.attributionConfidence()).isEqualTo(AttributionConfidence.HIGH);
        assertThat(event.inferredStoryKey()).isEqualTo("PAY-1001");
        assertThat(event.inferenceReason()).contains("branch");
        assertThat(event.attributionStatus()).isEqualTo(AttributionStatus.VALID);
    }

    @Test
    void rejectsRequestsWithoutModel() throws Exception {
        JsonNode request = objectMapper.readTree("""
                {"messages":[{"role":"user","content":"hello"}]}
                """);
        OpenAiProxyEnvelope envelope = new OpenAiProxyEnvelope(
                new ProxyAttribution("ACIP-123", "PLATFORM", "brian"),
                request
        );

        assertThatThrownBy(() -> proxyService.proxyChatCompletions(envelope))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("request.model is required");
    }

    @Test
    void workTrackingFailureDoesNotBlockSuccessfulProxyResponse() throws Exception {
        JsonNode request = objectMapper.readTree("""
                {"model":"gpt-4o-mini","messages":[{"role":"user","content":"hello"}]}
                """);
        OpenAiProxyEnvelope envelope = new OpenAiProxyEnvelope(
                new ProxyAttribution("ACIP-404", "PLATFORM", "brian"),
                request
        );
        String responseBody = """
                {"id":"chatcmpl_123","usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}
                """;

        when(gateway.postChatCompletions(request))
                .thenReturn(new UpstreamResponse(HttpStatus.OK, responseBody, new HttpHeaders()));
        when(usageParser.parse(responseBody)).thenReturn(new OpenAiTokenUsage(10, 5, 15));
        when(pricingService.estimateCostUsd("MOCK_LLM", "gpt-4o-mini", 10, 5))
                .thenReturn(new BigDecimal("0.00000450"));
        when(workTrackingProvider.findStoryByKey("ACIP-404"))
                .thenThrow(new IllegalStateException("Jira unavailable"));

        var response = proxyService.proxyChatCompletions(envelope);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ArgumentCaptor<UsageEvent> eventCaptor = ArgumentCaptor.forClass(UsageEvent.class);
        verify(usageEventRepository).save(eventCaptor.capture());
        UsageEvent event = eventCaptor.getValue();
        assertThat(event.attributionStatus()).isEqualTo(AttributionStatus.UNKNOWN_STORY);
        assertThat(event.workType()).isEqualTo("UNKNOWN");
    }
}
