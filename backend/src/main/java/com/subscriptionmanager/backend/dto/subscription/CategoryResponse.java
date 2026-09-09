package com.subscriptionmanager.backend.dto.subscription;

import com.subscriptionmanager.backend.entity.SubscriptionCategory;

public record CategoryResponse(
    Long id,
    String name,
    String icon,
    String color,
    boolean isDefault
) {
    public static CategoryResponse from(SubscriptionCategory category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getIcon(),
            category.getColor(),
            category.getUser() == null
        );
    }
}
