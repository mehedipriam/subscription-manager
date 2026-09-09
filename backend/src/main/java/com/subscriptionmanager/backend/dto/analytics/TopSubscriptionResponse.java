package com.subscriptionmanager.backend.dto.analytics;

import java.math.BigDecimal;

import com.subscriptionmanager.backend.dto.subscription.CategoryResponse;

public record TopSubscriptionResponse(
    Long subscriptionId,
    String name,
    CategoryResponse category,
    BigDecimal normalizedMonthlyCost,
    String currency
) {
}
