package com.subscriptionmanager.backend.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subscriptionmanager.backend.entity.Notification;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.UserPreferences;
import com.subscriptionmanager.backend.entity.enums.NotificationType;
import com.subscriptionmanager.backend.entity.enums.SubscriptionStatus;
import com.subscriptionmanager.backend.repository.NotificationRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;
import com.subscriptionmanager.backend.repository.UserPreferencesRepository;

import lombok.RequiredArgsConstructor;

/**
 * Generates in-app reminder notifications for upcoming renewals and ending
 * trials. Runs on a schedule; each generated notification is keyed by
 * (user, subscription, type, referenceDate) so re-running the scan never
 * creates duplicates for an event that's already been announced.
 */
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final UserPreferencesRepository userPreferencesRepository;

    @Value("${app.notifications.renewal-reminder-days:7}")
    private int renewalReminderDays;

    @Value("${app.notifications.trial-reminder-days:3}")
    private int trialReminderDays;

    @Scheduled(
        initialDelayString = "${app.notifications.scheduler-initial-delay-ms:10000}",
        fixedRateString = "${app.notifications.scheduler-interval-ms:86400000}"
    )
    @Transactional
    public void generateReminders() {
        LocalDate today = LocalDate.now();
        generateRenewalReminders(today);
        generateTrialReminders(today);
    }

    private void generateRenewalReminders(LocalDate today) {
        List<Subscription> renewing = subscriptionRepository.findActiveRenewingBetween(
            SubscriptionStatus.ACTIVE, today, today.plusDays(renewalReminderDays));

        for (Subscription subscription : renewing) {
            createIfAbsent(
                subscription,
                NotificationType.RENEWAL_UPCOMING,
                subscription.getNextBillingDate(),
                "%s renews on %s (%.2f %s)".formatted(
                    subscription.getName(),
                    subscription.getNextBillingDate(),
                    subscription.getPrice(),
                    subscription.getCurrency())
            );
        }
    }

    private void generateTrialReminders(LocalDate today) {
        List<Subscription> endingTrials = subscriptionRepository.findActiveTrialsEndingBetween(
            SubscriptionStatus.ACTIVE, today, today.plusDays(trialReminderDays));

        for (Subscription subscription : endingTrials) {
            createIfAbsent(
                subscription,
                NotificationType.TRIAL_ENDING,
                subscription.getTrialEndDate(),
                "Your %s trial ends on %s".formatted(subscription.getName(), subscription.getTrialEndDate())
            );
        }
    }

    private void createIfAbsent(
        Subscription subscription, NotificationType type, LocalDate referenceDate, String message
    ) {
        Long userId = subscription.getUser().getId();
        if (!notificationsEnabled(userId)) {
            return;
        }
        boolean alreadyNotified = notificationRepository.existsByUserIdAndSubscriptionIdAndTypeAndReferenceDate(
            userId, subscription.getId(), type, referenceDate);
        if (alreadyNotified) {
            return;
        }

        Notification notification = new Notification();
        notification.setUser(subscription.getUser());
        notification.setSubscription(subscription);
        notification.setType(type);
        notification.setReferenceDate(referenceDate);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    private boolean notificationsEnabled(Long userId) {
        return userPreferencesRepository.findByUserId(userId)
            .map(UserPreferences::isNotificationsEnabled)
            .orElse(true);
    }
}
