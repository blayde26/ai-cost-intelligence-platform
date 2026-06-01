package com.acip.capture;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UsageImportController {

    private final java.util.Map<String, UsageImportProvider> importProviders;
    private final UsageImportSampleService sampleService;

    public UsageImportController(java.util.List<UsageImportProvider> importProviders, UsageImportSampleService sampleService) {
        this.importProviders = importProviders.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(UsageImportProvider::importProviderKey, provider -> provider));
        this.sampleService = sampleService;
    }

    @PostMapping(
            value = "/api/v1/usage/imports/csv",
            consumes = {MediaType.TEXT_PLAIN_VALUE, "text/csv"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public UsageImportResult importCsv(@RequestBody String csv) {
        return importUsage("csv", csv);
    }

    @PostMapping(
            value = "/api/v1/usage/imports/{providerKey}",
            consumes = {MediaType.TEXT_PLAIN_VALUE, "text/csv"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public UsageImportResult importUsage(@PathVariable String providerKey, @RequestBody String payload) {
        return provider(providerKey).importUsage(payload);
    }

    @PostMapping(
            value = "/api/v1/usage/imports/{providerKey}/preview",
            consumes = {MediaType.TEXT_PLAIN_VALUE, "text/csv"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public UsageImportResult previewUsage(@PathVariable String providerKey, @RequestBody String payload) {
        return provider(providerKey).previewUsage(payload);
    }

    @GetMapping(value = "/api/v1/usage/imports/samples/{variant}", produces = "text/csv")
    public ResponseEntity<String> sample(@PathVariable String variant) {
        UsageImportSample sample = sampleService.sample(variant);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sample.fileName() + "\"")
                .body(sample.content());
    }

    private UsageImportProvider provider(String providerKey) {
        UsageImportProvider provider = importProviders.get(providerKey);
        if (provider == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Usage import provider not found: " + providerKey);
        }
        return provider;
    }
}
