package com.subscriptionmanager.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.subscriptionmanager.backend.entity.Notification;
import com.subscriptionmanager.backend.entity.enums.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    boolean existsByUserIdAndSubscriptionIdAndTypeAndReferenceDate(
        Long userId, Long subscriptionId, NotificationType type, LocalDate referenceDate);

    boolean existsByUserIdAndTypeAndReferenceDate(Long userId, NotificationType type, LocalDate referenceDate);
}
