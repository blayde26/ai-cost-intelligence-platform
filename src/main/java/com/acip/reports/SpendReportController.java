package com.acip.reports;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SpendReportController {

    private final SpendReportRepository spendReportRepository;

    public SpendReportController(SpendReportRepository spendReportRepository) {
        this.spendReportRepository = spendReportRepository;
    }

    @GetMapping("/api/v1/reports/spend/by-story")
    public List<SpendByStoryReport> spendByStory() {
        return spendReportRepository.spendByStory();
    }

    @GetMapping("/api/v1/reports/spend/by-epic")
    public List<SpendByEpicReport> spendByEpic() {
        return spendReportRepository.spendByEpic();
    }
}
