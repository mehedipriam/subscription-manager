package com.subscriptionmanager.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.subscriptionmanager.backend.entity.enums.BillingCycle;

class CostNormalizationServiceTest {

    private final CostNormalizationService service = new CostNormalizationService();

    @Test
    void monthlyCycleIsUnchangedForMonthlyFigure() {
        assertThat(service.toMonthly(new BigDecimal("9.99"), BillingCycle.MONTHLY))
            .isEqualByComparingTo("9.99");
    }

    @Test
    void monthlyCycleTimesTwelveForYearlyFigure() {
        assertThat(service.toYearly(new BigDecimal("9.99"), BillingCycle.MONTHLY))
            .isEqualByComparingTo("119.88");
    }

    @Test
    void yearlyCycleDividedByTwelveForMonthlyFigure() {
        assertThat(service.toMonthly(new BigDecimal("120.00"), BillingCycle.YEARLY))
            .isEqualByComparingTo("10.00");
    }

    @Test
    void yearlyCycleIsUnchangedForYearlyFigure() {
        assertThat(service.toYearly(new BigDecimal("99.00"), BillingCycle.YEARLY))
            .isEqualByComparingTo("99.00");
    }

    @Test
    void quarterlyCycleDividedByThreeForMonthlyFigure() {
        assertThat(service.toMonthly(new BigDecimal("30.00"), BillingCycle.QUARTERLY))
            .isEqualByComparingTo("10.00");
    }

    @Test
    void quarterlyCycleTimesFourForYearlyFigure() {
        assertThat(service.toYearly(new BigDecimal("30.00"), BillingCycle.QUARTERLY))
            .isEqualByComparingTo("120.00");
    }

    @Test
    void weeklyCycleConvertsThroughFiftyTwoWeeksForMonthlyFigure() {
        // 5.00 * 52 / 12 = 21.666... -> rounds to 21.67
        assertThat(service.toMonthly(new BigDecimal("5.00"), BillingCycle.WEEKLY))
            .isEqualByComparingTo("21.67");
    }

    @Test
    void weeklyCycleTimesFiftyTwoForYearlyFigure() {
        assertThat(service.toYearly(new BigDecimal("5.00"), BillingCycle.WEEKLY))
            .isEqualByComparingTo("260.00");
    }

    @Test
    void roundsHalfUpRatherThanTruncating() {
        // 10 / 3 = 3.3333... -> should round to 3.33, not truncate to 3.34/3.32
        assertThat(service.toMonthly(new BigDecimal("10.00"), BillingCycle.QUARTERLY))
            .isEqualByComparingTo("3.33");
    }
}
