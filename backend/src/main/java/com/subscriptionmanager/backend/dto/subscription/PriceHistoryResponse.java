package com.subscriptionmanager.backend.dto.subscription;

import java.math.BigDecimal;
import java.time.Instant;

import com.subscriptionmanager.backend.entity.PriceHistory;

public record PriceHistoryResponse(
    Long id,
    BigDecimal oldPrice,
    BigDecimal newPrice,
    BigDecimal percentageChange,
    Instant changedAt
) {
    public static PriceHistoryResponse from(PriceHistory priceHistory) {
        return new PriceHistoryResponse(
            priceHistory.getId(),
            priceHistory.getOldPrice(),
            priceHistory.getNewPrice(),
            priceHistory.percentageChange(),
            priceHistory.getChangedAt()
        );
    }
}
