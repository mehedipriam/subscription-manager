package com.subscriptionmanager.backend.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subscriptionmanager.backend.dto.subscription.PaymentRequest;
import com.subscriptionmanager.backend.dto.subscription.PaymentResponse;
import com.subscriptionmanager.backend.entity.Payment;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.enums.PaymentStatus;
import com.subscriptionmanager.backend.exception.ResourceNotFoundException;
import com.subscriptionmanager.backend.repository.PaymentRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
    public List<PaymentResponse> listForSubscription(Long userId, Long subscriptionId) {
        Subscription subscription = findOwnedSubscription(userId, subscriptionId);
        return paymentRepository.findBySubscriptionIdAndDeletedAtIsNullOrderByPaymentDateDesc(subscription.getId())
            .stream()
            .map(PaymentResponse::from)
            .toList();
    }

    @Transactional
    public PaymentResponse create(Long userId, Long subscriptionId, PaymentRequest request) {
        Subscription subscription = findOwnedSubscription(userId, subscriptionId);

        Payment payment = new Payment();
        payment.setSubscription(subscription);
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency() == null || request.currency().isBlank()
            ? subscription.getCurrency() : request.currency().toUpperCase());
        payment.setPaymentDate(request.paymentDate());
        payment.setStatus(request.status() != null ? request.status() : PaymentStatus.SUCCESS);
        payment.setTransactionReference(request.transactionReference());

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public void delete(Long userId, Long subscriptionId, Long paymentId) {
        Subscription subscription = findOwnedSubscription(userId, subscriptionId);

        Payment payment = paymentRepository.findByIdAndSubscriptionIdAndDeletedAtIsNull(paymentId, subscription.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        payment.setDeletedAt(Instant.now());
        paymentRepository.save(payment);
    }

    private Subscription findOwnedSubscription(Long userId, Long subscriptionId) {
        return subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(subscriptionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }
}
