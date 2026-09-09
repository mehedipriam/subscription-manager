package com.subscriptionmanager.backend.dto.dashboard;

import java.math.BigDecimal;
import java.time.Instant;

public record ActivityItemResponse(
    String type,
    Instant timestamp,
    Long subscriptionId,
    String subscriptionName,
    BigDecimal amount,
    String currency
) {
    public static ActivityItemResponse subscriptionAdded(Long subscriptionId, String name, Instant timestamp) {
        return new ActivityItemResponse("SUBSCRIPTION_ADDED", timestamp, subscriptionId, name, null, null);
    }

    public static ActivityItemResponse paymentRecorded(
        Long subscriptionId, String name, Instant timestamp, BigDecimal amount, String currency
    ) {
        return new ActivityItemResponse("PAYMENT_RECORDED", timestamp, subscriptionId, name, amount, currency);
    }
}
