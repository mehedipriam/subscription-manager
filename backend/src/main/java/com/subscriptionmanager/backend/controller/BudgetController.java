package com.subscriptionmanager.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subscriptionmanager.backend.dto.budget.BudgetRequest;
import com.subscriptionmanager.backend.dto.budget.BudgetResponse;
import com.subscriptionmanager.backend.dto.budget.BudgetStatusResponse;
import com.subscriptionmanager.backend.security.UserPrincipal;
import com.subscriptionmanager.backend.service.BudgetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${api.base-path}/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping("/current")
    public ResponseEntity<BudgetStatusResponse> current(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(budgetService.getStatus(principal.getId(), LocalDate.now()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<BudgetResponse>> history(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(budgetService.history(principal.getId()));
    }

    @PutMapping
    public ResponseEntity<BudgetResponse> upsert(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody BudgetRequest request
    ) {
        return ResponseEntity.ok(budgetService.upsert(principal.getId(), request));
    }
}
