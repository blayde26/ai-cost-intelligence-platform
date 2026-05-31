package com.acip.capture;

public record UsageImportError(
        int rowNumber,
        String message
) {
}
