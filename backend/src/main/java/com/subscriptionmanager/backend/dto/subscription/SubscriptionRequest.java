package com.subscriptionmanager.backend.dto.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.subscriptionmanager.backend.entity.enums.BillingCycle;
import com.subscriptionmanager.backend.entity.enums.SubscriptionStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubscriptionRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    String name,

    String description,

    Long categoryId,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    BigDecimal price,

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    String currency,

    @NotNull(message = "Billing cycle is required")
    BillingCycle billingCycle,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    LocalDate nextBillingDate,

    SubscriptionStatus status,

    Boolean isTrial,

    LocalDate trialEndDate,

    @Size(max = 500, message = "Cancel URL must be at most 500 characters")
    String cancelUrl,

    String cancellationInstructions,

    @Pattern(regexp = "\\d{4}", message = "Card last 4 digits must be exactly 4 digits")
    String paymentCardLastFour
) {
}
