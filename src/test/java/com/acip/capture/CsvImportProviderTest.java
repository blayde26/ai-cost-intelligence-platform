package com.acip.capture;

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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CsvImportProviderTest {

    private final UsageEventRepository usageEventRepository = mock(UsageEventRepository.class);
    private final PricingService pricingService = mock(PricingService.class);
    private final WorkTrackingProvider workTrackingProvider = mock(WorkTrackingProvider.class);
    private final AttributionInferenceService attributionInferenceService = new AttributionInferenceService(new BranchStoryKeyParser());
    private final AttributionStatusService attributionStatusService = new AttributionStatusService(workTrackingProvider);
    private final CsvImportProvider provider = new CsvImportProvider(
            usageEventRepository,
            pricingService,
            workTrackingProvider,
            attributionInferenceService,
            attributionStatusService
    );

    @Test
    void importsCsvRowsIntoCanonicalUsageEvents() {
        when(workTrackingProvider.findStoryByKey("PAY-1001"))
                .thenReturn(Optional.of(new WorkItem("PAY-1001", WorkItemType.STORY, "Checkout", "In Progress", "payments", "PAY-1000", "CAPITALIZED")));
        when(workTrackingProvider.findEpicByKey("PAY-1000"))
                .thenReturn(Optional.of(new WorkItem("PAY-1000", WorkItemType.EPIC, "Checkout Modernization", "In Progress", "payments", null, "UNKNOWN")));
        String csv = """
                provider,model,storyKey,teamKey,userKey,promptTokens,completionTokens,totalTokens,estimatedCostUsd,requestTimestamp,repository,branch
                OLLAMA,llama3.2,PAY-1001,payments,brian,10,5,15,0.00000120,2026-05-31T12:00:00Z,ai-cost-intelligence-platform,feature/PAY-1001-test
                """;

        UsageImportResult result = provider.importCsv(csv);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        ArgumentCaptor<UsageEvent> eventCaptor = ArgumentCaptor.forClass(UsageEvent.class);
        verify(usageEventRepository).save(eventCaptor.capture());
        UsageEvent event = eventCaptor.getValue();
        assertThat(event.captureSource()).isEqualTo(UsageCaptureSource.CSV_IMPORT);
        assertThat(event.captureProvider()).isEqualTo("MANUAL_CSV_IMPORT");
        assertThat(event.captureMethod()).isEqualTo(UsageCaptureMethod.FILE_IMPORT);
        assertThat(event.captureConfidence()).isEqualTo(UsageCaptureConfidence.MEDIUM);
        assertThat(event.attributionSource()).isEqualTo(AttributionSource.EXPLICIT);
        assertThat(event.attributionConfidence()).isEqualTo(AttributionConfidence.HIGH);
        assertThat(event.attributionStatus()).isEqualTo(AttributionStatus.VALID);
        assertThat(event.epicKey()).isEqualTo("PAY-1000");
        assertThat(event.workType()).isEqualTo("CAPITALIZED");
        assertThat(event.estimatedCostUsd()).isEqualByComparingTo(new BigDecimal("0.00000120"));
    }

    @Test
    void importsBranchInferredStoryKeys() {
        when(workTrackingProvider.findStoryByKey("PAY-1002"))
                .thenReturn(Optional.of(new WorkItem("PAY-1002", WorkItemType.STORY, "Retry", "In Progress", "payments", "PAY-1000", "CAPITALIZED")));
        when(workTrackingProvider.findEpicByKey("PAY-1000"))
                .thenReturn(Optional.of(new WorkItem("PAY-1000", WorkItemType.EPIC, "Checkout Modernization", "In Progress", "payments", null, "UNKNOWN")));
        String csv = """
                provider,model,teamKey,userKey,totalTokens,estimatedCostUsd,requestTimestamp,branch
                OLLAMA,llama3.2,payments,brian,20,0.00000160,2026-05-31T12:00:00Z,feature/pay-1002-retry
                """;

        UsageImportResult result = provider.importCsv(csv);

        assertThat(result.importedCount()).isEqualTo(1);
        ArgumentCaptor<UsageEvent> eventCaptor = ArgumentCaptor.forClass(UsageEvent.class);
        verify(usageEventRepository).save(eventCaptor.capture());
        UsageEvent event = eventCaptor.getValue();
        assertThat(event.storyKey()).isEqualTo("PAY-1002");
        assertThat(event.attributionSource()).isEqualTo(AttributionSource.INFERRED_BRANCH);
        assertThat(event.inferredStoryKey()).isEqualTo("PAY-1002");
    }

    @Test
    void returnsRowErrorsWithoutRejectingTheWholeImport() {
        String csv = """
                provider,model,teamKey,userKey,totalTokens,estimatedCostUsd,requestTimestamp
                OLLAMA,llama3.2,platform,brian,20,0.00000160,2026-05-31T12:00:00Z
                ,llama3.2,platform,brian,20,0.00000160,2026-05-31T12:00:00Z
                """;

        UsageImportResult result = provider.importCsv(csv);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.errors()).singleElement()
                .satisfies(error -> {
                    assertThat(error.rowNumber()).isEqualTo(3);
                    assertThat(error.message()).contains("provider");
                });
    }

    @Test
    void previewsCsvRowsWithoutPersistingUsageEvents() {
        when(workTrackingProvider.findStoryByKey("PAY-1001"))
                .thenReturn(Optional.of(new WorkItem("PAY-1001", WorkItemType.STORY, "Checkout", "In Progress", "payments", "PAY-1000", "CAPITALIZED")));
        String csv = """
                provider,model,storyKey,teamKey,userKey,totalTokens,estimatedCostUsd,requestTimestamp
                OLLAMA,llama3.2,PAY-1001,payments,brian,4200,0.00033600,2026-05-31T12:00:00Z
                """;

        UsageImportResult result = provider.previewCsv(csv);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        org.mockito.Mockito.verifyNoInteractions(usageEventRepository);
    }
}
