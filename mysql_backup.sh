#!/bin/bash

################################################################################
# PHYSIOKALENDAR MYSQL BACKUP SCRIPT
#
# Erstellt Full und Incremental Backups mit konsistenter Benennung
# Format: {env}_full_YYYYMMDD_HHMMSS.sql.gz oder {env}_inc_YYYYMMDD_HHMMSS.sql.gz
#
# Verwendung:
#   ./mysql_backup.sh                # Auto-detect type (full/incremental)
#   ./mysql_backup.sh full           # Force Full Backup
#   ./mysql_backup.sh incremental    # Force Incremental Backup
#   ./mysql_backup.sh cleanup        # Alte Full Backups löschen (>28 Tage)
#
# Umgebungsvariablen:
#   DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
#   DB_ENV (prod oder test) - wird auto-erkannt wenn nicht gesetzt
#   BACKUP_DIR (default: /backup)
#   RETENTION_DAYS (default: 28)
################################################################################

set -e

# ============================================================================
# CONFIGURATION
# ============================================================================

DB_NAME="${DB_NAME:-physiocalendar_test}"
DB_USER="${DB_USER:-physiouser}"
DB_PASSWORD="${DB_PASSWORD:-testpassword}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
BACKUP_DIR="${BACKUP_DIR:-/backup}"
RETENTION_DAYS="${RETENTION_DAYS:-28}"
BACKUP_TYPE="${1:-auto}"  # full, incremental, cleanup, oder auto

# Auto-detect environment from DB_NAME
if [ -z "$DB_ENV" ]; then
    if [[ "$DB_NAME" == *"prod"* ]]; then
        DB_ENV="prod"
    else
        DB_ENV="test"
    fi
fi

# Timestamps
DATE=$(date +"%Y%m%d")
TIME=$(date +"%H%M%S")
DATETIME="${DATE}_${TIME}"
LOG_FILE="${BACKUP_DIR}/backup.log"

# ============================================================================
# FUNCTIONS
# ============================================================================

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

error() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: $*" | tee -a "$LOG_FILE" >&2
    exit 1
}

# Create backup filename with auto-incrementing counter
get_backup_filename() {
    local type=$1
    local pattern="${BACKUP_DIR}/${DB_ENV}_${type}_${DATE}_*.sql.gz"
    local counter=1

    # Count existing backups with same type and date
    if ls $pattern 2>/dev/null | grep -q .; then
        counter=$(($(ls $pattern 2>/dev/null | wc -l) + 1))
    fi

    printf "%s/%s_%s_%s_%03d.sql.gz" "$BACKUP_DIR" "$DB_ENV" "$type" "$DATETIME" "$counter"
}

# Check MySQL connection
check_mysql_connection() {
    if ! mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" -e "SELECT 1;" >/dev/null 2>&1; then
        error "Kann nicht zur MySQL-Datenbank verbinden: $DB_HOST:$DB_PORT"
    fi
    log "✓ MySQL-Verbindung OK"
}

# Create full backup
create_full_backup() {
    log "=== FULL BACKUP STARTEN ==="
    log "Umgebung: $DB_ENV | Datenbank: $DB_NAME | Host: $DB_HOST"

    check_mysql_connection

    local backup_file=$(get_backup_filename "full")
    log "Zieldatei: $backup_file"

    if mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
        --single-transaction --routines --triggers "$DB_NAME" | gzip > "$backup_file"; then

        local size=$(du -h "$backup_file" | cut -f1)
        log "✓ Full Backup erfolgreich erstellt: $size"
        echo "$backup_file"
        return 0
    else
        error "Full Backup fehlgeschlagen"
    fi
}

# Create incremental backup
create_incremental_backup() {
    log "=== INCREMENTAL BACKUP STARTEN ==="
    log "Umgebung: $DB_ENV | Datenbank: $DB_NAME | Host: $DB_HOST"

    check_mysql_connection

    local backup_file=$(get_backup_filename "inc")
    log "Zieldatei: $backup_file"

    # Flush logs to create new binary log
    if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" -e "FLUSH BINARY LOGS;" 2>/dev/null; then
        log "✓ Binary Logs geflusht"
    fi

    # Try to get latest binary log
    local binlog_file=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
        -B -N -e "SHOW BINARY LOGS;" 2>/dev/null | tail -n 1 | awk '{print $1}')

    if [ -z "$binlog_file" ]; then
        log "⚠ Kein Binary Log gefunden - erstelle stattdessen Full Backup als Fallback"
        create_full_backup
        return 0
    fi

    log "Extrahiere Binary Log: $binlog_file"

    if mysqlbinlog --read-from-remote-server --host="$DB_HOST" --port="$DB_PORT" \
        --user="$DB_USER" --password="$DB_PASSWORD" "$binlog_file" 2>/dev/null | gzip > "$backup_file"; then

        local size=$(du -h "$backup_file" | cut -f1)
        log "✓ Incremental Backup erfolgreich erstellt: $size"
        echo "$backup_file"
        return 0
    else
        log "⚠ Binary Log Extraktion fehlgeschlagen - erstelle stattdessen Full Backup als Fallback"
        create_full_backup
        return 0
    fi
}

# Cleanup old full backups (retention policy)
cleanup_old_backups() {
    log "=== CLEANUP ALTE BACKUPS ==="
    log "Retention Policy: $RETENTION_DAYS Tage"

    local deleted_count=0

    # Find and delete full backups older than retention days
    while IFS= read -r file; do
        rm -f "$file"
        log "✗ Gelöscht: $(basename "$file")"
        ((deleted_count++))
    done < <(find "$BACKUP_DIR" -name "${DB_ENV}_full_*.sql.gz" -mtime +$RETENTION_DAYS)

    if [ $deleted_count -eq 0 ]; then
        log "✓ Keine alten Backups gefunden"
    else
        log "✓ $deleted_count alte Backups gelöscht"
    fi
}

# ============================================================================
# MAIN
# ============================================================================

# Ensure backup directory exists
mkdir -p "$BACKUP_DIR"

log "╔════════════════════════════════════════════════════════════════╗"
log "║         PHYSIOKALENDAR MYSQL BACKUP SCRIPT                     ║"
log "║                                                                ║"
log "║  Backup-Typ: $BACKUP_TYPE"
log "║  Umgebung:   $DB_ENV"
log "║  Verzeichnis: $BACKUP_DIR"
log "║  Log-Datei:  $LOG_FILE"
log "╚════════════════════════════════════════════════════════════════╝"

case "$BACKUP_TYPE" in
    full)
        create_full_backup
        cleanup_old_backups
        ;;
    incremental)
        create_incremental_backup
        ;;
    cleanup)
        cleanup_old_backups
        ;;
    auto)
        # Default: incremental in the morning/day, full in the evening
        HOUR=$(date +%H)
        if [ "$HOUR" -ge 18 ] || [ "$HOUR" -lt 7 ]; then
            # 18:00 - 06:59: Full backup
            create_full_backup
            cleanup_old_backups
        else
            # 07:00 - 17:59: Incremental backup
            create_incremental_backup
        fi
        ;;
    *)
        error "Ungültiger Backup-Typ: $BACKUP_TYPE. Nutze: full, incremental, cleanup, oder auto"
        ;;
esac

log "=== BACKUP FERTIG ==="
