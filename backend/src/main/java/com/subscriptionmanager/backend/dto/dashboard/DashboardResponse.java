package com.subscriptionmanager.backend.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

import com.subscriptionmanager.backend.dto.budget.BudgetStatusResponse;

public record DashboardResponse(
    BigDecimal totalMonthlySpend,
    BigDecimal totalYearlySpend,
    int activeSubscriptionCount,
    int upcomingRenewalsCount,
    List<UpcomingPaymentResponse> upcomingPayments,
    List<CategorySpendResponse> categoryBreakdown,
    List<ActivityItemResponse> recentActivity,
    List<PriceChangeResponse> recentPriceChanges,
    BudgetStatusResponse budgetStatus
) {
}
