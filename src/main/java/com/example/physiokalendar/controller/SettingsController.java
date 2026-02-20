package com.example.physiokalendar.controller;

import com.example.physiokalendar.entity.AppSetting;
import com.example.physiokalendar.service.AppSettingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for managing application settings/configuration.
 */
@RestController
@RequestMapping("/api/settings")
@Slf4j
@AllArgsConstructor
public class SettingsController {

    private final AppSettingService appSettingService;

    /**
     * Get all settings in a specific category (e.g., "OPENING_HOURS")
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<AppSetting>> getSettingsByCategory(@PathVariable String category) {
        log.info("Getting settings for category: {}", category);
        List<AppSetting> settings = appSettingService.getSettingsByCategory(category);
        return ResponseEntity.ok(settings);
    }

    /**
     * Get a specific setting by key
     */
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, String>> getSetting(@PathVariable String key) {
        log.info("Getting setting: {}", key);
        return appSettingService.getSetting(key)
                .map(value -> ResponseEntity.ok(Map.of("key", key, "value", value)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Save or update multiple settings at once
     */
    @PostMapping("/batch")
    public ResponseEntity<List<AppSetting>> saveSettings(@RequestBody List<AppSetting> settings) {
        log.info("Saving {} settings", settings.size());
        List<AppSetting> saved = settings.stream()
                .map(s -> appSettingService.saveSetting(s.getKey(), s.getValue(), s.getCategory(), s.getDescription()))
                .toList();
        return ResponseEntity.ok(saved);
    }

    /**
     * Save or update a single setting
     */
    @PostMapping
    public ResponseEntity<AppSetting> saveSetting(@RequestBody AppSetting setting) {
        log.info("Saving setting: {} = {}", setting.getKey(), setting.getValue());
        AppSetting saved = appSettingService.saveSetting(
                setting.getKey(),
                setting.getValue(),
                setting.getCategory(),
                setting.getDescription()
        );
        return ResponseEntity.ok(saved);
    }
}
