package com.acip.reports;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttributionCoverageServiceTest {

    private final AttributionReportRepository repository = mock(AttributionReportRepository.class);
    private final AttributionCoverageService service = new AttributionCoverageService(repository);

    @Test
    void allValidEventsReturnOneHundredPercentCoverage() {
        when(repository.coverage()).thenReturn(raw("10.00", "10.00", "0.00", 2, 2, 0));

        assertThat(service.coverage().coveragePercent()).isEqualTo(100.0);
    }

    @Test
    void allInvalidEventsReturnZeroPercentCoverage() {
        when(repository.coverage()).thenReturn(raw("10.00", "0.00", "10.00", 2, 0, 2));

        assertThat(service.coverage().coveragePercent()).isEqualTo(0.0);
    }

    @Test
    void mixedEventsReturnCostBasedCoverage() {
        when(repository.coverage()).thenReturn(raw("100.00", "92.50", "7.50", 10, 9, 1));

        assertThat(service.coverage().coveragePercent()).isEqualTo(92.5);
    }

    @Test
    void zeroEventsReturnZeroCoverage() {
        when(repository.coverage()).thenReturn(raw("0.00", "0.00", "0.00", 0, 0, 0));

        AttributionCoverageReport report = service.coverage();

        assertThat(report.coveragePercent()).isEqualTo(0.0);
        assertThat(report.eventCount()).isZero();
    }

    private AttributionCoverageReport raw(
            String totalCost,
            String attributedCost,
            String unattributedCost,
            long eventCount,
            long validEventCount,
            long invalidEventCount
    ) {
        return new AttributionCoverageReport(
                new BigDecimal(totalCost),
                new BigDecimal(attributedCost),
                new BigDecimal(unattributedCost),
                0.0,
                eventCount,
                validEventCount,
                invalidEventCount
        );
    }
}
