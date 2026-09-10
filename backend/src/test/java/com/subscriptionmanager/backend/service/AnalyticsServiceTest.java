package com.subscriptionmanager.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.subscriptionmanager.backend.dto.analytics.AnalyticsResponse;
import com.subscriptionmanager.backend.dto.analytics.MonthlySpendPoint;
import com.subscriptionmanager.backend.dto.analytics.TopSubscriptionResponse;
import com.subscriptionmanager.backend.entity.Payment;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.enums.BillingCycle;
import com.subscriptionmanager.backend.entity.enums.PaymentStatus;
import com.subscriptionmanager.backend.entity.enums.SubscriptionStatus;
import com.subscriptionmanager.backend.repository.PaymentRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        CostNormalizationService costNormalizationService = new CostNormalizationService();
        service = new AnalyticsService(
            subscriptionRepository, paymentRepository, costNormalizationService,
            new CategoryBreakdownService(costNormalizationService));
        lenient().when(paymentRepository.findBySubscriptionUserIdAndStatusAndDeletedAtIsNullAndPaymentDateGreaterThanEqual(
            eq(1L), eq(PaymentStatus.SUCCESS), any())).thenReturn(List.of());
    }

    private Subscription subscription(long id, String name, BigDecimal price, SubscriptionStatus status) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setName(name);
        subscription.setPrice(price);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setCurrency("USD");
        subscription.setStatus(status);
        return subscription;
    }

    @Test
    void totalsOnlyIncludeActiveSubscriptions() {
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of(
            subscription(1L, "Netflix", new BigDecimal("10.00"), SubscriptionStatus.ACTIVE),
            subscription(2L, "Old", new BigDecimal("999.00"), SubscriptionStatus.CANCELLED)
        ));

        AnalyticsResponse result = service.getAnalytics(1L);

        assertThat(result.yearlyTotal()).isEqualByComparingTo("120.00");
    }

    @Test
    void mostExpensiveIsSortedDescendingAndLimitedToTen() {
        List<Subscription> subs = List.of(
            subscription(1L, "Cheap", new BigDecimal("5.00"), SubscriptionStatus.ACTIVE),
            subscription(2L, "Expensive", new BigDecimal("50.00"), SubscriptionStatus.ACTIVE),
            subscription(3L, "Mid", new BigDecimal("20.00"), SubscriptionStatus.ACTIVE)
        );
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(subs);

        AnalyticsResponse result = service.getAnalytics(1L);

        assertThat(result.mostExpensive()).extracting(TopSubscriptionResponse::name)
            .containsExactly("Expensive", "Mid", "Cheap");
    }

    @Test
    void monthlyTrendCoversTwelveMonthsEndingWithTheCurrentMonthAndFillsGapsWithZero() {
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of());

        AnalyticsResponse result = service.getAnalytics(1L);

        assertThat(result.monthlyTrend()).hasSize(12);
        assertThat(result.monthlyTrend().get(11).month()).isEqualTo(YearMonth.now().toString());
        assertThat(result.monthlyTrend()).allSatisfy(point ->
            assertThat(point.totalSpend()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    @Test
    void monthlyTrendSumsActualPaymentsPerMonthRegardlessOfSubscriptionStatus() {
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of());

        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("9.99"));
        payment.setPaymentDate(LocalDate.now().withDayOfMonth(1));
        when(paymentRepository.findBySubscriptionUserIdAndStatusAndDeletedAtIsNullAndPaymentDateGreaterThanEqual(
            eq(1L), eq(PaymentStatus.SUCCESS), any())).thenReturn(List.of(payment));

        AnalyticsResponse result = service.getAnalytics(1L);

        MonthlySpendPoint currentMonthPoint = result.monthlyTrend().get(result.monthlyTrend().size() - 1);
        assertThat(currentMonthPoint.totalSpend()).isEqualByComparingTo("9.99");
    }
}
