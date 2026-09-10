package com.subscriptionmanager.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.subscriptionmanager.backend.entity.UsageLog;

public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {

    List<UsageLog> findBySubscriptionIdOrderByUsedAtDesc(Long subscriptionId);

    @Query("select ul.subscription.id as subscriptionId, max(ul.usedAt) as lastUsedAt, count(ul) as usageCount "
        + "from UsageLog ul where ul.subscription.user.id = :userId group by ul.subscription.id")
    List<UsageSummaryProjection> summarizeByUserId(@Param("userId") Long userId);
}
