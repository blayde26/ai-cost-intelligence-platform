package com.acip.outcomes;

import java.util.List;

public interface OutcomeProvider {

    String providerKey();

    List<TeamAnalyticsSnapshot> teamSnapshots();

    List<RepositoryAnalyticsSnapshot> repositorySnapshots();
}
