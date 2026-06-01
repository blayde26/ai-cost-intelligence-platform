package com.acip.capture;

import org.springframework.stereotype.Service;

@Service
public class UsageImportSampleService {

    public UsageImportSample sample(String variant) {
        return switch (normalize(variant)) {
            case "openai" -> new UsageImportSample("openai", "acip-openai-style-usage-sample.csv", openAiStyle());
            case "advanced" -> new UsageImportSample("advanced", "acip-advanced-usage-sample.csv", advanced());
            default -> new UsageImportSample("minimal", "acip-minimal-usage-sample.csv", minimal());
        };
    }

    private String normalize(String variant) {
        return variant == null || variant.isBlank() ? "minimal" : variant.trim().toLowerCase();
    }

    private String minimal() {
        return """
                provider,model,teamKey,userKey,totalTokens,estimatedCostUsd,requestTimestamp
                OLLAMA,llama3.2,payments,alex,4200,0.00033600,2026-06-01T12:00:00Z
                """;
    }

    private String openAiStyle() {
        return """
                provider,model,teamKey,userKey,promptTokens,completionTokens,totalTokens,estimatedCostUsd,requestTimestamp
                OPENAI,gpt-4o-mini,platform,alex,1200,240,1440,0.00456000,2026-06-01T12:00:00Z
                OPENAI,gpt-4.1-mini,platform,sam,1800,320,2120,0.00810000,2026-06-01T13:15:00Z
                """;
    }

    private String advanced() {
        return """
                provider,model,storyKey,epicKey,teamKey,userKey,promptTokens,completionTokens,totalTokens,estimatedCostUsd,requestTimestamp,environment,workType,requestStatus,repository,branch,commitHash,initiativeKey,initiativeName
                OLLAMA,llama3.2,PAY-1002,PAY-1000,payments,alex,3200,600,3800,0.00030400,2026-06-01T12:00:00Z,local,CAPITALIZED,SUCCEEDED,checkout-service,feature/PAY-1002-payment-retry,abc123,PAYMENT-INIT,Checkout Modernization
                OPENAI,gpt-4o-mini,,CX-1000,customer-experience,sam,900,210,1110,0.00310000,2026-06-01T13:00:00Z,prod,OPERATIONAL,SUCCEEDED,support-bot,feature/CX-1001-support-bot,def456,CX-AUTO,Support Automation
                """;
    }
}
