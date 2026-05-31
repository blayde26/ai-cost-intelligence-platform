package com.acip.usage;

import jakarta.validation.constraints.NotBlank;

public record AttributionCorrectionRequest(
        String storyKey,
        String epicKey,
        String teamKey,
        String workType,
        @NotBlank String correctedBy,
        String note
) {
}
