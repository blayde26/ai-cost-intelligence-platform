package com.acip.jira;

import com.acip.worktracking.WorkItem;
import com.acip.worktracking.WorkItemType;
import com.acip.worktracking.WorkTrackingProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JiraSyncServiceTest {

    private final WorkTrackingProvider provider = mock(WorkTrackingProvider.class);
    private final EpicRepository epicRepository = mock(EpicRepository.class);
    private final StoryRepository storyRepository = mock(StoryRepository.class);
    private final JiraSyncService service = new JiraSyncService(provider, epicRepository, storyRepository);

    @Test
    void syncUsesRequestJqlWhenProvided() {
        String jql = "project = KAN ORDER BY updated DESC";
        WorkItem epic = new WorkItem("KAN-1", WorkItemType.EPIC, "Platform", "In Progress", "KAN", null, "UNKNOWN");
        WorkItem story = new WorkItem("KAN-2", WorkItemType.STORY, "Integrate Jira", "To Do", "KAN", "KAN-1", "CAPITALIZED");
        when(provider.fetchWorkItems(jql)).thenReturn(List.of(epic, story));

        JiraSyncResult result = service.sync(new JiraSyncRequest(jql));

        assertThat(result.issuesFetched()).isEqualTo(2);
        assertThat(result.epicsUpserted()).isEqualTo(1);
        assertThat(result.storiesUpserted()).isEqualTo(1);
        verify(provider).fetchWorkItems(jql);
        verify(epicRepository).upsert(epic);
        verify(storyRepository).upsert(story);
    }

    @Test
    void syncUsesProviderDefaultsWhenJqlIsMissing() {
        when(provider.fetchWorkItems(null)).thenReturn(List.of());

        JiraSyncResult result = service.sync(null);

        assertThat(result.issuesFetched()).isZero();
        verify(provider).fetchWorkItems(null);
    }
}
