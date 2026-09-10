package com.subscriptionmanager.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subscriptionmanager.backend.dto.subscription.PriceHistoryResponse;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.exception.ResourceNotFoundException;
import com.subscriptionmanager.backend.repository.PriceHistoryRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> listForSubscription(Long userId, Long subscriptionId) {
        Subscription subscription = findOwnedSubscription(userId, subscriptionId);
        return priceHistoryRepository.findBySubscriptionIdOrderByChangedAtDesc(subscription.getId())
            .stream()
            .map(PriceHistoryResponse::from)
            .toList();
    }

    private Subscription findOwnedSubscription(Long userId, Long subscriptionId) {
        return subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(subscriptionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }
}
