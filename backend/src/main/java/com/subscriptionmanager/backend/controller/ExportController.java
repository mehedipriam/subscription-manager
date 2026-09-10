package com.subscriptionmanager.backend.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subscriptionmanager.backend.security.UserPrincipal;
import com.subscriptionmanager.backend.service.ExportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${api.base-path}/export")
@RequiredArgsConstructor
public class ExportController {

    private static final MediaType TEXT_CSV = MediaType.parseMediaType("text/csv");

    private final ExportService exportService;

    @GetMapping("/subscriptions/csv")
    public ResponseEntity<byte[]> subscriptionsCsv(@AuthenticationPrincipal UserPrincipal principal) {
        return asFile(exportService.subscriptionsCsv(principal.getId()), "subscriptions.csv", TEXT_CSV);
    }

    @GetMapping("/subscriptions/pdf")
    public ResponseEntity<byte[]> subscriptionsPdf(@AuthenticationPrincipal UserPrincipal principal) {
        return asFile(exportService.subscriptionsPdf(principal.getId()), "subscriptions.pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/payments/csv")
    public ResponseEntity<byte[]> paymentsCsv(@AuthenticationPrincipal UserPrincipal principal) {
        return asFile(exportService.paymentsCsv(principal.getId()), "payments.csv", TEXT_CSV);
    }

    @GetMapping("/payments/pdf")
    public ResponseEntity<byte[]> paymentsPdf(@AuthenticationPrincipal UserPrincipal principal) {
        return asFile(exportService.paymentsPdf(principal.getId()), "payments.pdf", MediaType.APPLICATION_PDF);
    }

    private ResponseEntity<byte[]> asFile(byte[] content, String filename, MediaType mediaType) {
        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename).build().toString()
            )
            .body(content);
    }
}
