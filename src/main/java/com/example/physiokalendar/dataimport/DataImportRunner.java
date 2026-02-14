package com.example.physiokalendar.dataimport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * CommandLineRunner um Daten beim Startup zu importieren
 *
 * Aktivierung via application.properties:
 * app.dataimport.enabled=true
 * app.dataimport.filepath=src/main/java/com/example/physiokalendar/dataimport/backup.json
 *
 * Oder umgebungsvariablen:
 * APP_DATAIMPORT_ENABLED=true
 * APP_DATAIMPORT_FILEPATH=src/main/java/com/example/physiokalendar/dataimport/backup.json
 */
@Component
@ConditionalOnProperty(name = "app.dataimport.enabled", havingValue = "true", matchIfMissing = false)
public class DataImportRunner implements CommandLineRunner {

    @Autowired
    private DataImportService dataImportService;

    @org.springframework.beans.factory.annotation.Value("${app.dataimport.filepath:src/main/java/com/example/physiokalendar/dataimport/backup.json}")
    private String importFilePath;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   DATENIMPORT - AUTOMATISCHER START    ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("\nDatei: " + importFilePath);
        System.out.println("Status: Importiere Daten...\n");

        try {
            dataImportService.importData(importFilePath);
            System.out.println("\n✓ Import erfolgreich abgeschlossen!");
            System.out.println("  Error-Log: src/main/java/com/example/physiokalendar/dataimport/error_log.txt\n");
        } catch (Exception e) {
            System.out.println("\n✗ Fehler beim Import: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
