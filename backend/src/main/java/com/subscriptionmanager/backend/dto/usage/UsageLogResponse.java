package com.subscriptionmanager.backend.dto.usage;

import java.time.Instant;

import com.subscriptionmanager.backend.entity.UsageLog;

public record UsageLogResponse(
    Long id,
    Instant usedAt
) {
    public static UsageLogResponse from(UsageLog usageLog) {
        return new UsageLogResponse(usageLog.getId(), usageLog.getUsedAt());
    }
}
