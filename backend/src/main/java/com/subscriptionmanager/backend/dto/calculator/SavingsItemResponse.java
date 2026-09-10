package com.subscriptionmanager.backend.dto.calculator;

import java.math.BigDecimal;

public record SavingsItemResponse(
    Long subscriptionId,
    String name,
    BigDecimal monthlyCost,
    BigDecimal yearlyCost,
    String currency
) {
}
