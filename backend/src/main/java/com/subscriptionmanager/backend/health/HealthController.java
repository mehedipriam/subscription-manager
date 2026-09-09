package com.subscriptionmanager.backend.health;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("${api.base-path}/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "backend",
            "timestamp", Instant.now().toString()
        );
    }
}
