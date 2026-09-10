package com.subscriptionmanager.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subscriptionmanager.backend.dto.dashboard.ActivityItemResponse;
import com.subscriptionmanager.backend.dto.dashboard.CategorySpendResponse;
import com.subscriptionmanager.backend.dto.dashboard.DashboardResponse;
import com.subscriptionmanager.backend.dto.dashboard.PriceChangeResponse;
import com.subscriptionmanager.backend.dto.dashboard.UpcomingPaymentResponse;
import com.subscriptionmanager.backend.entity.Payment;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.enums.SubscriptionStatus;
import com.subscriptionmanager.backend.repository.PaymentRepository;
import com.subscriptionmanager.backend.repository.PriceHistoryRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int UPCOMING_WINDOW_DAYS = 30;
    private static final int RENEWAL_SOON_DAYS = 7;
    private static final int RECENT_ACTIVITY_LIMIT = 10;

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final CostNormalizationService costNormalizationService;
    private final CategoryBreakdownService categoryBreakdownService;

    @Transactional(readOnly = true)
    public DashboardResponse getSummary(Long userId) {
        List<Subscription> subscriptions = subscriptionRepository.findByUserIdAndDeletedAtIsNull(userId);
        List<Subscription> active = subscriptions.stream()
            .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
            .toList();

        BigDecimal totalMonthlySpend = active.stream()
            .map(s -> costNormalizationService.toMonthly(s.getPrice(), s.getBillingCycle()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalYearlySpend = active.stream()
            .map(s -> costNormalizationService.toYearly(s.getPrice(), s.getBillingCycle()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();
        LocalDate renewalSoonCutoff = today.plusDays(RENEWAL_SOON_DAYS);
        LocalDate upcomingCutoff = today.plusDays(UPCOMING_WINDOW_DAYS);

        int upcomingRenewalsCount = (int) active.stream()
            .filter(s -> isWithin(s.getNextBillingDate(), today, renewalSoonCutoff))
            .count();

        List<UpcomingPaymentResponse> upcomingPayments = active.stream()
            .filter(s -> isWithin(s.getNextBillingDate(), today, upcomingCutoff))
            .sorted(Comparator.comparing(Subscription::getNextBillingDate))
            .limit(RECENT_ACTIVITY_LIMIT)
            .map(UpcomingPaymentResponse::from)
            .toList();

        List<CategorySpendResponse> categoryBreakdown = categoryBreakdownService.build(active, totalMonthlySpend);

        List<ActivityItemResponse> recentActivity = buildRecentActivity(userId, subscriptions);

        List<PriceChangeResponse> recentPriceChanges = priceHistoryRepository
            .findTop10BySubscriptionUserIdOrderByChangedAtDesc(userId).stream()
            .map(PriceChangeResponse::from)
            .toList();

        return new DashboardResponse(
            totalMonthlySpend,
            totalYearlySpend,
            active.size(),
            upcomingRenewalsCount,
            upcomingPayments,
            categoryBreakdown,
            recentActivity,
            recentPriceChanges
        );
    }

    private boolean isWithin(LocalDate date, LocalDate from, LocalDate to) {
        return date != null && !date.isBefore(from) && !date.isAfter(to);
    }

    private List<ActivityItemResponse> buildRecentActivity(Long userId, List<Subscription> subscriptions) {
        Stream<ActivityItemResponse> subscriptionActivity = subscriptions.stream()
            .sorted(Comparator.comparing(Subscription::getCreatedAt).reversed())
            .limit(RECENT_ACTIVITY_LIMIT)
            .map(s -> ActivityItemResponse.subscriptionAdded(s.getId(), s.getName(), s.getCreatedAt()));

        List<Payment> recentPayments = paymentRepository
            .findTop10BySubscriptionUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        Stream<ActivityItemResponse> paymentActivity = recentPayments.stream()
            .map(p -> ActivityItemResponse.paymentRecorded(
                p.getSubscription().getId(), p.getSubscription().getName(), p.getCreatedAt(), p.getAmount(), p.getCurrency()));

        return Stream.concat(subscriptionActivity, paymentActivity)
            .sorted(Comparator.comparing(ActivityItemResponse::timestamp).reversed())
            .limit(RECENT_ACTIVITY_LIMIT)
            .toList();
    }
}
