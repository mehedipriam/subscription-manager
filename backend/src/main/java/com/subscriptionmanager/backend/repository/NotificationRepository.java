package com.subscriptionmanager.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.subscriptionmanager.backend.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
}
