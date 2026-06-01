package com.acip.sourcecontrol;

import java.util.List;

public interface RepositoryOutcomeProvider {

    String providerKey();

    List<RepositoryOutcomeMetrics> repositoryMetrics();

    default SourceControlMetricsCacheState cacheState() {
        return new SourceControlMetricsCacheState(false, false, null, null, 0);
    }
}
