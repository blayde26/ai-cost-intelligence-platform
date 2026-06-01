package com.acip.reports;

import com.acip.common.CsvExport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping(value = "/api/v1/reports/spend/by-story.csv", produces = "text/csv")
    public ResponseEntity<String> spendByStoryCsv(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        List<SpendByStoryReport> rows = spendReportRepository.spendByStory(ReportDateRange.parse(startDate, endDate));
        String csv = CsvExport.render(
                List.of("storyKey", "storyName", "epicKey", "teamKey", "requestCount", "totalTokens", "estimatedCostUsd"),
                rows.stream()
                        .map(row -> List.of(
                                nullable(row.storyKey()),
                                nullable(row.storyName()),
                                nullable(row.epicKey()),
                                nullable(row.teamKey()),
                                row.requestCount(),
                                row.totalTokens(),
                                row.estimatedCostUsd()
                        ))
                        .toList()
        );
        return csv("acip-spend-by-story.csv", csv);
    }

    @GetMapping("/api/v1/reports/spend/by-epic")
    public List<SpendByEpicReport> spendByEpic(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return spendReportRepository.spendByEpic(ReportDateRange.parse(startDate, endDate));
    }

    @GetMapping(value = "/api/v1/reports/spend/by-epic.csv", produces = "text/csv")
    public ResponseEntity<String> spendByEpicCsv(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        List<SpendByEpicReport> rows = spendReportRepository.spendByEpic(ReportDateRange.parse(startDate, endDate));
        String csv = CsvExport.render(
                List.of("epicKey", "epicName", "teamKey", "storyCount", "requestCount", "totalTokens", "estimatedCostUsd"),
                rows.stream()
                        .map(row -> List.of(
                                nullable(row.epicKey()),
                                nullable(row.epicName()),
                                nullable(row.teamKey()),
                                row.storyCount(),
                                row.requestCount(),
                                row.totalTokens(),
                                row.estimatedCostUsd()
                        ))
                        .toList()
        );
        return csv("acip-spend-by-epic.csv", csv);
    }

    @GetMapping("/api/v1/reports/spend/by-team")
    public List<SpendByTeamReport> spendByTeam(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return spendReportRepository.spendByTeam(ReportDateRange.parse(startDate, endDate));
    }

    @GetMapping(value = "/api/v1/reports/spend/by-team.csv", produces = "text/csv")
    public ResponseEntity<String> spendByTeamCsv(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        List<SpendByTeamReport> rows = spendReportRepository.spendByTeam(ReportDateRange.parse(startDate, endDate));
        String csv = CsvExport.render(
                List.of("teamKey", "epicCount", "storyCount", "requestCount", "totalTokens", "estimatedCostUsd"),
                rows.stream()
                        .map(row -> List.of(
                                nullable(row.teamKey()),
                                row.epicCount(),
                                row.storyCount(),
                                row.requestCount(),
                                row.totalTokens(),
                                row.estimatedCostUsd()
                        ))
                        .toList()
        );
        return csv("acip-spend-by-team.csv", csv);
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

    private ResponseEntity<String> csv(String filename, String csv) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }

    private String nullable(String value) {
        return value == null ? "" : value;
    }
}
