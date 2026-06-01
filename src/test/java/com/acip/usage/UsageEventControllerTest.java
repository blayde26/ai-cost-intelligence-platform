package com.acip.usage;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsageEventControllerTest {

    private final UsageEventRepository repository = mock(UsageEventRepository.class);
    private final AttributionCorrectionService correctionService = mock(AttributionCorrectionService.class);
    private final UsageEventController controller = new UsageEventController(repository, correctionService);

    @Test
    void returnsEventByIdWhenPresent() {
        UUID id = UUID.randomUUID();
        UsageEvent event = mock(UsageEvent.class);
        when(repository.findById(id)).thenReturn(Optional.of(event));

        assertThat(controller.eventById(id)).isSameAs(event);
    }

    @Test
    void throwsNotFoundWhenEventIsMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.eventById(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delegatesAttributionCorrection() {
        UUID id = UUID.randomUUID();
        AttributionCorrectionRequest request = new AttributionCorrectionRequest("PAY-1001", null, "payments", null, "brian", "fixed");
        UsageEvent corrected = mock(UsageEvent.class);
        when(correctionService.correct(id, request)).thenReturn(corrected);

        assertThat(controller.correctAttribution(id, request)).isSameAs(corrected);
        verify(correctionService).correct(id, request);
    }

    @Test
    void exportsRecentEventsCsv() {
        UUID id = UUID.randomUUID();
        UsageEvent event = new UsageEvent(
                id,
                "MOCK_LLM",
                "mock-gpt-4o-mini",
                "PAY-1002",
                "PAY-1000",
                "payments",
                "brian",
                10,
                20,
                30,
                new BigDecimal("0.42"),
                25,
                OffsetDateTime.of(2026, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                "local",
                "CAPITALIZED",
                "SUCCEEDED",
                AttributionStatus.VALID,
                "a".repeat(64)
        );
        when(repository.findRecent(5)).thenReturn(List.of(event));

        ResponseEntity<String> response = controller.recentEventsCsv(5);

        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("acip-usage-events.csv");
        assertThat(response.getBody()).contains("id,provider,model,storyKey,epicKey,teamKey,userKey,totalTokens");
        assertThat(response.getBody()).contains(id + ",MOCK_LLM,mock-gpt-4o-mini,PAY-1002,PAY-1000,payments,brian,30");
    }
}
