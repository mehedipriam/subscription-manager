package com.subscriptionmanager.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.subscriptionmanager.backend.dto.usage.UsageInsightResponse;
import com.subscriptionmanager.backend.dto.usage.UsageLogResponse;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.UsageLog;
import com.subscriptionmanager.backend.entity.enums.BillingCycle;
import com.subscriptionmanager.backend.entity.enums.SubscriptionStatus;
import com.subscriptionmanager.backend.exception.ResourceNotFoundException;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;
import com.subscriptionmanager.backend.repository.UsageLogRepository;
import com.subscriptionmanager.backend.repository.UsageSummaryProjection;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {

    @Mock
    private UsageLogRepository usageLogRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;

    private UsageService service;

    @BeforeEach
    void setUp() {
        service = new UsageService(usageLogRepository, subscriptionRepository, new CostNormalizationService());
        ReflectionTestUtils.setField(service, "rarelyUsedDays", 30);
    }

    private Subscription subscription(long id, String name, Instant createdAt) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setName(name);
        subscription.setPrice(new BigDecimal("10.00"));
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setCurrency("USD");
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCreatedAt(createdAt);
        return subscription;
    }

    private UsageSummaryProjection summary(Long subscriptionId, Instant lastUsedAt, long usageCount) {
        return new UsageSummaryProjection() {
            @Override
            public Long getSubscriptionId() {
                return subscriptionId;
            }

            @Override
            public Instant getLastUsedAt() {
                return lastUsedAt;
            }

            @Override
            public Long getUsageCount() {
                return usageCount;
            }
        };
    }

    @Test
    void logUsageSavesAnEntryForAnOwnedSubscription() {
        Subscription subscription = subscription(1L, "Netflix", Instant.now());
        when(subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(1L, 10L)).thenReturn(Optional.of(subscription));
        when(usageLogRepository.save(any(UsageLog.class))).thenAnswer(invocation -> {
            UsageLog log = invocation.getArgument(0);
            log.setId(5L);
            return log;
        });

        UsageLogResponse response = service.logUsage(10L, 1L);

        assertThat(response.id()).isEqualTo(5L);
    }

    @Test
    void logUsageThrowsWhenSubscriptionNotOwnedByUser() {
        when(subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.logUsage(10L, 1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void insightsFlagsSubscriptionUnusedPastThresholdAsRarelyUsed() {
        Instant longAgo = Instant.now().minus(45, ChronoUnit.DAYS);
        Subscription subscription = subscription(1L, "Gym", Instant.now());
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of(subscription));
        when(usageLogRepository.summarizeByUserId(10L)).thenReturn(List.of(summary(1L, longAgo, 3L)));

        List<UsageInsightResponse> insights = service.insights(10L);

        assertThat(insights).hasSize(1);
        assertThat(insights.get(0).rarelyUsed()).isTrue();
        assertThat(insights.get(0).usageCount()).isEqualTo(3L);
    }

    @Test
    void insightsUsesCreationDateWhenSubscriptionNeverLoggedAsUsed() {
        Instant createdLongAgo = Instant.now().minus(60, ChronoUnit.DAYS);
        Subscription subscription = subscription(1L, "Gym", createdLongAgo);
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of(subscription));
        when(usageLogRepository.summarizeByUserId(10L)).thenReturn(List.of());

        List<UsageInsightResponse> insights = service.insights(10L);

        assertThat(insights.get(0).lastUsedAt()).isNull();
        assertThat(insights.get(0).usageCount()).isZero();
        assertThat(insights.get(0).rarelyUsed()).isTrue();
        assertThat(insights.get(0).daysSinceLastUsed()).isGreaterThanOrEqualTo(59);
    }

    @Test
    void insightsDoesNotFlagRecentlyUsedSubscriptionAsRarelyUsed() {
        Instant recently = Instant.now().minus(2, ChronoUnit.DAYS);
        Subscription subscription = subscription(1L, "Netflix", Instant.now().minus(100, ChronoUnit.DAYS));
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of(subscription));
        when(usageLogRepository.summarizeByUserId(10L)).thenReturn(List.of(summary(1L, recently, 10L)));

        List<UsageInsightResponse> insights = service.insights(10L);

        assertThat(insights.get(0).rarelyUsed()).isFalse();
    }

    @Test
    void insightsExcludesInactiveSubscriptions() {
        Subscription active = subscription(1L, "Netflix", Instant.now());
        Subscription cancelled = subscription(2L, "Gym", Instant.now());
        cancelled.setStatus(SubscriptionStatus.CANCELLED);
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of(active, cancelled));
        when(usageLogRepository.summarizeByUserId(10L)).thenReturn(List.of());

        List<UsageInsightResponse> insights = service.insights(10L);

        assertThat(insights).extracting(UsageInsightResponse::subscriptionId).containsExactly(1L);
    }

    @Test
    void insightsAreSortedByDaysSinceLastUsedDescending() {
        Subscription usedRecently = subscription(1L, "Netflix", Instant.now().minus(200, ChronoUnit.DAYS));
        Subscription usedLongAgo = subscription(2L, "Gym", Instant.now().minus(200, ChronoUnit.DAYS));
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of(usedRecently, usedLongAgo));
        when(usageLogRepository.summarizeByUserId(10L)).thenReturn(List.of(
            summary(1L, Instant.now().minus(1, ChronoUnit.DAYS), 5L),
            summary(2L, Instant.now().minus(90, ChronoUnit.DAYS), 1L)
        ));

        List<UsageInsightResponse> insights = service.insights(10L);

        assertThat(insights).extracting(UsageInsightResponse::subscriptionId).containsExactly(2L, 1L);
    }
}
