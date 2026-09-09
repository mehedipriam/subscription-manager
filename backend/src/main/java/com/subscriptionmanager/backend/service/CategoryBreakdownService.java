package com.subscriptionmanager.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.subscriptionmanager.backend.dto.dashboard.CategorySpendResponse;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.SubscriptionCategory;

import lombok.RequiredArgsConstructor;

/**
 * Groups active subscriptions by category and normalizes their cost to a
 * monthly figure, with each category's share of the total. Shared by the
 * dashboard and analytics endpoints so both agree on the same numbers.
 */
@Service
@RequiredArgsConstructor
public class CategoryBreakdownService {

    private final CostNormalizationService costNormalizationService;

    public List<CategorySpendResponse> build(List<Subscription> activeSubscriptions, BigDecimal totalMonthlySpend) {
        Map<String, List<Subscription>> byCategory = new LinkedHashMap<>();
        for (Subscription s : activeSubscriptions) {
            String key = s.getCategory() == null ? "uncategorized" : s.getCategory().getId().toString();
            byCategory.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }

        return byCategory.values().stream()
            .map(group -> {
                SubscriptionCategory category = group.get(0).getCategory();
                BigDecimal monthlyAmount = group.stream()
                    .map(s -> costNormalizationService.toMonthly(s.getPrice(), s.getBillingCycle()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal percentage = totalMonthlySpend.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : monthlyAmount.multiply(BigDecimal.valueOf(100))
                        .divide(totalMonthlySpend, 1, RoundingMode.HALF_UP);

                return new CategorySpendResponse(
                    category == null ? null : category.getId(),
                    category == null ? "Uncategorized" : category.getName(),
                    category == null ? null : category.getIcon(),
                    category == null ? null : category.getColor(),
                    monthlyAmount,
                    percentage
                );
            })
            .sorted(Comparator.comparing(CategorySpendResponse::monthlyAmount).reversed())
            .toList();
    }
}
