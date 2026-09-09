package com.subscriptionmanager.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subscriptionmanager.backend.dto.dashboard.DashboardResponse;
import com.subscriptionmanager.backend.security.UserPrincipal;
import com.subscriptionmanager.backend.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${api.base-path}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardResponse> summary(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dashboardService.getSummary(principal.getId()));
    }
}
