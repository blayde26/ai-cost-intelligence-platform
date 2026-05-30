package com.acip.pricing;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;

@Service
public class PricingService {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");
    private final ProviderPricingRepository providerPricingRepository;
    private final Clock clock;

    @Autowired
    public PricingService(ProviderPricingRepository providerPricingRepository) {
        this(providerPricingRepository, Clock.systemUTC());
    }

    PricingService(ProviderPricingRepository providerPricingRepository, Clock clock) {
        this.providerPricingRepository = providerPricingRepository;
        this.clock = clock;
    }

    public BigDecimal estimateCostUsd(String provider, String model, int promptTokens, int completionTokens) {
        return providerPricingRepository.findEffectivePricing(provider, model, LocalDate.now(clock))
                .map(pricing -> calculateCost(pricing, promptTokens, completionTokens))
                .orElse(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP));
    }

    private BigDecimal calculateCost(ProviderPricing pricing, int promptTokens, int completionTokens) {
        BigDecimal inputCost = BigDecimal.valueOf(promptTokens)
                .divide(ONE_MILLION, 12, RoundingMode.HALF_UP)
                .multiply(pricing.inputCostPerMillion());
        BigDecimal outputCost = BigDecimal.valueOf(completionTokens)
                .divide(ONE_MILLION, 12, RoundingMode.HALF_UP)
                .multiply(pricing.outputCostPerMillion());
        return inputCost.add(outputCost).setScale(8, RoundingMode.HALF_UP);
    }
}
