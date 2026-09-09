package com.subscriptionmanager.backend.dto.analytics;

import java.math.BigDecimal;

public record MonthlySpendPoint(
    String month,
    BigDecimal totalSpend
) {
}
