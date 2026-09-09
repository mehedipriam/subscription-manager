package com.subscriptionmanager.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subscriptionmanager.backend.dto.subscription.CategoryRequest;
import com.subscriptionmanager.backend.dto.subscription.CategoryResponse;
import com.subscriptionmanager.backend.security.UserPrincipal;
import com.subscriptionmanager.backend.service.SubscriptionCategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${api.base-path}/categories")
@RequiredArgsConstructor
public class SubscriptionCategoryController {

    private final SubscriptionCategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoryService.listForUser(principal.getId()));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity.ok(categoryService.create(principal.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id
    ) {
        categoryService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
