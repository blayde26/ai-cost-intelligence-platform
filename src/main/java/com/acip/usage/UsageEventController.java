package com.acip.usage;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
