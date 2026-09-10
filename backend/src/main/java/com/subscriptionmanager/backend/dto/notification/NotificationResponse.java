package com.subscriptionmanager.backend.dto.notification;

import java.time.Instant;

import com.subscriptionmanager.backend.entity.Notification;
import com.subscriptionmanager.backend.entity.enums.NotificationType;

public record NotificationResponse(
    Long id,
    NotificationType type,
    String message,
    Long subscriptionId,
    String subscriptionName,
    boolean isRead,
    Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getMessage(),
            notification.getSubscription() == null ? null : notification.getSubscription().getId(),
            notification.getSubscription() == null ? null : notification.getSubscription().getName(),
            notification.isRead(),
            notification.getCreatedAt()
        );
    }
}
