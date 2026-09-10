package com.subscriptionmanager.backend.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subscriptionmanager.backend.dto.calculator.CancellationSavingsResponse;
import com.subscriptionmanager.backend.dto.calculator.SavingsItemResponse;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Powers the "what if I cancel?" calculator: given a set of the user's own
 * subscriptions, reports the monthly/yearly spend that cancelling all of
 * them would save. Subscription ids the user doesn't own are silently
 * ignored rather than rejected, since this is a read-only projection, not
 * an action on those subscriptions.
 */
@Service
@RequiredArgsConstructor
public class SavingsCalculatorService {

    private final SubscriptionRepository subscriptionRepository;
    private final CostNormalizationService costNormalizationService;

    @Transactional(readOnly = true)
    public CancellationSavingsResponse calculate(Long userId, List<Long> subscriptionIds) {
        Set<Long> requestedIds = new HashSet<>(subscriptionIds);

        List<SavingsItemResponse> items = subscriptionRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
            .filter(subscription -> requestedIds.contains(subscription.getId()))
            .map(subscription -> new SavingsItemResponse(
                subscription.getId(),
                subscription.getName(),
                costNormalizationService.toMonthly(subscription.getPrice(), subscription.getBillingCycle()),
                costNormalizationService.toYearly(subscription.getPrice(), subscription.getBillingCycle()),
                subscription.getCurrency()))
            .toList();

        BigDecimal monthlySavings = items.stream()
            .map(SavingsItemResponse::monthlyCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal yearlySavings = items.stream()
            .map(SavingsItemResponse::yearlyCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CancellationSavingsResponse(monthlySavings, yearlySavings, items);
    }
}
