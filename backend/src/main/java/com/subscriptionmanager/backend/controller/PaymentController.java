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

import com.subscriptionmanager.backend.dto.subscription.PaymentRequest;
import com.subscriptionmanager.backend.dto.subscription.PaymentResponse;
import com.subscriptionmanager.backend.security.UserPrincipal;
import com.subscriptionmanager.backend.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${api.base-path}/subscriptions/{subscriptionId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> list(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long subscriptionId
    ) {
        return ResponseEntity.ok(paymentService.listForSubscription(principal.getId(), subscriptionId));
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long subscriptionId,
        @Valid @RequestBody PaymentRequest request
    ) {
        return ResponseEntity.ok(paymentService.create(principal.getId(), subscriptionId, request));
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long subscriptionId,
        @PathVariable Long paymentId
    ) {
        paymentService.delete(principal.getId(), subscriptionId, paymentId);
        return ResponseEntity.noContent().build();
    }
}
