package com.subscriptionmanager.backend.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subscriptionmanager.backend.dto.subscription.SubscriptionRequest;
import com.subscriptionmanager.backend.dto.subscription.SubscriptionResponse;
import com.subscriptionmanager.backend.entity.PriceHistory;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.SubscriptionCategory;
import com.subscriptionmanager.backend.entity.User;
import com.subscriptionmanager.backend.entity.enums.BillingCycle;
import com.subscriptionmanager.backend.entity.enums.SubscriptionStatus;
import com.subscriptionmanager.backend.exception.ResourceNotFoundException;
import com.subscriptionmanager.backend.repository.PriceHistoryRepository;
import com.subscriptionmanager.backend.repository.SubscriptionCategoryRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;
import com.subscriptionmanager.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> listForUser(Long userId) {
        return subscriptionRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
            .map(SubscriptionResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getForUser(Long userId, Long subscriptionId) {
        return SubscriptionResponse.from(findOwned(userId, subscriptionId));
    }

    @Transactional
    public SubscriptionResponse create(Long userId, SubscriptionRequest request) {
        User user = userRepository.getReferenceById(userId);

        Subscription subscription = new Subscription();
        subscription.setUser(user);
        applyRequest(subscription, userId, request);

        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @Transactional
    public SubscriptionResponse update(Long userId, Long subscriptionId, SubscriptionRequest request) {
        Subscription subscription = findOwned(userId, subscriptionId);
        BigDecimal previousPrice = subscription.getPrice();
        applyRequest(subscription, userId, request);

        if (previousPrice.compareTo(subscription.getPrice()) != 0) {
            PriceHistory priceHistory = new PriceHistory();
            priceHistory.setSubscription(subscription);
            priceHistory.setOldPrice(previousPrice);
            priceHistory.setNewPrice(subscription.getPrice());
            priceHistory.setChangedAt(Instant.now());
            priceHistoryRepository.save(priceHistory);
        }

        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @Transactional
    public void delete(Long userId, Long subscriptionId) {
        Subscription subscription = findOwned(userId, subscriptionId);
        subscription.setDeletedAt(Instant.now());
        subscriptionRepository.save(subscription);
    }

    private Subscription findOwned(Long userId, Long subscriptionId) {
        return subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(subscriptionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }

    private void applyRequest(Subscription subscription, Long userId, SubscriptionRequest request) {
        subscription.setName(request.name());
        subscription.setDescription(request.description());
        subscription.setPrice(request.price());
        subscription.setCurrency(request.currency() == null || request.currency().isBlank()
            ? "USD" : request.currency().toUpperCase());
        subscription.setBillingCycle(request.billingCycle());
        subscription.setStartDate(request.startDate());
        subscription.setNextBillingDate(request.nextBillingDate() != null
            ? request.nextBillingDate()
            : computeNextBillingDate(request.startDate(), request.billingCycle()));
        subscription.setStatus(request.status() != null ? request.status() : SubscriptionStatus.ACTIVE);
        subscription.setTrial(Boolean.TRUE.equals(request.isTrial()));
        subscription.setTrialEndDate(request.trialEndDate());
        subscription.setCancelUrl(request.cancelUrl());
        subscription.setCancellationInstructions(request.cancellationInstructions());
        subscription.setCategory(resolveCategory(userId, request.categoryId()));
    }

    private SubscriptionCategory resolveCategory(Long userId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        SubscriptionCategory category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        boolean isDefault = category.getUser() == null;
        boolean ownedByUser = category.getUser() != null && category.getUser().getId().equals(userId);
        if (!isDefault && !ownedByUser) {
            throw new ResourceNotFoundException("Category not found");
        }
        return category;
    }

    private LocalDate computeNextBillingDate(LocalDate startDate, BillingCycle cycle) {
        LocalDate next = startDate;
        LocalDate today = LocalDate.now();
        while (next.isBefore(today)) {
            next = advance(next, cycle);
        }
        return next;
    }

    private LocalDate advance(LocalDate date, BillingCycle cycle) {
        return switch (cycle) {
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
            case YEARLY -> date.plusYears(1);
        };
    }
}
