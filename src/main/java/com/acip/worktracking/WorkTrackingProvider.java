package com.acip.worktracking;

import java.util.List;
import java.util.Optional;

public interface WorkTrackingProvider {

    List<WorkItem> fetchStories();

    List<WorkItem> fetchEpics();

    default List<WorkItem> fetchWorkItems(String query) {
        java.util.ArrayList<WorkItem> items = new java.util.ArrayList<>();
        items.addAll(fetchEpics());
        items.addAll(fetchStories());
        return items;
    }

    Optional<WorkItem> findStoryByKey(String storyKey);

    Optional<WorkItem> findEpicByKey(String epicKey);

    default boolean storyExists(String storyKey) {
        return findStoryByKey(storyKey).isPresent();
    }
}
