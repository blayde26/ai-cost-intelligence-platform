package com.acip.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class OpenAiUsageParser {

    private final ObjectMapper objectMapper;

    public OpenAiUsageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OpenAiTokenUsage parse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return OpenAiTokenUsage.empty();
        }
        try {
            JsonNode usage = objectMapper.readTree(responseBody).path("usage");
            if (usage.isMissingNode()) {
                return OpenAiTokenUsage.empty();
            }
            int promptTokens = usage.path("prompt_tokens").asInt(0);
            int completionTokens = usage.path("completion_tokens").asInt(0);
            int totalTokens = usage.path("total_tokens").asInt(promptTokens + completionTokens);
            return new OpenAiTokenUsage(promptTokens, completionTokens, totalTokens);
        } catch (Exception ignored) {
            return OpenAiTokenUsage.empty();
        }
    }
}
