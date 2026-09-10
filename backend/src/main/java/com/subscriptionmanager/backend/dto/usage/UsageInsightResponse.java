package com.subscriptionmanager.backend.dto.usage;

import java.math.BigDecimal;
import java.time.Instant;

public record UsageInsightResponse(
    Long subscriptionId,
    String subscriptionName,
    BigDecimal monthlyCost,
    String currency,
    Instant lastUsedAt,
    int daysSinceLastUsed,
    long usageCount,
    boolean rarelyUsed
) {
}
