package com.example.physiokalendar.config;

import com.example.physiokalendar.service.AppSettingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Initialize application settings on startup.
 * Ensures default configuration is available.
 */
@Component
@Slf4j
@AllArgsConstructor
public class AppStartupInitializer {

    private final AppSettingService appSettingService;

    /**
     * Run initialization after Spring Boot is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeAppSettings() {
        log.info("Initializing application settings...");

        try {
            // Initialize default opening hours if not already set
            appSettingService.initializeDefaultOpeningHours();

            log.info("Application settings initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing application settings", e);
        }
    }
}
