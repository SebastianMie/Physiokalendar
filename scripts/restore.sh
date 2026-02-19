#!/bin/bash

################################################################################
# PHYSIOKALENDAR BACKUP RESTORE SCRIPT
#
# Stellt eine Datenbank aus einem Backup wieder her
#
# Verwendung:
#   ./scripts/restore.sh test backup_filename.sql.gz
#   ./scripts/restore.sh prod 2026-02-19_180000.sql.gz
#   ./scripts/restore.sh test --latest  # Newest backup
#   ./scripts/restore.sh test --list    # List available backups
#
# Parameter:
#   STAGE: test oder prod (erforderlich)
#   BACKUP: Dateiname oder --latest oder --list
################################################################################

set -e

# ============================================================================
# CONFIGURATION
# ============================================================================

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

# Parse arguments
STAGE="${1:-}"
BACKUP_FILE="${2:-}"

# Validate stage
if [ -z "$STAGE" ]; then
    echo "❌ ERROR: Stage erforderlich (test oder prod)"
    echo ""
    echo "Verwendung:"
    echo "  ./scripts/restore.sh test backup_filename.sql.gz"
    echo "  ./scripts/restore.sh test --latest"
    echo "  ./scripts/restore.sh test --list"
    echo ""
    exit 1
fi

if [[ ! "$STAGE" =~ ^(test|prod)$ ]]; then
    echo "❌ ERROR: Ungültige Stage '$STAGE'. Nur 'test' oder 'prod' erlaubt."
    exit 1
fi

# ============================================================================
# LOAD ENVIRONMENT & VALIDATION
# ============================================================================

# Load environment file
ENV_FILE="${PROJECT_ROOT}/.env.${STAGE}"
if [ ! -f "$ENV_FILE" ]; then
    echo "❌ ERROR: Environment-Datei nicht gefunden: $ENV_FILE"
    exit 1
fi

echo "📄 Lade Environment: $ENV_FILE"
source "$ENV_FILE"

# Load compose file
COMPOSE_FILE="${PROJECT_ROOT}/compose.${STAGE}.yml"
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "❌ ERROR: Compose-Datei nicht gefunden: $COMPOSE_FILE"
    exit 1
fi

# Get container names
DATABASE_CONTAINER="${COMPOSE_PROJECT_NAME:-physio-${STAGE}}-db"

# ============================================================================
# HANDLE SPECIAL COMMANDS
# ============================================================================

if [ "$BACKUP_FILE" == "--list" ]; then
    echo ""
    echo "📁 Verfügbare Backups für $STAGE:"
    echo ""
    if ls "${PROJECT_ROOT}/backups/${STAGE}"_*.sql.gz 2>/dev/null | head -1 > /dev/null; then
        ls -lth "${PROJECT_ROOT}/backups/${STAGE}"_*.sql.gz | awk '{printf "  %-50s %6s  %s %s\n", $9, $5, $6, $7}'
    else
        echo "  Keine Backups gefunden!"
    fi
    echo ""
    exit 0
fi

if [ "$BACKUP_FILE" == "--latest" ]; then
    echo "Suche neuestes Backup für $STAGE..."
    BACKUP_FILE=$(ls -t "${PROJECT_ROOT}/backups/${STAGE}"_*.sql.gz 2>/dev/null | head -1)
    if [ -z "$BACKUP_FILE" ]; then
        echo "❌ ERROR: Keine Backups gefunden für $STAGE"
        exit 1
    fi
fi

# ============================================================================
# VALIDATE BACKUP FILE
# ============================================================================

if [ -z "$BACKUP_FILE" ]; then
    echo "❌ ERROR: Backup-Datei erforderlich"
    echo ""
    echo "Verfügbare Backups:"
    ./scripts/restore.sh "$STAGE" --list
    exit 1
fi

# Resolve full path
if [[ "$BACKUP_FILE" != /* ]]; then
    BACKUP_FILE="${PROJECT_ROOT}/backups/$BACKUP_FILE"
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ ERROR: Backup-Datei nicht gefunden: $BACKUP_FILE"
    exit 1
fi

# Get database info from environment
DB_NAME="${DB_NAME:-physiocalendar}"
if [ "$STAGE" == "test" ]; then
    DB_NAME="${DB_NAME:=physiocalendar_test}"
else
    DB_NAME="${DB_NAME:=physiocalendar}"
fi

# ============================================================================
# CONFIRM RESTORE
# ============================================================================

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║              BACKUP RESTORE SCRIPT                             ║"
echo "║                                                                ║"
echo "║  ⚠️  WARNUNG: Diese Aktion wird die Datenbank ÜBERSCHREIBEN!   ║"
echo "║                                                                ║"
echo "║  Stage:           $STAGE"
echo "║  Datenbank:       $DB_NAME"
echo "║  Backup-Datei:    $(basename $BACKUP_FILE)"
echo "║  Größe:           $(du -h $BACKUP_FILE | cut -f1)"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

read -p "Möchten Sie wirklich fortfahren? (Geben Sie 'JA' ein) " -r confirmation
if [ "$confirmation" != "JA" ]; then
    echo "❌ Abgebrochen."
    exit 1
fi

# ============================================================================
# EXECUTE RESTORE
# ============================================================================

echo ""
echo "🔄 Starte Restore-Prozess..."
echo ""

# Check if database container is running
if ! docker ps | grep -q "$DATABASE_CONTAINER"; then
    echo "⚠️  Datenbank-Container ist nicht laufend. Starte Stack..."
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build
    echo "⏳ Warte 10 Sekunden bis Datenbank bereit ist..."
    sleep 10
fi

# Perform restore
BACKUP_BASENAME=$(basename "$BACKUP_FILE")

echo "📥 Stelle Datenbank wieder her aus: $BACKUP_BASENAME"
gunzip -c "$BACKUP_FILE" | \
    docker exec -i "$DATABASE_CONTAINER" \
    mysql -h localhost -u "${DB_USER:-physiouser}" -p"${DB_PASSWORD:-testpassword}" "$DB_NAME"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Restore erfolgreich abgeschlossen!"
    echo ""
    echo "📊 Datenbank-Statistiken:"
    docker exec "$DATABASE_CONTAINER" \
        mysql -u "${DB_USER:-physiouser}" -p"${DB_PASSWORD:-testpassword}" "$DB_NAME" \
        -e "SELECT COUNT(*) as patients FROM patients; SELECT COUNT(*) as appointments FROM appointments; SELECT COUNT(*) as therapists FROM therapist;" \
        || echo "   (Kann Statistiken nicht abrufen)"
    echo ""
else
    echo ""
    echo "❌ Restore fehlgeschlagen!"
    exit 1
fi
