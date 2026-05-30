package com.acip.proxy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "acip.openai")
public record OpenAiProperties(
        String provider,
        String apiKey,
        boolean requireApiKey,
        String chatCompletionsUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
