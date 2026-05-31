package com.acip.proxy;

import jakarta.validation.constraints.NotBlank;

public record ProxyAttribution(
        String storyKey,
        @NotBlank String teamKey,
        @NotBlank String userKey,
        String repository,
        String branch,
        String commitHash
) {
    public ProxyAttribution(String storyKey, String teamKey, String userKey) {
        this(storyKey, teamKey, userKey, null, null, null);
    }
}
