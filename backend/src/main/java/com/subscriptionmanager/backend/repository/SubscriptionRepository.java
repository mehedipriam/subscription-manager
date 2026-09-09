package com.subscriptionmanager.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.subscriptionmanager.backend.entity.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<Subscription> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    boolean existsByCategoryIdAndDeletedAtIsNull(Long categoryId);

    /**
     * Soft-deleted subscriptions still hold a foreign key to their category,
     * which would otherwise block deleting that category. Called only after
     * confirming no active subscription references it.
     */
    @Modifying
    @Query("update Subscription s set s.category = null where s.category.id = :categoryId")
    void clearCategoryReferences(@Param("categoryId") Long categoryId);
}
