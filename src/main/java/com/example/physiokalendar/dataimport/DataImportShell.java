package com.example.physiokalendar.dataimport;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.shell.standard.ShellComponent;
// import org.springframework.shell.standard.ShellMethod;
// import org.springframework.shell.standard.ShellOption;

/**
 * Shell-Komponente für die Daten-Importierung via CLI
 * HINWEIS: Deaktiviert - Datenimport erfolgt über Flyway SQL Migrationen
 *
 * Verwendung (falls benötigt):
 * 1. Spring Shell Dependency in pom.xml aktivieren
 * 2. Spring Boot starten: mvn spring-boot:run
 * 3. Im Shell-Prompt: import-data --filePath=src/main/java/com/example/physiokalendar/dataimport/backup.json
 */
// @ShellComponent
public class DataImportShell {

    @Autowired
    private DataImportService dataImportService;

    // @ShellMethod(key = "import-data", value = "Daten aus JSON-Datei importieren")
    public void importData(
            String filePath) {
            // @ShellOption(value = "--filePath", help = "Pfad zur backup.json Datei")

        System.out.println("========================================");
        System.out.println("Starte Datenimport ...");
        System.out.println("Datei: " + filePath);
        System.out.println("========================================\n");

        try {
            dataImportService.importData(filePath);
            System.out.println("\n✓ Import erfolgreich abgeschlossen!");
            System.out.println("Fehler-Log: src/main/java/com/example/physiokalendar/dataimport/error_log.txt\n");
        } catch (Exception e) {
            System.out.println("\n✗ Fehler beim Import: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // @ShellMethod(key = "help-import", value = "Hilfe zum Datenimport anzeigen")
    public void showHelp() {
        System.out.println("""
                ========================================
                HILFE ZUM DATENIMPORT
                ========================================

                Befehl: import-data
                Parameter: --filePath=<Pfad zur backup.json>

                Beispiel:
                import-data --filePath=src/main/java/com/example/physiokalendar/dataimport/backup.json

                Was wird importiert:
                - Therapeuten (therapists)
                - Patienten (automatisch aus Terminen)
                - Einzeltermine (daylist)
                - Serientermine (masterlist)
                - Abwesenheiten

                Fehlerbehandlung:
                - Fehlerhafte Einträge werden protokolliert
                - Das Programm setzt fort, auch bei Fehlern
                - Alle Fehler: error_log.txt

                Wichtig:
                1. Machen Sie ein Datenbank-Backup
                2. Überprüfen Sie error_log.txt nach dem Import
                3. Validieren Sie die Daten in der UI
                ========================================
                """);
    }
}
