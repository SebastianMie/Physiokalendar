package com.example.physiokalendar.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for environment and system information.
 * Provides environment banner info for frontend.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:5173"})
public class EnvironmentController {

    @Value("${app.environment:dev}")
    private String environment;

    @Value("${spring.application.name:Physiokalendar}")
    private String applicationName;

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.seed-data-enabled:false}")
    private boolean seedDataEnabled;

    /**
     * Get environment information.
     * Used by frontend to display environment banner.
     *
     * GET /api/env
     */
    @GetMapping("/env")
    public ResponseEntity<Map<String, Object>> getEnvironment() {
        Map<String, Object> envInfo = new HashMap<>();
        envInfo.put("environment", environment.toUpperCase());
        envInfo.put("applicationName", applicationName);
        envInfo.put("isDevelopment", "dev".equalsIgnoreCase(environment));
        envInfo.put("isTest", "test".equalsIgnoreCase(environment));
        envInfo.put("isProduction", "prod".equalsIgnoreCase(environment));
        envInfo.put("mailEnabled", mailEnabled);
        envInfo.put("seedDataEnabled", seedDataEnabled);

        // Banner configuration for frontend
        Map<String, String> banner = new HashMap<>();
        switch (environment.toLowerCase()) {
            case "dev":
                banner.put("show", "true");
                banner.put("text", "ENTWICKLUNG");
                banner.put("color", "#4caf50"); // Green
                break;
            case "test":
                banner.put("show", "true");
                banner.put("text", "TEST");
                banner.put("color", "#ff9800"); // Orange
                break;
            case "prod":
                banner.put("show", "false");
                banner.put("text", "");
                banner.put("color", "");
                break;
            default:
                banner.put("show", "true");
                banner.put("text", environment.toUpperCase());
                banner.put("color", "#9c27b0"); // Purple for unknown
        }
        envInfo.put("banner", banner);

        return ResponseEntity.ok(envInfo);
    }

    /**
     * Health check endpoint.
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("environment", environment);
        health.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(health);
    }
}
