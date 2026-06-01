package com.acip.reports;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpendReportControllerTest {

    private final SpendReportRepository repository = mock(SpendReportRepository.class);
    private final SpendReportController controller = new SpendReportController(repository);

    @Test
    void exportsSpendByStoryCsv() {
        when(repository.spendByStory(ReportDateRange.parse(null, null))).thenReturn(List.of(
                new SpendByStoryReport("PAY-1002", "Retry payments, safely", "PAY-1000", "payments", new BigDecimal("12.34"), 4200, 3)
        ));

        ResponseEntity<String> response = controller.spendByStoryCsv(null, null);

        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("acip-spend-by-story.csv");
        assertThat(response.getBody()).contains("storyKey,storyName,epicKey,teamKey,requestCount,totalTokens,estimatedCostUsd");
        assertThat(response.getBody()).contains("PAY-1002,\"Retry payments, safely\",PAY-1000,payments,3,4200,12.34");
    }

    @Test
    void exportsSpendByTeamCsv() {
        when(repository.spendByTeam(ReportDateRange.parse(null, null))).thenReturn(List.of(
                new SpendByTeamReport("platform", new BigDecimal("99.01"), 1000, 5, 2, 4)
        ));

        ResponseEntity<String> response = controller.spendByTeamCsv(null, null);

        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("acip-spend-by-team.csv");
        assertThat(response.getBody()).contains("platform,2,4,5,1000,99.01");
    }
}
