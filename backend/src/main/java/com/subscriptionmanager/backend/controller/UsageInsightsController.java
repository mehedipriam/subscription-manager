package com.subscriptionmanager.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.subscriptionmanager.backend.dto.usage.UsageInsightResponse;
import com.subscriptionmanager.backend.security.UserPrincipal;
import com.subscriptionmanager.backend.service.UsageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${api.base-path}/usage-insights")
@RequiredArgsConstructor
public class UsageInsightsController {

    private final UsageService usageService;

    @GetMapping
    public ResponseEntity<List<UsageInsightResponse>> list(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(name = "rarelyUsedOnly", defaultValue = "false") boolean rarelyUsedOnly
    ) {
        List<UsageInsightResponse> insights = usageService.insights(principal.getId());
        if (rarelyUsedOnly) {
            insights = insights.stream().filter(UsageInsightResponse::rarelyUsed).toList();
        }
        return ResponseEntity.ok(insights);
    }
}
