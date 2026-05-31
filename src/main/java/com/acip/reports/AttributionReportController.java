package com.acip.reports;

import com.acip.usage.AttributionStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AttributionReportController {

    private final AttributionCoverageService attributionCoverageService;
    private final AttributionReportRepository attributionReportRepository;

    public AttributionReportController(
            AttributionCoverageService attributionCoverageService,
            AttributionReportRepository attributionReportRepository
    ) {
        this.attributionCoverageService = attributionCoverageService;
        this.attributionReportRepository = attributionReportRepository;
    }

    @GetMapping("/api/v1/reports/attribution-coverage")
    public AttributionCoverageReport coverage(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return attributionCoverageService.coverage(ReportDateRange.parse(startDate, endDate));
    }

    @GetMapping("/api/v1/reports/unattributed")
    public List<UnattributedSpendEvent> unattributed(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String teamKey,
            @RequestParam(required = false) String userKey,
            @RequestParam(required = false) AttributionStatus attributionStatus,
            @RequestParam(defaultValue = "cost_desc") String sort
    ) {
        ReportDateRange range = ReportDateRange.parse(firstNonBlank(startDate, fromDate), firstNonBlank(endDate, toDate));
        return attributionReportRepository.unattributedEvents(new UnattributedSpendFilter(range.startDate(), range.endDateExclusive(), teamKey, userKey, attributionStatus, sort));
    }

    @GetMapping("/api/v1/reports/unattributed/summary")
    public UnattributedSpendSummary unattributedSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String teamKey,
            @RequestParam(required = false) String userKey,
            @RequestParam(required = false) AttributionStatus attributionStatus
    ) {
        ReportDateRange range = ReportDateRange.parse(firstNonBlank(startDate, fromDate), firstNonBlank(endDate, toDate));
        return attributionReportRepository.unattributedSummary(new UnattributedSpendFilter(range.startDate(), range.endDateExclusive(), teamKey, userKey, attributionStatus, "cost_desc"));
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }
}
