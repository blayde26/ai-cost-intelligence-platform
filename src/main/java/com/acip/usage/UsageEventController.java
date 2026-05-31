package com.acip.usage;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class UsageEventController {

    private final UsageEventRepository usageEventRepository;

    public UsageEventController(UsageEventRepository usageEventRepository) {
        this.usageEventRepository = usageEventRepository;
    }

    @GetMapping("/api/v1/usage/events")
    public List<UsageEvent> recentEvents(@RequestParam(defaultValue = "100") int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 500));
        return usageEventRepository.findRecent(boundedLimit);
    }

    @GetMapping("/api/v1/usage/events/{id}")
    public UsageEvent eventById(@PathVariable UUID id) {
        return usageEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usage event not found."));
    }
}
