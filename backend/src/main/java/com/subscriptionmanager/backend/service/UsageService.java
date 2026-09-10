package com.subscriptionmanager.backend.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subscriptionmanager.backend.dto.usage.UsageInsightResponse;
import com.subscriptionmanager.backend.dto.usage.UsageLogResponse;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.UsageLog;
import com.subscriptionmanager.backend.entity.enums.SubscriptionStatus;
import com.subscriptionmanager.backend.exception.ResourceNotFoundException;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;
import com.subscriptionmanager.backend.repository.UsageLogRepository;
import com.subscriptionmanager.backend.repository.UsageSummaryProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsageService {

    private final UsageLogRepository usageLogRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CostNormalizationService costNormalizationService;

    @Value("${app.usage.rarely-used-days:30}")
    private int rarelyUsedDays;

    @Transactional
    public UsageLogResponse logUsage(Long userId, Long subscriptionId) {
        Subscription subscription = findOwned(userId, subscriptionId);

        UsageLog usageLog = new UsageLog();
        usageLog.setSubscription(subscription);
        usageLog.setUsedAt(Instant.now());

        return UsageLogResponse.from(usageLogRepository.save(usageLog));
    }

    @Transactional(readOnly = true)
    public List<UsageLogResponse> history(Long userId, Long subscriptionId) {
        Subscription subscription = findOwned(userId, subscriptionId);
        return usageLogRepository.findBySubscriptionIdOrderByUsedAtDesc(subscription.getId()).stream()
            .map(UsageLogResponse::from)
            .toList();
    }

    /**
     * One insight per active subscription: how long since it was last
     * logged as used (or since it was added, if never used), and whether
     * that crosses the "rarely used" threshold worth flagging for
     * cancellation.
     */
    @Transactional(readOnly = true)
    public List<UsageInsightResponse> insights(Long userId) {
        List<Subscription> active = subscriptionRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
            .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
            .toList();

        Map<Long, UsageSummaryProjection> summaryBySubscriptionId = usageLogRepository.summarizeByUserId(userId)
            .stream()
            .collect(Collectors.toMap(UsageSummaryProjection::getSubscriptionId, Function.identity()));

        Instant now = Instant.now();

        return active.stream()
            .map(subscription -> toInsight(subscription, summaryBySubscriptionId.get(subscription.getId()), now))
            .sorted(Comparator.comparingInt(UsageInsightResponse::daysSinceLastUsed).reversed())
            .toList();
    }

    private UsageInsightResponse toInsight(Subscription subscription, UsageSummaryProjection summary, Instant now) {
        Instant lastUsedAt = summary != null ? summary.getLastUsedAt() : null;
        long usageCount = summary != null ? summary.getUsageCount() : 0;

        Instant sinceReference = lastUsedAt != null ? lastUsedAt : subscription.getCreatedAt();
        int daysSinceLastUsed = (int) Duration.between(sinceReference, now).toDays();
        boolean rarelyUsed = daysSinceLastUsed >= rarelyUsedDays;

        return new UsageInsightResponse(
            subscription.getId(),
            subscription.getName(),
            costNormalizationService.toMonthly(subscription.getPrice(), subscription.getBillingCycle()),
            subscription.getCurrency(),
            lastUsedAt,
            daysSinceLastUsed,
            usageCount,
            rarelyUsed
        );
    }

    private Subscription findOwned(Long userId, Long subscriptionId) {
        return subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(subscriptionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }
}
