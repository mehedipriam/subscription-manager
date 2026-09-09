package com.subscriptionmanager.backend.dto.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    String name,

    @Size(max = 50, message = "Icon must be at most 50 characters")
    String icon,

    @Size(max = 20, message = "Color must be at most 20 characters")
    String color
) {
}
