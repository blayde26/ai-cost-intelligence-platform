package com.acip.proxy;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenAiProxyController {

    private final OpenAiProxyService openAiProxyService;

    public OpenAiProxyController(OpenAiProxyService openAiProxyService) {
        this.openAiProxyService = openAiProxyService;
    }

    @PostMapping("/api/v1/proxy/openai/chat/completions")
    public ResponseEntity<String> proxyChatCompletions(@Valid @RequestBody OpenAiProxyEnvelope envelope) {
        return openAiProxyService.proxyChatCompletions(envelope);
    }
}
