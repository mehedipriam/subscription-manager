package com.subscriptionmanager.backend.dto.calculator;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record CancellationSavingsRequest(
    @NotEmpty(message = "At least one subscription must be selected")
    List<Long> subscriptionIds
) {
}
