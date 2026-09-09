package com.subscriptionmanager.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.subscriptionmanager.backend.entity.enums.BillingCycle;

/**
 * Converts a subscription's price + billing cycle into normalized
 * monthly/yearly figures so subscriptions on different cycles can be
 * compared and summed on equal footing. Shared by the dashboard and
 * analytics endpoints.
 */
@Service
public class CostNormalizationService {

    private static final int SCALE = 2;
    private static final BigDecimal WEEKS_PER_YEAR = BigDecimal.valueOf(52);
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal MONTHS_PER_QUARTER = BigDecimal.valueOf(3);
    private static final BigDecimal QUARTERS_PER_YEAR = BigDecimal.valueOf(4);

    public BigDecimal toMonthly(BigDecimal price, BillingCycle cycle) {
        return switch (cycle) {
            case WEEKLY -> price.multiply(WEEKS_PER_YEAR).divide(MONTHS_PER_YEAR, SCALE, RoundingMode.HALF_UP);
            case MONTHLY -> price.setScale(SCALE, RoundingMode.HALF_UP);
            case QUARTERLY -> price.divide(MONTHS_PER_QUARTER, SCALE, RoundingMode.HALF_UP);
            case YEARLY -> price.divide(MONTHS_PER_YEAR, SCALE, RoundingMode.HALF_UP);
        };
    }

    public BigDecimal toYearly(BigDecimal price, BillingCycle cycle) {
        return switch (cycle) {
            case WEEKLY -> price.multiply(WEEKS_PER_YEAR).setScale(SCALE, RoundingMode.HALF_UP);
            case MONTHLY -> price.multiply(MONTHS_PER_YEAR).setScale(SCALE, RoundingMode.HALF_UP);
            case QUARTERLY -> price.multiply(QUARTERS_PER_YEAR).setScale(SCALE, RoundingMode.HALF_UP);
            case YEARLY -> price.setScale(SCALE, RoundingMode.HALF_UP);
        };
    }
}
