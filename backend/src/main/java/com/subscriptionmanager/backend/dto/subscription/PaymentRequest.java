package com.subscriptionmanager.backend.dto.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.subscriptionmanager.backend.entity.enums.PaymentStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentRequest(
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Amount cannot be negative")
    BigDecimal amount,

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    String currency,

    @NotNull(message = "Payment date is required")
    LocalDate paymentDate,

    PaymentStatus status,

    @Size(max = 255, message = "Transaction reference must be at most 255 characters")
    String transactionReference
) {
}
