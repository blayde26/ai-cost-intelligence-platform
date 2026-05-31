package com.acip.capture;

public interface UsageCaptureProvider {

    UsageCaptureSource source();

    UsageCaptureMethod method();

    UsageCaptureConfidence defaultConfidence();

    String providerKey();

    default UsageCaptureMetadata metadata() {
        return new UsageCaptureMetadata(source(), providerKey(), method(), defaultConfidence());
    }
}
