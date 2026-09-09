package com.subscriptionmanager.backend.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

import com.subscriptionmanager.backend.dto.dashboard.CategorySpendResponse;

public record AnalyticsResponse(
    List<MonthlySpendPoint> monthlyTrend,
    BigDecimal yearlyTotal,
    List<CategorySpendResponse> categoryBreakdown,
    List<TopSubscriptionResponse> mostExpensive
) {
}
