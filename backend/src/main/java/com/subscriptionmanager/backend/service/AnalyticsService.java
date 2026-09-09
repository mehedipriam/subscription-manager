package com.subscriptionmanager.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subscriptionmanager.backend.dto.analytics.AnalyticsResponse;
import com.subscriptionmanager.backend.dto.analytics.MonthlySpendPoint;
import com.subscriptionmanager.backend.dto.analytics.TopSubscriptionResponse;
import com.subscriptionmanager.backend.dto.dashboard.CategorySpendResponse;
import com.subscriptionmanager.backend.dto.subscription.CategoryResponse;
import com.subscriptionmanager.backend.entity.Payment;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.enums.PaymentStatus;
import com.subscriptionmanager.backend.entity.enums.SubscriptionStatus;
import com.subscriptionmanager.backend.repository.PaymentRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int TREND_MONTHS = 12;
    private static final int TOP_EXPENSIVE_LIMIT = 10;

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final CostNormalizationService costNormalizationService;
    private final CategoryBreakdownService categoryBreakdownService;

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(Long userId) {
        List<Subscription> active = subscriptionRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
            .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
            .toList();

        BigDecimal totalMonthlySpend = active.stream()
            .map(s -> costNormalizationService.toMonthly(s.getPrice(), s.getBillingCycle()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal yearlyTotal = active.stream()
            .map(s -> costNormalizationService.toYearly(s.getPrice(), s.getBillingCycle()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MonthlySpendPoint> monthlyTrend = buildMonthlyTrend(userId);
        List<CategorySpendResponse> categoryBreakdown = categoryBreakdownService.build(active, totalMonthlySpend);
        List<TopSubscriptionResponse> mostExpensive = buildMostExpensive(active);

        return new AnalyticsResponse(monthlyTrend, yearlyTotal, categoryBreakdown, mostExpensive);
    }

    private List<TopSubscriptionResponse> buildMostExpensive(List<Subscription> active) {
        return active.stream()
            .map(s -> new TopSubscriptionResponse(
                s.getId(),
                s.getName(),
                s.getCategory() == null ? null : CategoryResponse.from(s.getCategory()),
                costNormalizationService.toMonthly(s.getPrice(), s.getBillingCycle()),
                s.getCurrency()
            ))
            .sorted(Comparator.comparing(TopSubscriptionResponse::normalizedMonthlyCost).reversed())
            .limit(TOP_EXPENSIVE_LIMIT)
            .toList();
    }

    /**
     * Actual spend per month, from recorded successful payments — unlike the
     * rest of analytics, this can't be derived from normalization alone
     * since it reflects history, not a point-in-time snapshot. Fills every
     * month in the window with 0 even if nothing was paid that month.
     */
    private List<MonthlySpendPoint> buildMonthlyTrend(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(TREND_MONTHS - 1L);
        LocalDate since = startMonth.atDay(1);

        List<Payment> payments = paymentRepository
            .findBySubscriptionUserIdAndStatusAndDeletedAtIsNullAndPaymentDateGreaterThanEqual(
                userId, PaymentStatus.SUCCESS, since);

        Map<YearMonth, BigDecimal> totalsByMonth = payments.stream()
            .collect(Collectors.groupingBy(
                p -> YearMonth.from(p.getPaymentDate()),
                Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
            ));

        List<MonthlySpendPoint> points = new ArrayList<>(TREND_MONTHS);
        for (int i = 0; i < TREND_MONTHS; i++) {
            YearMonth month = startMonth.plusMonths(i);
            BigDecimal total = totalsByMonth.getOrDefault(month, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            points.add(new MonthlySpendPoint(month.toString(), total));
        }
        return points;
    }
}
