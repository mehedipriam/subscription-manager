package com.subscriptionmanager.backend.dto.auth;

import java.time.Instant;

import com.subscriptionmanager.backend.entity.User;

public record UserResponse(
    Long id,
    String email,
    String fullName,
    Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getCreatedAt());
    }
}
