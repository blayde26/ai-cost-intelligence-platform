package com.acip.capture;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsageImportController {

    private final ManualImportProvider manualImportProvider;

    public UsageImportController(ManualImportProvider manualImportProvider) {
        this.manualImportProvider = manualImportProvider;
    }

    @PostMapping(
            value = "/api/v1/usage/imports/csv",
            consumes = {MediaType.TEXT_PLAIN_VALUE, "text/csv"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public UsageImportResult importCsv(@RequestBody String csv) {
        return manualImportProvider.importCsv(csv);
    }
}
