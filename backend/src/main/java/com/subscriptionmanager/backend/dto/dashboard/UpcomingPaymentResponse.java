package com.subscriptionmanager.backend.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.subscriptionmanager.backend.dto.subscription.CategoryResponse;
import com.subscriptionmanager.backend.entity.Subscription;

public record UpcomingPaymentResponse(
    Long subscriptionId,
    String name,
    LocalDate nextBillingDate,
    BigDecimal price,
    String currency,
    CategoryResponse category
) {
    public static UpcomingPaymentResponse from(Subscription subscription) {
        return new UpcomingPaymentResponse(
            subscription.getId(),
            subscription.getName(),
            subscription.getNextBillingDate(),
            subscription.getPrice(),
            subscription.getCurrency(),
            subscription.getCategory() == null ? null : CategoryResponse.from(subscription.getCategory())
        );
    }
}
