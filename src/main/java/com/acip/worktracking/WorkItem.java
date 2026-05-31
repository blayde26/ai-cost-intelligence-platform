package com.acip.worktracking;

public record WorkItem(
        String key,
        WorkItemType type,
        String summary,
        String status,
        String teamKey,
        String epicKey,
        String workType
) {

    public boolean isEpic() {
        return type == WorkItemType.EPIC;
    }
}
