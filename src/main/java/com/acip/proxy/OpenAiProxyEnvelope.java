package com.acip.proxy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OpenAiProxyEnvelope(
        @Valid @NotNull ProxyAttribution attribution,
        @JsonProperty("request") @NotNull JsonNode request
) {
}
