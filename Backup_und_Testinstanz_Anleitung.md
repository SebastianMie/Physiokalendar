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

## 3. Automatisierung (Backup + automatischer Import in Test)
Folgende Skripte und Dienste sind im Repository enthalten, um Backups zu automatisieren und bei Bedarf Dev‑Dumps in die Test‑DB zu importieren:

- `scripts/mysql_backup.sh` — tägliche Full/Incremental Backups (konfigurierbar, schreibt nach `/backup`).
- `Dockerfile.backup` + `backup-cron` — Sidecar‑Container, der `mysql_backup.sh` per cron ausführt (Service `physio-test-backup` in `compose.test.yml`).
- `scripts/sync_dev_to_test.sh` — erzeugt Dev‑Dump und spielt ihn in die Test‑DB ein (inkl. Test‑DB‑Backup, `--force` zum Überspringen von Fehlern).
- `scripts/import_latest_to_test.sh` — importiert die neueste `dev_dump_*.sql.gz` aus `./backups/` in `physio-test-db` (legt zuvor ein Test‑Backup an).
- `Makefile` — Shortcuts: `make backup-dev`, `make import-latest-to-test`, `make start-backup`.

Automatische Planung

- Der Backup‑Sidecar verwendet die Datei `backup-cron` (in der Image‑Build) — aktuell: `0 2 * * * /usr/local/bin/mysql_backup.sh` → täglicher Lauf um **02:00** (Container‑/Server‑Zeitzone).
- Prüfen, ob der Cron‑Eintrag aktiv ist:
  ```bash
  docker exec -it physio-test-backup cat /etc/cron.d/backup-cron
  ```
- Letzte Ausgaben / Historie prüfen:
  ```bash
  docker exec -it physio-test-backup tail -n 200 /var/log/mysql_backup.log
  docker logs physio-test-backup --since "24h"
  ```

Manuelles inkrementelles Backup (Kurzbefehle)

- 1) Prüfe, ob Binary‑Logging aktiviert ist (wenn nicht → Script erzeugt Fallback Vollbackup):
  ```bash
  docker exec -it physio-test-db mysql -uroot -p${DB_ROOT_PASSWORD:-testrootpassword} \
    -e "SHOW VARIABLES LIKE 'log_bin'; SELECT @@GLOBAL.binlog_format;"
  ```

- 2) Manuell das Backup‑Script ausführen (das Script wählt inkrementell oder Fallback automatisch):
  ```bash
  # Verwende MSYS_NO_PATHCONV in Git Bash auf Windows, sonst normal:
  MSYS_NO_PATHCONV=1 docker exec -i physio-test-backup sh -c "/usr/local/bin/mysql_backup.sh"
  # oder interaktiv:
  docker exec -it physio-test-backup /bin/sh -c "/usr/local/bin/mysql_backup.sh"
  ```
  - Ergebnis: `inc_YYYY-MM-DD*.sql.gz` (bei aktivem binlog) oder `full_fallback_YYYY-MM-DD*.sql.gz`.

- 3) (Optional / Fortgeschritten) Direkter mysqlbinlog‑Dump einer Binlog‑Datei:
  ```bash
  # Auf DB: neue Binlog-Datei erzeugen und Name holen
  docker exec -it physio-test-db mysql -uroot -p${DB_ROOT_PASSWORD:-testrootpassword} -e "FLUSH LOGS; SHOW BINARY LOGS;"

  # Beispiel: BINLOG_FILE aus dem vorherigen Befehl übernehmen und auf dem Backup-Container auslesen
  docker exec -i physio-test-backup sh -c \
    "mysqlbinlog --read-from-remote-server --host=physio-test-db --port=3306 --user=${DB_USER:-physiouser} --password=${DB_PASSWORD:-testpassword} BINLOG_FILE | gzip > /backup/inc_$(date +%Y-%m-%d).sql.gz"
  ```
  - Hinweis: für `mysqlbinlog` sind Replikations‑Rechte/`REPLICATION CLIENT` hilfreich; Root funktioniert immer.

- 4) Prüfen, ob ein inkrementelles Backup erstellt wurde:
  ```bash
  ls -lah backups | grep inc_ || ls -lah backups | grep full_fallback_
  ```

Logs & Troubleshooting

- Cron‑Status prüfen (crontab in Container) und Logs (`/var/log/mysql_backup.log`).
- Wenn inkrementelles Backup ausbleibt: Binlog auf `physio-test-db` aktivieren (siehe `mysql/conf.d/my.cnf`) oder akzeptiere, dass Script einen `full_fallback` erstellt.

Windows‑Scheduled‑Task (Hinweis)

- Die Windows‑PowerShell‑Hilfsdatei `scripts/register_import_task.ps1` ist optional und wird in dieser Dokumentation **nicht** mehr empfohlen. Die empfohlene Methoden sind:
  - Backup/Cron im Container (`physio-test-backup`) oder
  - Host‑Cron (für nicht‑Docker Umgebungen).

Beispiele (manuell):

- Start Backup‑Sidecar (führt täglich Backups aus):
  ```bash
  docker compose -f compose.test.yml --env-file .env.test up -d physio-test-backup
  ```

- Erstelle sofortiges Dev‑Backup (lokal):
  ```bash
  make backup-dev
  ```

- Importiere das neueste Dev‑Backup in Test (non‑interactive):
  ```bash
  make import-latest-to-test
  # oder:
  bash scripts/import_latest_to_test.sh -y
  ```

- Cron (Host) — importiere Dev→Test jede Nacht um 03:30 (optional):
  ```cron
  30 3 * * * cd /path/to/Physiokalendar && ./scripts/import_latest_to_test.sh -y >> logs/import_cron.log 2>&1
  ```

---

## Hinweise
- Die Testinstanz ist isoliert und beeinflusst nicht Ihre Dev/Prod-Umgebung.
- Für Produktion verwenden Sie `compose.prod.yml`.
- Bei Problemen: Stellen Sie sicher, dass Ports frei sind und Firewall-Einstellungen korrekt.
- Aktualisieren Sie diese Dokumentation bei Änderungen am Setup.

## Wartbarkeit
- Speichern Sie diese Datei im Projekt-Repository (z.B. in `docs/`).
- Überprüfen Sie regelmäßig die Backup-Strategie und aktualisieren Sie bei Infrastruktur-Änderungen.
- Verwenden Sie Versionskontrolle für alle Änderungen an dieser Doku.