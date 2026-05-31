package com.acip.usage;

import org.springframework.stereotype.Service;

@Service
public class AttributionInferenceService {

    private final BranchStoryKeyParser branchStoryKeyParser;

    public AttributionInferenceService(BranchStoryKeyParser branchStoryKeyParser) {
        this.branchStoryKeyParser = branchStoryKeyParser;
    }

    public AttributionInference infer(String explicitStoryKey, String branch) {
        String normalizedStoryKey = normalize(explicitStoryKey);
        if (normalizedStoryKey != null) {
            return new AttributionInference(
                    normalizedStoryKey,
                    AttributionSource.EXPLICIT,
                    AttributionConfidence.HIGH,
                    "Story key was provided explicitly."
            );
        }
        return branchStoryKeyParser.parse(branch)
                .map(storyKey -> new AttributionInference(
                        storyKey,
                        AttributionSource.INFERRED_BRANCH,
                        AttributionConfidence.HIGH,
                        "Story key was inferred from branch name."
                ))
                .orElseGet(() -> new AttributionInference(
                        null,
                        AttributionSource.MISSING,
                        AttributionConfidence.LOW,
                        "No explicit story key or parseable branch key was provided."
                ));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
