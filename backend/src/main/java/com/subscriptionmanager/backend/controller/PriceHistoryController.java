package com.subscriptionmanager.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subscriptionmanager.backend.dto.subscription.PriceHistoryResponse;
import com.subscriptionmanager.backend.security.UserPrincipal;
import com.subscriptionmanager.backend.service.PriceHistoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${api.base-path}/subscriptions/{subscriptionId}/price-history")
@RequiredArgsConstructor
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    @GetMapping
    public ResponseEntity<List<PriceHistoryResponse>> list(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long subscriptionId
    ) {
        return ResponseEntity.ok(priceHistoryService.listForSubscription(principal.getId(), subscriptionId));
    }
}
