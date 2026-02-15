# Backup-Strategie und Testinstanz-Anleitung für Physiokalendar

## Übersicht

Diese Dokumentation beschreibt eine geeignete Backup-Strategie für die Physiokalendar-Anwendung (Spring Boot Backend, Angular Frontend, MySQL Datenbank) sowie einen Schritt-für-Schritt-Plan zum Hochziehen einer Docker-basierten Testinstanz. Die Schritte sind sequentiell aufgebaut, um maximale Übersicht und Wartbarkeit zu gewährleisten.

## 1. Geeignete Backup-Strategie

Eine umfassende Backup-Strategie deckt Daten, Code und Infrastruktur ab und minimiert Risiken von Datenverlust.

### 1.1 Datenbank-Backup (MySQL)
- **Methode:** Regelmäßige logische Backups mit `mysqldump`.
- **Häufigkeit:** Täglich (für Produktion) oder wöchentlich (für Test/Dev). Vollständige Backups alle 7 Tage, inkrementelle täglich.
- **Speicherort:** Sichere Cloud-Speicher (z.B. AWS S3, Azure Blob Storage) oder NAS mit Verschlüsselung. Lokale Kopien für schnellen Zugriff.
- **Automatisierung:** Cron-Job oder Tool wie `automysqlbackup`. Beispiel-Befehl:
  ```
  mysqldump -u [user] -p[password] physiocalendar > backup_$(date +%Y%m%d).sql
  ```
- **Retention:** 30 Tage für tägliche Backups, 1 Jahr für wöchentliche.

### 1.2 Code- und Konfigurations-Backup
- **Git-Repository:** Regelmäßige Commits und Pushes zu einem Remote-Repository (z.B. GitHub, GitLab). Branches für Dev/Test/Prod.
- **Konfigurationsdateien:** Backup von `.env`-Dateien, `docker-compose.yml`, `application.yml` usw. Speichern in Git oder separatem Config-Repo.
- **Artefakte:** Backup von gebauten JARs/Docker-Images (z.B. in einer Registry wie Docker Hub oder Azure Container Registry).

### 1.3 Docker-Infrastruktur-Backup
- **Volumes:** Backup der Docker-Volumes (z.B. `db_data`) mit:
  ```
  docker run --rm -v [volume]:/data -v $(pwd):/backup alpine tar czf /backup/volume_backup.tar.gz -C /data .
  ```
- **Container-Images:** Regelmäßiges Pushen von Images zu einer Registry.
- **Compose-Dateien:** Versionierung in Git.

### 1.4 Zusätzliche Maßnahmen
- **Test-Restores:** Monatlich testen, ob Backups wiederherstellbar sind.
- **Verschlüsselung:** Alle Backups verschlüsseln (z.B. mit GPG).
- **Monitoring:** Alerts bei Backup-Fehlern (z.B. via Prometheus oder einfache Skripte).
- **Kosten:** Für kleine Projekte reicht kostenloser Cloud-Speicher; skalieren Sie bei Bedarf.

## 2. Schritt-für-Schritt-Plan: Docker-Testinstanz hochziehen

Basierend auf den vorhandenen Dateien (`compose.test.yml`, `Dockerfile`, `Dockerfile.frontend`, `.env.test`) starten Sie eine isolierte Testinstanz. Die Instanz läuft auf separaten Ports (DB: 3307, Backend: 8081, Frontend: 4201) und verwendet eine Test-Datenbank.

### Voraussetzungen
- Docker und Docker Compose installiert (`docker --version` und `docker compose version` prüfen).
- Projektverzeichnis: `c:\Users\sem\Dokumente\Workspace\Kunden\Projekte\Physiokalendar`.

### Schritt 1: Vorbereitung
1. Navigieren Sie ins Projektverzeichnis:
   ```
   cd c:\Users\sem\Dokumente\Workspace\Kunden\Projekte\Physiokalendar
   ```
2. Überprüfen Sie die `.env.test`-Datei (sie ist bereits vorhanden und konfiguriert).

### Schritt 2: Testinstanz starten
1. Starten Sie die Container im Hintergrund:
   ```
   docker compose -f compose.test.yml --env-file .env.test up -d
   ```
   - Dies baut und startet:
     - MySQL-DB (Container: `physio-test-db`, Port 3307).
     - Spring Boot-Backend (Container: `physio-test-backend`, Port 8081, Profil: `test`).
     - Angular-Frontend (Container: `physio-test-frontend`, Port 4201).
2. Warten Sie 1-2 Minuten, bis die Container healthy sind.
3. Überprüfen Sie den Status:
   ```
   docker compose -f compose.test.yml ps
   ```

### Schritt 3: Verifizieren
1. Überprüfen Sie die Logs:
   ```
   docker compose -f compose.test.yml logs
   ```
2. Testen Sie die Endpunkte:
   - Frontend: Öffnen Sie http://localhost:4201 im Browser.
   - Backend: http://localhost:8081 (z.B. API-Endpunkte).
   - DB: Verbinden Sie sich mit einem MySQL-Client (z.B. MySQL Workbench) zu `localhost:3307` (User: `physiouser`, Pass: `testpassword`, DB: `physiocalendar_test`).

### Schritt 4: Datenbank-Dump exportieren (aus Produktions-DB)
1. Verbinden Sie sich mit Ihrer Produktions-MySQL-DB (ersetzen Sie die Platzhalter):
   ```
   mysqldump -h [prod-db-host] -P [prod-db-port] -u [prod-user] -p[prod-password] physiocalendar > prod_backup.sql
   ```
   - Beispiel (falls lokal): `mysqldump -h localhost -P 3306 -u root -p physiocalendar > prod_backup.sql`.
2. Speichern Sie die Datei sicher (z.B. in `./backups/`).

### Schritt 5: Datenbank-Dump in Testinstanz importieren
1. Stellen Sie sicher, dass die Test-DB leer ist (oder löschen Sie Daten bei Bedarf).
2. Importieren Sie den Dump in die Test-DB:
   ```
   mysql -h localhost -P 3307 -u physiouser -ptestpassword physiocalendar_test < prod_backup.sql
   ```
3. Überprüfen Sie den Import: Verbinden Sie sich mit der Test-DB und führen Sie `SHOW TABLES;` aus.

### Schritt 6: Testen und Anpassen
1. Testen Sie die Anwendung mit den importierten Daten.
2. Bei Fehlern: Logs prüfen (`docker compose -f compose.test.yml logs [service-name]`) oder Container neu starten (`docker compose -f compose.test.yml restart`).

### Schritt 7: Instanz stoppen und aufräumen
1. Stoppen Sie die Instanz:
   ```
   docker compose -f compose.test.yml down
   ```
2. Volumes entfernen (für frischen Start):
   ```
   docker compose -f compose.test.yml down -v
   ```

## Hinweise
- Die Testinstanz ist isoliert und beeinflusst nicht Ihre Dev/Prod-Umgebung.
- Für Produktion verwenden Sie `compose.prod.yml`.
- Bei Problemen: Stellen Sie sicher, dass Ports frei sind und Firewall-Einstellungen korrekt.
- Aktualisieren Sie diese Dokumentation bei Änderungen am Setup.

## Wartbarkeit
- Speichern Sie diese Datei im Projekt-Repository (z.B. in `docs/`).
- Überprüfen Sie regelmäßig die Backup-Strategie und aktualisieren Sie bei Infrastruktur-Änderungen.
- Verwenden Sie Versionskontrolle für alle Änderungen an dieser Doku.