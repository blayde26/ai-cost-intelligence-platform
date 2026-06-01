package com.acip.outcomes;

import java.math.BigDecimal;

public record OutcomeCorrelationSignal(
        String subjectType,
        String subjectKey,
        BigDecimal aiSpend,
        String outcomeMetric,
        double outcomeValue,
        String signal,
        String interpretation
) {
}
