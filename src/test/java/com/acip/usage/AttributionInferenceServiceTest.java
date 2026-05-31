package com.acip.usage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttributionInferenceServiceTest {

    private final AttributionInferenceService service = new AttributionInferenceService(new BranchStoryKeyParser());

    @Test
    void explicitStoryKeyWinsOverBranchInference() {
        AttributionInference inference = service.infer("KAN-9", "feature/PAY-1001-checkout");

        assertThat(inference.storyKey()).isEqualTo("KAN-9");
        assertThat(inference.source()).isEqualTo(AttributionSource.EXPLICIT);
        assertThat(inference.confidence()).isEqualTo(AttributionConfidence.HIGH);
    }

    @Test
    void branchStoryKeyIsUsedWhenExplicitStoryKeyIsMissing() {
        AttributionInference inference = service.infer(null, "feature/PAY-1001-checkout");

        assertThat(inference.storyKey()).isEqualTo("PAY-1001");
        assertThat(inference.source()).isEqualTo(AttributionSource.INFERRED_BRANCH);
        assertThat(inference.confidence()).isEqualTo(AttributionConfidence.HIGH);
    }

    @Test
    void missingMetadataProducesMissingAttribution() {
        AttributionInference inference = service.infer(null, "main");

        assertThat(inference.storyKey()).isNull();
        assertThat(inference.source()).isEqualTo(AttributionSource.MISSING);
        assertThat(inference.confidence()).isEqualTo(AttributionConfidence.LOW);
    }
}
