package com.acip.usage;

import java.time.OffsetDateTime;

public record AttributionCorrection(
        String storyKey,
        String epicKey,
        String teamKey,
        String workType,
        AttributionStatus attributionStatus,
        String correctedBy,
        OffsetDateTime correctedTimestamp,
        String note
) {
}
