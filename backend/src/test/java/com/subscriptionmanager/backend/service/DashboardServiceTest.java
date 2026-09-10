package com.subscriptionmanager.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.subscriptionmanager.backend.dto.budget.BudgetStatusResponse;
import com.subscriptionmanager.backend.dto.dashboard.DashboardResponse;
import com.subscriptionmanager.backend.dto.usage.UsageInsightResponse;
import com.subscriptionmanager.backend.entity.Payment;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.enums.BillingCycle;
import com.subscriptionmanager.backend.entity.enums.SubscriptionStatus;
import com.subscriptionmanager.backend.repository.PaymentRepository;
import com.subscriptionmanager.backend.repository.PriceHistoryRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PriceHistoryRepository priceHistoryRepository;
    @Mock
    private BudgetService budgetService;
    @Mock
    private UsageService usageService;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        CostNormalizationService costNormalizationService = new CostNormalizationService();
        service = new DashboardService(
            subscriptionRepository, paymentRepository, priceHistoryRepository,
            costNormalizationService, new CategoryBreakdownService(costNormalizationService),
            budgetService, usageService);

        when(priceHistoryRepository.findTop10BySubscriptionUserIdOrderByChangedAtDesc(1L)).thenReturn(List.of());
        when(paymentRepository.findTop10BySubscriptionUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(1L))
            .thenReturn(List.of());
        when(budgetService.getStatus(eq(1L), any(), any())).thenReturn(
            BudgetStatusResponse.from(LocalDate.now().withDayOfMonth(1), null, BigDecimal.ZERO));
        when(usageService.insights(1L)).thenReturn(List.of());
    }

    private Subscription subscription(long id, String name, LocalDate nextBillingDate, SubscriptionStatus status) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setName(name);
        subscription.setPrice(new BigDecimal("10.00"));
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setCurrency("USD");
        subscription.setStatus(status);
        subscription.setNextBillingDate(nextBillingDate);
        subscription.setCreatedAt(Instant.now());
        return subscription;
    }

    @Test
    void upcomingRenewalsCountOnlyCountsWithinSevenDays() {
        LocalDate today = LocalDate.now();
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of(
            subscription(1L, "Soon", today.plusDays(3), SubscriptionStatus.ACTIVE),
            subscription(2L, "Later", today.plusDays(20), SubscriptionStatus.ACTIVE)
        ));

        DashboardResponse result = service.getSummary(1L);

        assertThat(result.upcomingRenewalsCount()).isEqualTo(1);
        assertThat(result.upcomingPayments()).hasSize(2);
    }

    @Test
    void upcomingPaymentsExcludeRenewalsBeyondThirtyDays() {
        LocalDate today = LocalDate.now();
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of(
            subscription(1L, "Soon", today.plusDays(10), SubscriptionStatus.ACTIVE),
            subscription(2L, "TooFar", today.plusDays(45), SubscriptionStatus.ACTIVE)
        ));

        DashboardResponse result = service.getSummary(1L);

        assertThat(result.upcomingPayments()).hasSize(1);
    }

    @Test
    void inactiveSubscriptionsAreExcludedFromTotalsAndCounts() {
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of(
            subscription(1L, "Active", LocalDate.now().plusDays(5), SubscriptionStatus.ACTIVE),
            subscription(2L, "Paused", LocalDate.now().plusDays(5), SubscriptionStatus.PAUSED)
        ));

        DashboardResponse result = service.getSummary(1L);

        assertThat(result.activeSubscriptionCount()).isEqualTo(1);
        assertThat(result.totalMonthlySpend()).isEqualByComparingTo("10.00");
    }

    @Test
    void recentActivityMergesSubscriptionsAndPaymentsSortedByTimestampDescending() {
        Subscription subscription = subscription(1L, "Netflix", LocalDate.now().plusDays(5), SubscriptionStatus.ACTIVE);
        subscription.setCreatedAt(Instant.now().minusSeconds(3600));
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of(subscription));

        Payment payment = new Payment();
        payment.setSubscription(subscription);
        payment.setAmount(new BigDecimal("9.99"));
        payment.setCurrency("USD");
        payment.setCreatedAt(Instant.now());
        when(paymentRepository.findTop10BySubscriptionUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(1L))
            .thenReturn(List.of(payment));

        DashboardResponse result = service.getSummary(1L);

        assertThat(result.recentActivity()).hasSize(2);
        assertThat(result.recentActivity().get(0).type()).isEqualTo("PAYMENT_RECORDED");
        assertThat(result.recentActivity().get(1).type()).isEqualTo("SUBSCRIPTION_ADDED");
    }

    @Test
    void usageRecommendationsOnlyIncludeRarelyUsedSortedByCostDescendingLimitedToFive() {
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of());
        when(usageService.insights(1L)).thenReturn(List.of(
            insight(1L, "Cheap", new BigDecimal("5.00"), true),
            insight(2L, "NotRarelyUsed", new BigDecimal("100.00"), false),
            insight(3L, "Expensive", new BigDecimal("50.00"), true)
        ));

        DashboardResponse result = service.getSummary(1L);

        assertThat(result.usageRecommendations()).extracting(UsageInsightResponse::subscriptionId)
            .containsExactly(3L, 1L);
    }

    private UsageInsightResponse insight(long id, String name, BigDecimal monthlyCost, boolean rarelyUsed) {
        return new UsageInsightResponse(id, name, monthlyCost, "USD", null, 40, 0, rarelyUsed);
    }
}
