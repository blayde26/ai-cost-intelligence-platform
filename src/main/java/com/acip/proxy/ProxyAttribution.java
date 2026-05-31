package com.acip.proxy;

import jakarta.validation.constraints.NotBlank;

public record ProxyAttribution(
        String storyKey,
        @NotBlank String teamKey,
        @NotBlank String userKey
) {
}
