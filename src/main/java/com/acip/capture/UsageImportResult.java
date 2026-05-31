package com.acip.capture;

import java.util.List;

public record UsageImportResult(
        int importedCount,
        int skippedCount,
        List<UsageImportError> errors
) {
}
