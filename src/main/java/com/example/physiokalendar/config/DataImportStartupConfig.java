package com.example.physiokalendar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.physiokalendar.dataimport.DataImportService;

/**
 * Führt Datenimport beim Startup aus, wenn Konfiguration aktiviert ist.
 * Nur für Serientermine - NICHT für Standard-Import.
 *
 * Konfiguration in application.properties:
 * app.dataimport.series.enabled=true
 * app.dataimport.series.filepath=src/main/java/com/example/physiokalendar/dataimport/backup.json
 */
@Component
public class DataImportStartupConfig {

    private static final Logger log = LoggerFactory.getLogger(DataImportStartupConfig.class);

    @Autowired
    private DataImportService dataImportService;

    @Value("${app.dataimport.series.enabled:false}")
    private boolean seriesImportEnabled;

    @Value("${app.dataimport.series.filepath:}")
    private String seriesImportFilePath;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!seriesImportEnabled) {
            log.info("Series import is disabled (app.dataimport.series.enabled=false)");
            return;
        }

        if (seriesImportFilePath == null || seriesImportFilePath.isEmpty()) {
            log.error("Series import enabled but filepath is empty!");
            return;
        }

        log.info("========== STARTUP DATA IMPORT (SERIES ONLY) ==========");
        log.info("Filepath: {}", seriesImportFilePath);

        try {
            long startTime = System.currentTimeMillis();
            dataImportService.importSeriesOnly(seriesImportFilePath);
            long duration = System.currentTimeMillis() - startTime;
            log.info("========== SERIES IMPORT COMPLETED in {} ms ==========", duration);
        } catch (Exception e) {
            log.error("FATAL ERROR during series import startup", e);
            // Nicht abbrechen - Application lädt trotzdem
        }
    }
}
