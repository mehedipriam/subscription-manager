package com.subscriptionmanager.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.subscriptionmanager.backend.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySubscriptionIdAndDeletedAtIsNullOrderByPaymentDateDesc(Long subscriptionId);

    Optional<Payment> findByIdAndSubscriptionIdAndDeletedAtIsNull(Long id, Long subscriptionId);
}
