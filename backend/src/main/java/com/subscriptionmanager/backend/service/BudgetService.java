package com.subscriptionmanager.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subscriptionmanager.backend.dto.budget.BudgetRequest;
import com.subscriptionmanager.backend.dto.budget.BudgetResponse;
import com.subscriptionmanager.backend.dto.budget.BudgetStatusResponse;
import com.subscriptionmanager.backend.entity.Budget;
import com.subscriptionmanager.backend.entity.enums.SubscriptionStatus;
import com.subscriptionmanager.backend.repository.BudgetRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;
import com.subscriptionmanager.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final CostNormalizationService costNormalizationService;

    @Transactional(readOnly = true)
    public BudgetStatusResponse getStatus(Long userId, LocalDate forDate) {
        return getStatus(userId, forDate, projectedMonthlySpend(userId));
    }

    /**
     * Same as {@link #getStatus(Long, LocalDate)} but takes an
     * already-computed projected spend, for callers (e.g. the dashboard)
     * that have already summed the user's active subscriptions and would
     * otherwise trigger a redundant query.
     */
    @Transactional(readOnly = true)
    public BudgetStatusResponse getStatus(Long userId, LocalDate forDate, BigDecimal projectedSpend) {
        LocalDate periodMonth = forDate.withDayOfMonth(1);
        Budget budget = budgetRepository.findByUserIdAndPeriodMonth(userId, periodMonth).orElse(null);
        return BudgetStatusResponse.from(periodMonth, budget, projectedSpend);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> history(Long userId) {
        return budgetRepository.findByUserIdOrderByPeriodMonthDesc(userId).stream()
            .map(BudgetResponse::from)
            .toList();
    }

    @Transactional
    public BudgetResponse upsert(Long userId, BudgetRequest request) {
        LocalDate periodMonth = (request.periodMonth() != null ? request.periodMonth() : LocalDate.now())
            .withDayOfMonth(1);

        Budget budget = budgetRepository.findByUserIdAndPeriodMonth(userId, periodMonth)
            .orElseGet(() -> {
                Budget created = new Budget();
                created.setUser(userRepository.getReferenceById(userId));
                created.setPeriodMonth(periodMonth);
                return created;
            });
        budget.setAmount(request.amount());

        return BudgetResponse.from(budgetRepository.save(budget));
    }

    /**
     * The user's current recurring monthly cost, normalized across billing
     * cycles. Used both to report budget status and, by the reminder
     * scheduler, to detect when a budget has been exceeded.
     */
    @Transactional(readOnly = true)
    public BigDecimal projectedMonthlySpend(Long userId) {
        return subscriptionRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
            .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
            .map(s -> costNormalizationService.toMonthly(s.getPrice(), s.getBillingCycle()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
