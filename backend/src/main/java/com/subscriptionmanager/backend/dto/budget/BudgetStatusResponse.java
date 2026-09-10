package com.subscriptionmanager.backend.dto.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import com.subscriptionmanager.backend.entity.Budget;

public record BudgetStatusResponse(
    LocalDate periodMonth,
    BigDecimal budgetAmount,
    BigDecimal projectedSpend,
    BigDecimal remaining,
    BigDecimal percentageUsed,
    boolean exceeded
) {
    public static BudgetStatusResponse from(LocalDate periodMonth, Budget budget, BigDecimal projectedSpend) {
        BigDecimal amount = budget != null ? budget.getAmount() : null;

        BigDecimal remaining = amount != null ? amount.subtract(projectedSpend) : null;
        BigDecimal percentageUsed = amount != null && amount.compareTo(BigDecimal.ZERO) > 0
            ? projectedSpend.divide(amount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
            : null;
        boolean exceeded = amount != null && projectedSpend.compareTo(amount) > 0;

        return new BudgetStatusResponse(periodMonth, amount, projectedSpend, remaining, percentageUsed, exceeded);
    }
}
