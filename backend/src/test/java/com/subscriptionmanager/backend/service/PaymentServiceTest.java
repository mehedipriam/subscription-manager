package com.subscriptionmanager.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.subscriptionmanager.backend.dto.subscription.PaymentRequest;
import com.subscriptionmanager.backend.dto.subscription.PaymentResponse;
import com.subscriptionmanager.backend.entity.Payment;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.enums.PaymentStatus;
import com.subscriptionmanager.backend.exception.ResourceNotFoundException;
import com.subscriptionmanager.backend.repository.PaymentRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, subscriptionRepository);
    }

    private Subscription subscription(long id, String currency) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setCurrency(currency);
        return subscription;
    }

    @Test
    void listForSubscriptionReturnsPaymentsForAnOwnedSubscription() {
        Subscription subscription = subscription(5L, "USD");
        when(subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(5L, 1L)).thenReturn(Optional.of(subscription));
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setSubscription(subscription);
        payment.setAmount(new BigDecimal("9.99"));
        payment.setCurrency("USD");
        payment.setPaymentDate(LocalDate.now());
        payment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findBySubscriptionIdAndDeletedAtIsNullOrderByPaymentDateDesc(5L))
            .thenReturn(List.of(payment));

        List<PaymentResponse> result = service.listForSubscription(1L, 5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).subscriptionId()).isEqualTo(5L);
    }

    @Test
    void listForSubscriptionThrowsWhenSubscriptionNotOwnedByUser() {
        when(subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listForSubscription(1L, 5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createDefaultsCurrencyToSubscriptionCurrencyWhenNotProvided() {
        Subscription subscription = subscription(5L, "EUR");
        when(subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(5L, 1L)).thenReturn(Optional.of(subscription));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentRequest request = new PaymentRequest(new BigDecimal("9.99"), null, LocalDate.now(), null, null);
        PaymentResponse response = service.create(1L, 5L, request);

        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void createUsesProvidedCurrencyAndStatusWhenGiven() {
        Subscription subscription = subscription(5L, "EUR");
        when(subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(5L, 1L)).thenReturn(Optional.of(subscription));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentRequest request = new PaymentRequest(
            new BigDecimal("9.99"), "usd", LocalDate.now(), PaymentStatus.PENDING, "txn-123");
        PaymentResponse response = service.create(1L, 5L, request);

        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.transactionReference()).isEqualTo("txn-123");
    }

    @Test
    void deleteSoftDeletesAnOwnedPayment() {
        Subscription subscription = subscription(5L, "USD");
        when(subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(5L, 1L)).thenReturn(Optional.of(subscription));
        Payment payment = new Payment();
        payment.setId(2L);
        when(paymentRepository.findByIdAndSubscriptionIdAndDeletedAtIsNull(2L, 5L)).thenReturn(Optional.of(payment));

        service.delete(1L, 5L, 2L);

        assertThat(payment.getDeletedAt()).isNotNull();
        verify(paymentRepository).save(payment);
    }

    @Test
    void deleteThrowsWhenPaymentNotFoundUnderTheSubscription() {
        Subscription subscription = subscription(5L, "USD");
        when(subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(5L, 1L)).thenReturn(Optional.of(subscription));
        when(paymentRepository.findByIdAndSubscriptionIdAndDeletedAtIsNull(2L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 5L, 2L)).isInstanceOf(ResourceNotFoundException.class);
        verify(paymentRepository, never()).save(any());
    }
}
