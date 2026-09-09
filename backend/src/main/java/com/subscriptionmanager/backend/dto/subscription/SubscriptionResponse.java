package com.subscriptionmanager.backend.dto.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.enums.BillingCycle;
import com.subscriptionmanager.backend.entity.enums.SubscriptionStatus;

public record SubscriptionResponse(
    Long id,
    String name,
    String description,
    CategoryResponse category,
    BigDecimal price,
    String currency,
    BillingCycle billingCycle,
    LocalDate startDate,
    LocalDate nextBillingDate,
    SubscriptionStatus status,
    boolean isTrial,
    LocalDate trialEndDate,
    String cancelUrl,
    String cancellationInstructions,
    Instant createdAt,
    Instant updatedAt
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
            subscription.getId(),
            subscription.getName(),
            subscription.getDescription(),
            subscription.getCategory() == null ? null : CategoryResponse.from(subscription.getCategory()),
            subscription.getPrice(),
            subscription.getCurrency(),
            subscription.getBillingCycle(),
            subscription.getStartDate(),
            subscription.getNextBillingDate(),
            subscription.getStatus(),
            subscription.isTrial(),
            subscription.getTrialEndDate(),
            subscription.getCancelUrl(),
            subscription.getCancellationInstructions(),
            subscription.getCreatedAt(),
            subscription.getUpdatedAt()
        );
    }
}
