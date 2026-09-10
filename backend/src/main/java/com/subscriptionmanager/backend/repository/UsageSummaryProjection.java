package com.subscriptionmanager.backend.repository;

import java.time.Instant;

public interface UsageSummaryProjection {
    Long getSubscriptionId();

    Instant getLastUsedAt();

    Long getUsageCount();
}
