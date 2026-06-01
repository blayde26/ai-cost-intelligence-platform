package com.acip.capture;

public interface UsageImportProvider {

    String importProviderKey();

    String displayName();

    UsageImportResult importUsage(String payload);

    UsageImportResult previewUsage(String payload);
}
