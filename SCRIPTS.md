# 🔧 Physiokalender - Backup & Restore Scripts

Schnelle Referenz für alle verfügbaren Backup-Scripts.

---

## 📚 Überblick

| Script | Funktion | Stage-Parameter |
|--------|----------|-----------------|
| `backup.sh` | Backup triggern | test, prod |
| `restore.sh` | Datenbank wiederherstellen | test, prod |
| `import-staging.sh` | Backup zwischen Stages importieren | test ↔ prod |
| `backup-status.sh` | Backup-Status prüfen | test, prod, all |

---

## 1️⃣ Backup Triggern: `backup.sh`

### Verwendung
```bash
./scripts/backup.sh <STAGE> [TYPE]

STAGE:    test oder prod (erforderlich)
TYPE:     full, incremental, cleanup, auto (optional, default: auto)
```

### Beispiele

**Auto-Modus (intelligente Auswahl)**
```bash
./scripts/backup.sh test      # Auto wählt full (um 18:00) oder incremental (um 07:00)
./scripts/backup.sh prod
```

**Spezifischen Typ erzwingen**
```bash
./scripts/backup.sh test full         # Full Backup für TEST
./scripts/backup.sh prod incremental  # Incremental für PROD
./scripts/backup.sh test cleanup      # Cleanup alte Backups >28 Tage
```

### Output
```
╔════════════════════════════════════════════════════════════════╗
║              BACKUP TRIGGER SCRIPT                             ║
║  Stage:           test                                         ║
║  Container:       physio-test-backup                           ║
║  Backup-Type:     full                                         ║
║  Verzeichnis:     ./backups                                    ║
╚════════════════════════════════════════════════════════════════╝

🔄 Führe Backup aus: /usr/local/bin/mysql_backup.sh full

✅ Backup erfolgreich durchgeführt!

📁 Verfügbare Backups für test:
   ./backups/test_full_20260219_180000.sql.gz (142.5M)
   ./backups/test_inc_20260219_070000.sql.gz (24.3M)

💾 Neuestes Backup: test_full_20260219_180000.sql.gz (142.5 MB)
```

### Was passiert?
1. ✅ Environment-Datei (`.env.{stage}`) wird geladen
2. ✅ Compose-Datei (`.compose.{stage}.yml`) wird geladen
3. ✅ Backup-Container wird geprüft/gestartet
4. ✅ `mysql_backup.sh` wird im Container ausgeführt
5. ✅ Neueste Backups werden angezeigt

### Fehlerbehandlung
```bash
# Container läuft nicht? → Script startet ihn automatisch
# Environment-Datei fehlt? → Klare Fehlermeldung mit Pfad
# Backup fehlgeschlagen? → Container-Logs werden angezeigt
```

---

## 2️⃣ Backup Status: `backup-status.sh`

### Verwendung
```bash
./scripts/backup-status.sh [STAGE]

STAGE: test, prod, all (optional, default: all)
```

### Beispiele

**Alle Umgebungen**
```bash
./scripts/backup-status.sh
```

**Nur TEST**
```bash
./scripts/backup-status.sh test
```

**Nur PROD**
```bash
./scripts/backup-status.sh prod
```

**Cleanup-Analyse** (was würde gelöscht?)
```bash
./scripts/backup-status.sh --clean
```

### Output Beispiel (test Umgebung)
```
════════════════════════════════════════════════════════════════
  📊 BACKUP STATUS: test (Umgebung)
════════════════════════════════════════════════════════════════

  📁 FULL BACKUPS
  ─────────────────────────────────────────────────────────────
  ✓  test_full_20260219_180000.sql.gz        142.5 MB  2026-02-19 18:00:00
  ✓  test_full_20260218_180000.sql.gz        138.2 MB  2026-02-18 18:00:00

  📁 INCREMENTAL BACKUPS
  ─────────────────────────────────────────────────────────────
  ✓  test_inc_20260219_070000.sql.gz          24.3 MB  2026-02-19 07:00:00
  ✓  test_inc_20260218_070000.sql.gz          21.1 MB  2026-02-18 07:00:00

  💾 SPEICHERVORKOMMEN
  ─────────────────────────────────────────────────────────────
  Gesamt (alle Stages):  580.5 MB
  Nur test Backups:      326.1 MB

  🕐 NEUESTE BACKUPS
  ─────────────────────────────────────────────────────────────
  Full:         2026-02-19 18:00:00
  Incremental:  2026-02-19 07:00:00
```

### Cleanup-Analyse Output
```
════════════════════════════════════════════════════════════════
  🧹 CLEANUP ANALYSE
════════════════════════════════════════════════════════════════

  📊 Stage: test (Retention: 28 Tage)
  ─────────────────────────────────────────────────────────────
    ✓  Keine Backups älter als 28 Tage

  📊 Stage: prod (Retention: 28 Tage)
  ─────────────────────────────────────────────────────────────
    ✗  prod_full_20260122_180000.sql.gz (85.2 MB) - würde gelöscht

  💡 Um alte Backups zu löschen, führen Sie aus:
     docker exec physio-test-backup /usr/local/bin/mysql_backup.sh cleanup
     docker exec physio-prod-backup /usr/local/bin/mysql_backup.sh cleanup
```

---

## 3️⃣ Restore Backup: `restore.sh`

### Verwendung
```bash
./scripts/restore.sh <STAGE> [BACKUP_FILE|--latest|--list]

STAGE:        test oder prod (erforderlich)
BACKUP_FILE:  Dateiname oder --latest oder --list
```

### Beispiele

**Verfügbare Backups anzeigen**
```bash
./scripts/restore.sh test --list
```

**Neuestes Backup zurückgeben**
```bash
./scripts/restore.sh test --latest
```

**Spezifisches Backup zurückgeben**
```bash
./scripts/restore.sh prod prod_full_20260219_180000.sql.gz
./scripts/restore.sh prod 20260219_180000.sql.gz
```

⚠️ **Sicherheit**: Script fragt immer um Bestätigung vor dem Restore!

---

## 4️⃣ Import Staging: `import-staging.sh`

### Verwendung
```bash
./scripts/restore.sh <STAGE> [BACKUP_FILE|--latest|--list]

STAGE:        test oder prod (erforderlich)
BACKUP_FILE:  Dateiname (test_full_20260219_180000.sql.gz)
--latest:     Neuestes Backup verwenden
--list:       Alle Backups auflisten
```

### Beispiele

**Liste Backups**
```bash
./scripts/restore.sh test --list

# Output:
# 📁 Verfügbare Backups für test:
#    ./backups/test_full_20260219_180000.sql.gz      142.5M  2026-02-19 18:00:00
#    ./backups/test_inc_20260219_070000.sql.gz        24.3M  2026-02-19 07:00:00
```

**Neuestes Backup**
```bash
./scripts/restore.sh test --latest
# Stellt automatisch das neueste Backup wieder her
```

**Spezifisches Backup**
```bash
./scripts/restore.sh prod prod_full_20260219_180000.sql.gz

# Oder nur der Dateiname:
./scripts/restore.sh prod 20260219_180000.sql.gz
```

### Bestätigung erforderlich
```
╔════════════════════════════════════════════════════════════════╗
║              BACKUP RESTORE SCRIPT                             ║
║                                                                ║
║  ⚠️  WARNUNG: Diese Aktion wird die Datenbank ÜBERSCHREIBEN!   ║
║                                                                ║
║  Stage:           test                                         ║
║  Datenbank:       physiocalendar_test                          ║
║  Backup-Datei:    test_full_20260219_180000.sql.gz            ║
║  Größe:           142.5 MB                                     ║
╚════════════════════════════════════════════════════════════════╝

Möchten Sie wirklich fortfahren? (Geben Sie 'JA' ein)
```

### Restore in Progress
```
📥 Stelle Datenbank wieder her aus: test_full_20260219_180000.sql.gz

✅ Restore erfolgreich abgeschlossen!

📊 Datenbank-Statistiken:
   patients | Count: 145
   appointments | Count: 523
   therapists | Count: 18
```

### Was passiert beim Restore?
1. ✅ Environment & Compose-Datei werden geladen
2. ✅ Benutzer-Bestätigung erforderlich (Sicherheit!)
3. ✅ Datenbank-Container wird geprüft (startet falls nötig)
4. ✅ Backup wird dekomprimiert und restored
5. ✅ Statistiken werden für Verifizierung angezeigt

---

## 4️⃣ Import Staging: `import-staging.sh`

**Nutzen**: Backups zwischen Stages austauschen (z.B. Test-Backup → Prod importieren)

### Verwendung
```bash
./scripts/import-staging.sh <SOURCE_STAGE> <TARGET_STAGE> [BACKUP_SPEC]

SOURCE_STAGE: Quell-Stage (test oder prod) - von wo das Backup kommt
TARGET_STAGE: Ziel-Stage (test oder prod) - wohin das Backup gespielt wird
BACKUP_SPEC:  Backup-Datei / --latest / --list (optional, default: --latest)
```

### Beispiele

**Neuestes test-Backup in prod importieren**
```bash
./scripts/import-staging.sh test prod
# Nutzt automatisch das neueste Backup von test
```

**Spezifisches Datum wählen**
```bash
./scripts/import-staging.sh test prod 2026-02-19
# Nutzt das erste Backup vom 19.02.2026 aus test
```

**Verfügbare Backups anzeigen**
```bash
./scripts/import-staging.sh test prod --list

# Output:
# 📁 Verfügbare Backups von 'test':
#    ./backups/test_full_20260219_180000.sql.gz      142.5M  2026-02-19 18:00
#    ./backups/test_inc_20260219_070000.sql.gz        24.3M  2026-02-19 07:00
```

**Konkretes Backup-Datei importieren**
```bash
./scripts/import-staging.sh test prod test_full_20260219_180000.sql.gz
```

### Import-Dialog
```
╔════════════════════════════════════════════════════════════════╗
║         IMPORT STAGING SCRIPT                                  ║
║                                                                ║
║  ⚠️  WARNUNG: Diese Aktion wird die Datenbank ÜBERSCHREIBEN!   ║
║                                                                ║
║  Quelle:          test (Backup)                               ║
║  Ziel:            prod (Datenbank: physiocalendar)            ║
║  Backup-Datei:    test_full_20260219_180000.sql.gz           ║
║  Größe:           142.5 MB                                    ║
║  Ziel-Container:  physio-prod-db                             ║
╚════════════════════════════════════════════════════════════════╝

Möchten Sie wirklich fortfahren? (Geben Sie 'JA' ein)
```

### Import abgeschlossen
```
✅ Import erfolgreich abgeschlossen!

📊 Datenbank-Statistiken (prod):
   Patienten: 145
   Termine: 523
   Therapeuten: 18

⚠️  Bitte überprüfen Sie die Daten in 'prod' vor weiterer Verwendung!
```

### Was passiert beim Import?
1. ✅ Quell- und Ziel-Stage werden validiert (müssen unterschiedlich sein)
2. ✅ Backup wird aus dem Quell-Stage gesucht
3. ✅ Ziel-Stage Environment wird geladen
4. ✅ Benutzer-Bestätigung erforderlich (Sicherheit!)
5. ✅ Ziel-Datenbank-Container wird geprüft (startet falls nötig)
6. ✅ Backup wird in Ziel-Datenbank importiert
7. ✅ Datenbank-Statistiken werden für Verifizierung angezeigt

### ⚠️ Wichtige Hinweise

- **Bestätigung erforderlich**: Script fragt immer `JA` ab
- **Stages müssen unterschiedlich sein**: Kann nicht gleichzeitig quelle und ziel sein
- **Keine selbstreferenziellen Imports**: `import-staging.sh prod prod` nicht möglich
- **Datenbank wird überschrieben**: Alle bestehenden Daten in der Ziel-Stage werden ersetzt
- **Verify nach Import**: Immer die Daten überprüfen nach dem Import!

---

## 5️⃣ Deployment: `deploy.sh`

**Nutzen**: Backend & optional Frontend zu test/prod deployen

### Verwendung
```bash
./scripts/deploy.sh --to test|prod [--from dev|test|prod] [--mvn] [--frontend] [--no-cache] [--confirm-prod]

Erforderlich:
  --to test|prod              Ziel-Stage (ERFORDERLICH)

Optional:
  --from dev|test|prod        Quell-Environment (default: dev)
  --mvn                       Lokalen Maven build vor Docker Build
  --frontend                  Frontend bauen (Physiokalender-v2-UI)
  --no-cache                  Docker build ohne Cache
  --confirm-prod              Bestätigung für prod (ERFORDERLICH bei --to prod)
```

### Beispiele

**Einfaches Deployment zu test (nur Docker)**
```bash
./scripts/deploy.sh --to test
# Stellt Docker Bilder wieder her und startet Services
```

**Deployment zu test mit lokalem Maven Build**
```bash
./scripts/deploy.sh --to test --mvn
# Kompiliert Backend → Docker Image → start
```

**Deployment mit Frontend zu test**
```bash
./scripts/deploy.sh --to test --mvn --frontend
# Kompiliert Backend → Kompiliert Frontend → Docker Images → start
```

**Deployment zu prod (erfordert Bestätigung)**
```bash
./scripts/deploy.sh --to prod --mvn --frontend --no-cache --confirm-prod
# Alle optionen + Bestätigung für Production
```

### Deployment-Ablauf

Wenn **keine lokalen Builds** angegeben (`--mvn`, `--frontend`):
```
1. ✅ Validiere Parameter
2. ✅ docker compose -f compose.${TO_ENV}.yml --env-file .env.${TO_ENV} up -d --build
3. ✅ Zeige Service-Status an
```

Wenn **--mvn angegeben**:
```
1. ✅ Validiere Parameter
2. ✅ ./mvnw.cmd clean package (lokaler Maven Build)
3. ✅ docker compose ... up -d --build
```

Wenn **--mvn --frontend angegeben**:
```
1. ✅ Validiere Parameter
2. ✅ ./mvnw.cmd clean package (Backend Build)
3. ✅ cd Physiokalender-v2-UI && npm ci && npm run build (Frontend Build)
4. ✅ docker compose ... up -d --build
5. ✅ Zeige Service-Status an
```

### Output Beispiel
```
╔════════════════════════════════════════════════════════════════╗
║              DEPLOYMENT SCRIPT                                 ║
║                                                                ║
║  Von:           dev                                             ║
║  Nach:          test                                            ║
║  Maven Build:   true                                           ║
║  Frontend:      true                                           ║
║  No Cache:      false                                          ║
╚════════════════════════════════════════════════════════════════╝

🔨 Starte lokalen Maven Build...
📦 Build ausgeführt...
✅ Maven Build abgeschlossen

🎨 Starte Frontend Build...
📁 Frontend Repo: Physiokalender-v2-UI
📦 npm ci...
🏗️  npm run build...
✅ Frontend Build abgeschlossen

🚀 Starte Docker Compose für test...
   Befehl: docker compose -f compose.test.yml --env-file .env.test up -d --build

✅ Deployment zu 'test' abgeschlossen!

📊 Service Status:
CONTAINER ID   IMAGE                           NAMES          STATUS
a1b2c3d4e5f6   physio-test-backend:latest      physio-test-backend   Up 2s (health: starting)
g7h8i9j0k1l2   mysql:8.0.36                    physio-test-db        Up 3s (healthy)
```

### ⚠️ Sicherheit für Production

- **--confirm-prod ist erforderlich**: `./scripts/deploy.sh --to prod` ohne Flag wird abgelehnt!
- **Rationale**: Verhindert versehentliche Production-Deployments
- **Fehlerbehandlung**: Script stoppt sofort bei unerwarteten Fehlern

### ⚠️ Wichtige Hinweise

- **Backend wird immer gebaut**: Docker re-builds auf Basis der neuesten Änderungen
- **Frontend kompiliert bei --frontend**: Nutzt `npm run build-${STAGE}` oder fallback zu `npm run build`
- **Aus Physiokalender-v2-UI repo**: Frontend compose files sind dort, nicht im Backend Repo
- **--from Parameter**: Wird derzeit nicht aktiv genutzt (für zukünftige Erweiterungen)

---

## 📊 Zeitplan & Automatische Backups

### Cron-Jobs (laufen automatisch)
- **18:00 Uhr** - Full Backup (tägliche Sicherung)
- **07:00 Uhr** - Incremental Backup (für Wiederherstellung)
- **03:00 Sonntag** - Cleanup (löscht Backups >28 Tage)

### Backup Naming Convention
```
{env}_full_YYYYMMDD_HHMMSS_NNN.sql.gz
{env}_inc_YYYYMMDD_HHMMSS_NNN.sql.gz

Beispiele:
- test_full_20260219_180000_001.sql.gz
- prod_inc_20260219_070000_001.sql.gz
- prod_full_20260218_180000_002.sql.gz (2 Backups am selben Tag)
```

---

## 🎯 Praktische Szenarien

### Scenario 1: Tägliche Backup vor Deployment
```bash
# Full Backup machen
./scripts/backup.sh prod full

# Dann deployen
./scripts/deploy.sh --from dev --to prod

# Falls etwas schiefgeht:
./scripts/restore.sh prod --latest
```

### Scenario 2: Entwicklung mit TEST-Daten
```bash
# PROD Daten nach TEST kopieren
./scripts/backup.sh prod full
# (warten bis vollständig)
./scripts/restore.sh test --latest

# TEST Daten sind jetzt identisch mit PROD
```

### Scenario 3: Regelmäßig Status prüfen
```bash
# Im Terminal aufrufen (zB alle 2 Wochen)
./scripts/backup-status.sh

# Falls Disk läuft voll:
./scripts/backup-status.sh --clean
docker exec physio-prod-backup /usr/local/bin/mysql_backup.sh cleanup
```

### Scenario 4: Notfall-Recovery
```bash
# Liste Backups
./scripts/restore.sh prod --list

# Wähle ein älteres Backup wenn neues corrupt ist
./scripts/restore.sh prod prod_full_20260215_180000.sql.gz

# Oder einfach:
./scripts/restore.sh prod --latest
```

---

## 🛠️ Troubleshooting

### Script findet Container nicht
```bash
# Prüfe ob Stack läuft
docker ps | grep backup

# Starte Stack falls nötig
docker compose -f compose.test.yml --env-file .env.test up -d
```

### Fehler beim Backup
```bash
# Logs prüfen
./scripts/backup-status.sh test

# Oder direkt in Container nachschauen
docker exec physio-test-backup tail -50 /var/log/mysql_backup.log
```

### Restore schlägt fehl
```bash
# Stelle sicher dass Datenbank läuft
docker ps | grep -db

# Prüfe Backup-Datei ist vorhanden
ls -lh ./backups/

# Versuche von älterem Backup
./scripts/restore.sh test --list  # (Liste auswählen)
```

---

## 📝 Notes

- ✅ Scripts sind portable (arbeiten mit alle Stages)
- ✅ Automatische Fehlerbehandlung & hilfreiche Messages
- ✅ Colored Output für bessere Lesbarkeit
- ✅ Parameter-Validierung eingebaut
- ✅ Für Windows/Bash/Zsh getestet

---

Weitere Infos: `README.md` → Sektion "Backup & Recovery System"
