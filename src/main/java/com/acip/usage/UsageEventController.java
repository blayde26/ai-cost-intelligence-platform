package com.acip.usage;

import com.acip.common.CsvExport;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class UsageEventController {

    private final UsageEventRepository usageEventRepository;
    private final AttributionCorrectionService attributionCorrectionService;

    public UsageEventController(UsageEventRepository usageEventRepository, AttributionCorrectionService attributionCorrectionService) {
        this.usageEventRepository = usageEventRepository;
        this.attributionCorrectionService = attributionCorrectionService;
    }

    @GetMapping("/api/v1/usage/events")
    public List<UsageEvent> recentEvents(@RequestParam(defaultValue = "100") int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 500));
        return usageEventRepository.findRecent(boundedLimit);
    }

    @GetMapping(value = "/api/v1/usage/events.csv", produces = "text/csv")
    public ResponseEntity<String> recentEventsCsv(@RequestParam(defaultValue = "100") int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 500));
        List<UsageEvent> events = usageEventRepository.findRecent(boundedLimit);
        String csv = CsvExport.render(
                List.of(
                        "id",
                        "provider",
                        "model",
                        "storyKey",
                        "epicKey",
                        "teamKey",
                        "userKey",
                        "totalTokens",
                        "estimatedCostUsd",
                        "requestTimestamp",
                        "requestStatus",
                        "attributionStatus",
                        "captureSource",
                        "attributionSource",
                        "repository",
                        "branch"
                ),
                events.stream()
                        .map(event -> List.of(
                                event.id(),
                                nullable(event.provider()),
                                nullable(event.model()),
                                nullable(event.storyKey()),
                                nullable(event.epicKey()),
                                nullable(event.teamKey()),
                                nullable(event.userKey()),
                                event.totalTokens(),
                                event.estimatedCostUsd(),
                                event.requestTimestamp(),
                                nullable(event.requestStatus()),
                                event.attributionStatus(),
                                event.captureSource(),
                                event.attributionSource(),
                                nullable(event.repository()),
                                nullable(event.branch())
                        ))
                        .toList()
        );
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"acip-usage-events.csv\"")
                .body(csv);
    }

    @GetMapping("/api/v1/usage/events/{id}")
    public UsageEvent eventById(@PathVariable UUID id) {
        return usageEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usage event not found."));
    }

    @PatchMapping("/api/v1/usage/events/{id}/attribution")
    public UsageEvent correctAttribution(
            @PathVariable UUID id,
            @Valid @RequestBody AttributionCorrectionRequest request
    ) {
        return attributionCorrectionService.correct(id, request);
    }

    private String nullable(String value) {
        return value == null ? "" : value;
    }
}
