package com.subscriptionmanager.backend.dto.calculator;

import java.math.BigDecimal;
import java.util.List;

public record CancellationSavingsResponse(
    BigDecimal monthlySavings,
    BigDecimal yearlySavings,
    List<SavingsItemResponse> subscriptions
) {
}
