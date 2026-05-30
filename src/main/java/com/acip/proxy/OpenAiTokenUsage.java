package com.acip.proxy;

public record OpenAiTokenUsage(
        int promptTokens,
        int completionTokens,
        int totalTokens
) {

    public static OpenAiTokenUsage empty() {
        return new OpenAiTokenUsage(0, 0, 0);
    }
}
