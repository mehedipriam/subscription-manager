package com.subscriptionmanager.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.subscriptionmanager.backend.entity.PriceHistory;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findBySubscriptionIdOrderByChangedAtDesc(Long subscriptionId);

    List<PriceHistory> findTop10BySubscriptionUserIdOrderByChangedAtDesc(Long userId);
}
