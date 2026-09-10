package com.subscriptionmanager.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.subscriptionmanager.backend.dto.calculator.CancellationSavingsResponse;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.enums.BillingCycle;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class SavingsCalculatorServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private SavingsCalculatorService service;

    @BeforeEach
    void setUp() {
        service = new SavingsCalculatorService(subscriptionRepository, new CostNormalizationService());
    }

    private Subscription subscription(long id, String name, BigDecimal price, BillingCycle cycle) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setName(name);
        subscription.setPrice(price);
        subscription.setBillingCycle(cycle);
        subscription.setCurrency("USD");
        return subscription;
    }

    @Test
    void sumsMonthlyAndYearlyCostForRequestedSubscriptions() {
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of(
            subscription(1L, "Netflix", new BigDecimal("10.00"), BillingCycle.MONTHLY),
            subscription(2L, "Spotify", new BigDecimal("5.00"), BillingCycle.MONTHLY),
            subscription(3L, "Gym", new BigDecimal("30.00"), BillingCycle.MONTHLY)
        ));

        CancellationSavingsResponse result = service.calculate(1L, List.of(1L, 2L));

        assertThat(result.monthlySavings()).isEqualByComparingTo("15.00");
        assertThat(result.yearlySavings()).isEqualByComparingTo("180.00");
        assertThat(result.subscriptions()).extracting("subscriptionId").containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void ignoresSubscriptionIdsNotOwnedByTheUser() {
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of(
            subscription(1L, "Netflix", new BigDecimal("10.00"), BillingCycle.MONTHLY)
        ));

        CancellationSavingsResponse result = service.calculate(1L, List.of(1L, 999L));

        assertThat(result.subscriptions()).hasSize(1);
        assertThat(result.monthlySavings()).isEqualByComparingTo("10.00");
    }

    @Test
    void emptySelectionProducesZeroSavings() {
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of(
            subscription(1L, "Netflix", new BigDecimal("10.00"), BillingCycle.MONTHLY)
        ));

        CancellationSavingsResponse result = service.calculate(1L, List.of());

        assertThat(result.subscriptions()).isEmpty();
        assertThat(result.monthlySavings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.yearlySavings()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
