#!/bin/bash
# MySQL Backup Script für Docker-Container (Inkrementell, täglich)
# Speichert vollständige und inkrementelle Backups, kompatibel für Restore
# Legt Backups im /backup-Verzeichnis ab (per Volume mountbar)

# Konfiguration
DB_NAME="${DB_NAME:-physiocalendar_test}"
DB_USER="${DB_USER:-physiouser}"
DB_PASSWORD="${DB_PASSWORD:-testpassword}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
BACKUP_DIR="/backup"
DATE=$(date +"%Y-%m-%d")
WEEKDAY=$(date +"%u")

# Erstelle Backup-Verzeichnis falls nicht vorhanden
mkdir -p "$BACKUP_DIR"

# Wöchentlicher Full Dump (z.B. Sonntags)
if [ "$WEEKDAY" -eq 7 ]; then
    echo "[INFO] Erstelle vollständiges Backup: $BACKUP_DIR/full_$DATE.sql.gz"
    mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" --single-transaction --routines --triggers --events "$DB_NAME" | gzip > "$BACKUP_DIR/full_$DATE.sql.gz"
    ln -sf "$BACKUP_DIR/full_$DATE.sql.gz" "$BACKUP_DIR/latest_full.sql.gz"
else
    # Inkrementelles Backup (binlog) — fallback auf Full Dump, falls Binlog nicht aktiv
    echo "[INFO] Prüfe, ob Binary-Logging aktiviert ist..."
    if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" -e "SHOW BINARY LOGS;" >/dev/null 2>&1; then
        echo "[INFO] Binary-Logging aktiv — erstelle binlog-basiertes inkrementelles Backup"
        mysqladmin -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" flush-logs
        BINLOG_FILE=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" -e "SHOW BINARY LOGS;" | tail -n 1 | awk '{print $1}')
        mysqlbinlog --read-from-remote-server --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" --password="$DB_PASSWORD" "$BINLOG_FILE" | gzip > "$BACKUP_DIR/inc_$DATE.sql.gz"
    else
        echo "[WARN] Binary-Logging nicht aktiv — erstelle zusätzliches vollständiges Backup als Fallback"
        mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" --single-transaction --routines --triggers --events "$DB_NAME" | gzip > "$BACKUP_DIR/full_fallback_$DATE.sql.gz"
        ln -sf "$BACKUP_DIR/full_fallback_$DATE.sql.gz" "$BACKUP_DIR/latest_full.sql.gz"
    fi
fi

echo "[INFO] Backup abgeschlossen: $DATE"
