package com.subscriptionmanager.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subscriptionmanager.backend.dto.usage.UsageLogResponse;
import com.subscriptionmanager.backend.security.UserPrincipal;
import com.subscriptionmanager.backend.service.UsageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${api.base-path}/subscriptions/{subscriptionId}/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageService usageService;

    @GetMapping
    public ResponseEntity<List<UsageLogResponse>> history(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long subscriptionId
    ) {
        return ResponseEntity.ok(usageService.history(principal.getId(), subscriptionId));
    }

    @PostMapping
    public ResponseEntity<UsageLogResponse> logUsage(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long subscriptionId
    ) {
        return ResponseEntity.ok(usageService.logUsage(principal.getId(), subscriptionId));
    }
}
