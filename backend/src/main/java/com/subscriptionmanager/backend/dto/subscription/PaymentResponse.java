package com.subscriptionmanager.backend.dto.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.subscriptionmanager.backend.entity.Payment;
import com.subscriptionmanager.backend.entity.enums.PaymentStatus;

public record PaymentResponse(
    Long id,
    Long subscriptionId,
    BigDecimal amount,
    String currency,
    LocalDate paymentDate,
    PaymentStatus status,
    String transactionReference,
    Instant createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getSubscription().getId(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getPaymentDate(),
            payment.getStatus(),
            payment.getTransactionReference(),
            payment.getCreatedAt()
        );
    }
}
