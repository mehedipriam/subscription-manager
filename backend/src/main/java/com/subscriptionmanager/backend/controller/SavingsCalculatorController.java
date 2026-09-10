package com.subscriptionmanager.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subscriptionmanager.backend.dto.calculator.CancellationSavingsRequest;
import com.subscriptionmanager.backend.dto.calculator.CancellationSavingsResponse;
import com.subscriptionmanager.backend.security.UserPrincipal;
import com.subscriptionmanager.backend.service.SavingsCalculatorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${api.base-path}/savings-calculator")
@RequiredArgsConstructor
public class SavingsCalculatorController {

    private final SavingsCalculatorService savingsCalculatorService;

    @PostMapping
    public ResponseEntity<CancellationSavingsResponse> calculate(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CancellationSavingsRequest request
    ) {
        return ResponseEntity.ok(savingsCalculatorService.calculate(principal.getId(), request.subscriptionIds()));
    }
}
