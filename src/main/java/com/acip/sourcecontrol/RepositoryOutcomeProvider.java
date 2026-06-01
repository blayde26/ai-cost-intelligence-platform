package com.acip.sourcecontrol;

import java.util.List;

public interface RepositoryOutcomeProvider {

    String providerKey();

    List<RepositoryOutcomeMetrics> repositoryMetrics();
}
