package com.subscriptionmanager.backend.dto.budget;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.subscriptionmanager.backend.entity.Budget;

public record BudgetResponse(
    Long id,
    LocalDate periodMonth,
    BigDecimal amount
) {
    public static BudgetResponse from(Budget budget) {
        return new BudgetResponse(budget.getId(), budget.getPeriodMonth(), budget.getAmount());
    }
}
