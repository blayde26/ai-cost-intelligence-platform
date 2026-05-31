package com.acip.capture;

import org.springframework.stereotype.Component;

@Component
public class ProxyCaptureProvider implements UsageCaptureProvider {

    @Override
    public UsageCaptureSource source() {
        return UsageCaptureSource.PROXY;
    }

    @Override
    public UsageCaptureMethod method() {
        return UsageCaptureMethod.REAL_TIME_PROXY;
    }

    @Override
    public UsageCaptureConfidence defaultConfidence() {
        return UsageCaptureConfidence.HIGH;
    }

    @Override
    public String providerKey() {
        return "OPENAI_COMPATIBLE_PROXY";
    }
}
