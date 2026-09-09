package com.subscriptionmanager.backend.dto.dashboard;

import java.math.BigDecimal;

public record CategorySpendResponse(
    Long categoryId,
    String categoryName,
    String icon,
    String color,
    BigDecimal monthlyAmount,
    BigDecimal percentage
) {
}
