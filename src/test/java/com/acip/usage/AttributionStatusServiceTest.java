package com.acip.usage;

import com.acip.worktracking.WorkItem;
import com.acip.worktracking.WorkItemType;
import com.acip.worktracking.WorkTrackingProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttributionStatusServiceTest {

    private final WorkTrackingProvider provider = mock(WorkTrackingProvider.class);
    private final AttributionStatusService service = new AttributionStatusService(provider);

    @Test
    void validStoryReturnsValid() {
        when(provider.findStoryByKey("PAY-1001"))
                .thenReturn(Optional.of(story("PAY-1001", "PAY-1000")));
        when(provider.findEpicByKey("PAY-1000"))
                .thenReturn(Optional.of(epic("PAY-1000")));

        assertThat(service.classify("PAY-1001")).isEqualTo(AttributionStatus.VALID);
    }

    @Test
    void missingStoryKeyReturnsMissingStoryKey() {
        assertThat(service.classify(" ")).isEqualTo(AttributionStatus.MISSING_STORY_KEY);
    }

    @Test
    void unknownStoryReturnsUnknownStory() {
        when(provider.findStoryByKey("BAD-999")).thenReturn(Optional.empty());

        assertThat(service.classify("BAD-999")).isEqualTo(AttributionStatus.UNKNOWN_STORY);
    }

    @Test
    void knownStoryWithMissingEpicReturnsUnknownEpic() {
        when(provider.findStoryByKey("PAY-1001"))
                .thenReturn(Optional.of(story("PAY-1001", "PAY-1000")));
        when(provider.findEpicByKey("PAY-1000")).thenReturn(Optional.empty());

        assertThat(service.classify("PAY-1001")).isEqualTo(AttributionStatus.UNKNOWN_EPIC);
    }

    @Test
    void manualOverrideReturnsManual() {
        assertThat(service.classify("PAY-1001", true)).isEqualTo(AttributionStatus.MANUAL);
    }

    private WorkItem story(String key, String epicKey) {
        return new WorkItem(key, WorkItemType.STORY, "Story", "In Progress", "payments", epicKey, "CAPITALIZED");
    }

    private WorkItem epic(String key) {
        return new WorkItem(key, WorkItemType.EPIC, "Epic", "In Progress", "payments", null, "UNKNOWN");
    }
}
