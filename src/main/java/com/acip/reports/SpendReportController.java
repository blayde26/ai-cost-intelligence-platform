package com.acip.reports;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SpendReportController {

    private final SpendReportRepository spendReportRepository;

    public SpendReportController(SpendReportRepository spendReportRepository) {
        this.spendReportRepository = spendReportRepository;
    }

    @GetMapping("/api/v1/reports/overview")
    public SpendOverviewReport overview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return spendReportRepository.overview(ReportDateRange.parse(startDate, endDate));
    }

    @GetMapping("/api/v1/reports/spend/by-story")
    public List<SpendByStoryReport> spendByStory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return spendReportRepository.spendByStory(ReportDateRange.parse(startDate, endDate));
    }

    @GetMapping("/api/v1/reports/spend/by-epic")
    public List<SpendByEpicReport> spendByEpic(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return spendReportRepository.spendByEpic(ReportDateRange.parse(startDate, endDate));
    }

    @GetMapping("/api/v1/reports/spend/by-team")
    public List<SpendByTeamReport> spendByTeam(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return spendReportRepository.spendByTeam(ReportDateRange.parse(startDate, endDate));
    }

    @GetMapping("/api/v1/reports/potential-waste")
    public PotentialWasteReport potentialWaste(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return spendReportRepository.potentialWaste(ReportDateRange.parse(startDate, endDate));
    }

    @GetMapping("/api/v1/reports/allocation")
    public SpendAllocationReport allocation(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return spendReportRepository.allocation(ReportDateRange.parse(startDate, endDate));
    }
}
