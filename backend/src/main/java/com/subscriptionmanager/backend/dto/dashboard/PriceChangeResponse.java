package com.subscriptionmanager.backend.dto.dashboard;

import java.math.BigDecimal;
import java.time.Instant;

import com.subscriptionmanager.backend.entity.PriceHistory;

public record PriceChangeResponse(
    Long subscriptionId,
    String subscriptionName,
    BigDecimal oldPrice,
    BigDecimal newPrice,
    BigDecimal percentageChange,
    String currency,
    Instant changedAt
) {
    public static PriceChangeResponse from(PriceHistory priceHistory) {
        return new PriceChangeResponse(
            priceHistory.getSubscription().getId(),
            priceHistory.getSubscription().getName(),
            priceHistory.getOldPrice(),
            priceHistory.getNewPrice(),
            priceHistory.percentageChange(),
            priceHistory.getSubscription().getCurrency(),
            priceHistory.getChangedAt()
        );
    }
}
