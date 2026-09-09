package com.subscriptionmanager.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subscriptionmanager.backend.dto.analytics.AnalyticsResponse;
import com.subscriptionmanager.backend.security.UserPrincipal;
import com.subscriptionmanager.backend.service.AnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${api.base-path}/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsResponse> summary(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(analyticsService.getAnalytics(principal.getId()));
    }
}
