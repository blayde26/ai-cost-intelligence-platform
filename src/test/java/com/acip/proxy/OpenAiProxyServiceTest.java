package com.acip.proxy;

import com.acip.jira.Story;
import com.acip.jira.StoryRepository;
import com.acip.pricing.PricingService;
import com.acip.usage.UsageEvent;
import com.acip.usage.UsageEventRepository;
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
    private final StoryRepository storyRepository = mock(StoryRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAiProxyService proxyService = new OpenAiProxyService(
            gateway,
            properties,
            usageParser,
            pricingService,
            usageEventRepository,
            storyRepository,
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
        when(storyRepository.findByStoryKey("ACIP-123"))
                .thenReturn(Optional.of(new Story("ACIP-123", "Build proxy", "In Progress", "ACIP-1", "CAPITALIZED")));

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
        assertThat(event.requestHash()).hasSize(64);
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
}
