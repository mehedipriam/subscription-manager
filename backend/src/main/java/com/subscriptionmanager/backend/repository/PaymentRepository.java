package com.subscriptionmanager.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.subscriptionmanager.backend.entity.Payment;
import com.subscriptionmanager.backend.entity.enums.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySubscriptionIdAndDeletedAtIsNullOrderByPaymentDateDesc(Long subscriptionId);

    Optional<Payment> findByIdAndSubscriptionIdAndDeletedAtIsNull(Long id, Long subscriptionId);

    List<Payment> findTop10BySubscriptionUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    List<Payment> findBySubscriptionUserIdAndStatusAndDeletedAtIsNullAndPaymentDateGreaterThanEqual(
        Long userId, PaymentStatus status, LocalDate since);
}
