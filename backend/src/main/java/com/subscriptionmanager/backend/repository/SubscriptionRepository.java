package com.subscriptionmanager.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.subscriptionmanager.backend.entity.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<Subscription> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
