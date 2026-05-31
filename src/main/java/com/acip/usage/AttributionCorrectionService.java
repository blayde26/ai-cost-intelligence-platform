package com.acip.usage;

import com.acip.jira.Story;
import com.acip.jira.StoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AttributionCorrectionService {

    private final UsageEventRepository usageEventRepository;
    private final StoryRepository storyRepository;
    private final Clock clock;

    @Autowired
    public AttributionCorrectionService(UsageEventRepository usageEventRepository, StoryRepository storyRepository) {
        this(usageEventRepository, storyRepository, Clock.systemUTC());
    }

    AttributionCorrectionService(UsageEventRepository usageEventRepository, StoryRepository storyRepository, Clock clock) {
        this.usageEventRepository = usageEventRepository;
        this.storyRepository = storyRepository;
        this.clock = clock;
    }

    public UsageEvent correct(UUID eventId, AttributionCorrectionRequest request) {
        UsageEvent originalEvent = usageEventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usage event not found."));
        String storyKey = normalize(request.storyKey());
        String teamKey = normalize(request.teamKey());
        String explicitEpicKey = normalize(request.epicKey());
        String explicitWorkType = normalize(request.workType());
        if (storyKey == null && teamKey == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Either storyKey or teamKey is required for manual attribution.");
        }

        Optional<Story> knownStory = storyKey == null ? Optional.empty() : storyRepository.findByStoryKey(storyKey);
        String correctedEpicKey = knownStory.map(Story::epicKey).orElse(explicitEpicKey);
        String correctedWorkType = knownStory.map(Story::workType).orElse(explicitWorkType == null ? "UNKNOWN" : explicitWorkType);
        String correctedTeamKey = teamKey == null ? originalEvent.teamKey() : teamKey;

        AttributionCorrection correction = new AttributionCorrection(
                storyKey,
                correctedEpicKey,
                correctedTeamKey,
                correctedWorkType,
                AttributionStatus.MANUAL,
                request.correctedBy().trim(),
                OffsetDateTime.now(clock),
                normalize(request.note())
        );
        usageEventRepository.applyCorrection(originalEvent, correction);
        return usageEventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usage event not found after correction."));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
