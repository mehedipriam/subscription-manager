package com.subscriptionmanager.backend.dto.budget;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record BudgetRequest(
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Amount cannot be negative")
    BigDecimal amount,

    LocalDate periodMonth
) {
}
