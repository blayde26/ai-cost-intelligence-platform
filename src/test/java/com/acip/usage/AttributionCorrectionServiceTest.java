package com.acip.usage;

import com.acip.jira.Story;
import com.acip.jira.StoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttributionCorrectionServiceTest {

    private final UsageEventRepository usageEventRepository = mock(UsageEventRepository.class);
    private final StoryRepository storyRepository = mock(StoryRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private final AttributionCorrectionService service = new AttributionCorrectionService(usageEventRepository, storyRepository, clock);

    @Test
    void correctsKnownStoryAsManualAndDerivesEpicAndWorkType() {
        UUID eventId = UUID.randomUUID();
        UsageEvent original = event(eventId);
        UsageEvent corrected = new UsageEvent(
                eventId,
                original.provider(),
                original.model(),
                "PAY-1001",
                "PAY-1000",
                "payments",
                original.userKey(),
                original.promptTokens(),
                original.completionTokens(),
                original.totalTokens(),
                original.estimatedCostUsd(),
                original.latencyMs(),
                original.requestTimestamp(),
                original.environment(),
                "CAPITALIZED",
                original.requestStatus(),
                AttributionStatus.MANUAL,
                original.requestHash(),
                null,
                null,
                null,
                null,
                null,
                true,
                OffsetDateTime.now(clock),
                "brian"
        );
        when(usageEventRepository.findById(eventId)).thenReturn(Optional.of(original), Optional.of(corrected));
        when(storyRepository.findByStoryKey("PAY-1001"))
                .thenReturn(Optional.of(new Story("PAY-1001", "Checkout", "Done", "PAY-1000", "CAPITALIZED")));

        UsageEvent result = service.correct(eventId, new AttributionCorrectionRequest("PAY-1001", null, "payments", null, "brian", "fixed"));

        assertThat(result).isSameAs(corrected);
        ArgumentCaptor<AttributionCorrection> correctionCaptor = ArgumentCaptor.forClass(AttributionCorrection.class);
        verify(usageEventRepository).applyCorrection(org.mockito.ArgumentMatchers.eq(original), correctionCaptor.capture());
        AttributionCorrection correction = correctionCaptor.getValue();
        assertThat(correction.storyKey()).isEqualTo("PAY-1001");
        assertThat(correction.epicKey()).isEqualTo("PAY-1000");
        assertThat(correction.workType()).isEqualTo("CAPITALIZED");
        assertThat(correction.attributionStatus()).isEqualTo(AttributionStatus.MANUAL);
        assertThat(correction.correctedTimestamp()).isEqualTo(OffsetDateTime.now(clock));
    }

    @Test
    void rejectsCorrectionsWithoutStoryOrTeam() {
        UUID eventId = UUID.randomUUID();
        when(usageEventRepository.findById(eventId)).thenReturn(Optional.of(event(eventId)));

        assertThatThrownBy(() -> service.correct(eventId, new AttributionCorrectionRequest(null, null, null, null, "brian", null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private UsageEvent event(UUID id) {
        return new UsageEvent(
                id,
                "MOCK_LLM",
                "mock-gpt-4o-mini",
                null,
                null,
                "platform",
                "brian",
                10,
                20,
                30,
                new BigDecimal("0.01"),
                100,
                OffsetDateTime.of(2026, 6, 1, 11, 0, 0, 0, ZoneOffset.UTC),
                "test",
                "UNKNOWN",
                "SUCCEEDED",
                AttributionStatus.MISSING_STORY_KEY,
                "a".repeat(64)
        );
    }
}
