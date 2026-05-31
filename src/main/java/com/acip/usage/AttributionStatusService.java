package com.acip.usage;

import com.acip.worktracking.WorkItem;
import com.acip.worktracking.WorkTrackingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AttributionStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributionStatusService.class);

    private final WorkTrackingProvider workTrackingProvider;

    public AttributionStatusService(WorkTrackingProvider workTrackingProvider) {
        this.workTrackingProvider = workTrackingProvider;
    }

    public AttributionStatus classify(String storyKey) {
        return classify(storyKey, false);
    }

    public AttributionStatus classify(String storyKey, boolean manualOverride) {
        if (manualOverride) {
            return AttributionStatus.MANUAL;
        }
        if (storyKey == null || storyKey.isBlank()) {
            return AttributionStatus.MISSING_STORY_KEY;
        }
        Optional<WorkItem> story = findStory(storyKey);
        if (story.isEmpty()) {
            return AttributionStatus.UNKNOWN_STORY;
        }
        String epicKey = story.get().epicKey();
        if (epicKey == null || epicKey.isBlank() || findEpic(epicKey).isEmpty()) {
            return AttributionStatus.UNKNOWN_EPIC;
        }
        return AttributionStatus.VALID;
    }

    private Optional<WorkItem> findStory(String storyKey) {
        try {
            return workTrackingProvider.findStoryByKey(storyKey);
        } catch (RuntimeException exception) {
            LOGGER.warn("Work tracking story lookup failed for storyKey={}: {}", storyKey, exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<WorkItem> findEpic(String epicKey) {
        try {
            return workTrackingProvider.findEpicByKey(epicKey);
        } catch (RuntimeException exception) {
            LOGGER.warn("Work tracking epic lookup failed for epicKey={}: {}", epicKey, exception.getMessage());
            return Optional.empty();
        }
    }
}
