# 🔧 Physiokalender - Backup & Restore Scripts

Schnelle Referenz für alle verfügbaren Backup-Scripts.

---

## 📚 Überblick

| Script | Funktion | Stage-Parameter |
|--------|----------|-----------------|
| `backup.sh` | Backup triggern | test, prod |
| `restore.sh` | Datenbank wiederherstellen | test, prod |
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
