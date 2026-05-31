package com.acip.worktracking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockWorkTrackingProviderTest {

    private final MockWorkTrackingProvider provider = new MockWorkTrackingProvider();

    @Test
    void knownStoryKeyReturnsStory() {
        assertThat(provider.findStoryByKey("PAY-1001"))
                .isPresent()
                .get()
                .extracting(WorkItem::summary)
                .isEqualTo("Refactor Tax Calculation Service");
    }

    @Test
    void unknownStoryKeyReturnsEmpty() {
        assertThat(provider.findStoryByKey("NOPE-999")).isEmpty();
    }

    @Test
    void knownEpicKeyReturnsEpic() {
        assertThat(provider.findEpicByKey("PLAT-1000"))
                .isPresent()
                .get()
                .extracting(WorkItem::type)
                .isEqualTo(WorkItemType.EPIC);
    }

    @Test
    void canFetchAllStoriesAndEpics() {
        assertThat(provider.fetchStories()).hasSize(20);
        assertThat(provider.fetchEpics()).hasSize(5);
    }
}
