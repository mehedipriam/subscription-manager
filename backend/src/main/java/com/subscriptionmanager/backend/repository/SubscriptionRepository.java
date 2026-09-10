package com.subscriptionmanager.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.enums.SubscriptionStatus;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<Subscription> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    boolean existsByCategoryIdAndDeletedAtIsNull(Long categoryId);

    @Query("select s from Subscription s where s.status = :status and s.deletedAt is null "
        + "and s.nextBillingDate between :from and :to")
    List<Subscription> findActiveRenewingBetween(
        @Param("status") SubscriptionStatus status, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select s from Subscription s where s.status = :status and s.deletedAt is null "
        + "and s.isTrial = true and s.trialEndDate between :from and :to")
    List<Subscription> findActiveTrialsEndingBetween(
        @Param("status") SubscriptionStatus status, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Soft-deleted subscriptions still hold a foreign key to their category,
     * which would otherwise block deleting that category. Called only after
     * confirming no active subscription references it.
     */
    @Modifying
    @Query("update Subscription s set s.category = null where s.category.id = :categoryId")
    void clearCategoryReferences(@Param("categoryId") Long categoryId);
}
