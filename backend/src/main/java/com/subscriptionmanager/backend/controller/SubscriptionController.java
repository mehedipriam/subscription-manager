package com.subscriptionmanager.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subscriptionmanager.backend.dto.subscription.SubscriptionRequest;
import com.subscriptionmanager.backend.dto.subscription.SubscriptionResponse;
import com.subscriptionmanager.backend.security.UserPrincipal;
import com.subscriptionmanager.backend.service.SubscriptionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${api.base-path}/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(subscriptionService.listForUser(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> get(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(subscriptionService.getForUser(principal.getId(), id));
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody SubscriptionRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.create(principal.getId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> update(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @Valid @RequestBody SubscriptionRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.update(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id
    ) {
        subscriptionService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
