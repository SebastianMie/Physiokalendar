#!/bin/bash

################################################################################
# PHYSIOKALENDAR IMPORT STAGING SCRIPT
#
# Importiert ein Backup von einer Quell-Stage in eine Ziel-Stage
# Nützlich um z.B. Test-Backups in Prod einzuspielen
#
# Verwendung:
#   ./scripts/import-staging.sh test prod              # Neuestes test Backup → prod
#   ./scripts/import-staging.sh test prod --latest     # Explizit neuestes Backup
#   ./scripts/import-staging.sh test prod 2026-02-19   # Spezifisches Datum
#   ./scripts/import-staging.sh test prod --list       # Verfügbare Backups aus test auflisten
#
# Parameter:
#   SOURCE_STAGE: Quell-Stage (test oder prod) - von wo das Backup kommt
#   TARGET_STAGE: Ziel-Stage (test oder prod) - wohin das Backup gespielt wird
#   BACKUP:       Backup-Datei / --latest / --list (default: --latest)
################################################################################

set -e

# ============================================================================
# CONFIGURATION
# ============================================================================

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

# Parse arguments
SOURCE_STAGE="${1:-}"
TARGET_STAGE="${2:-}"
BACKUP_SPEC="${3:-latest}"

# Validate source stage
if [ -z "$SOURCE_STAGE" ]; then
    echo "❌ ERROR: Quell-Stage erforderlich (test oder prod)"
    echo ""
    echo "Verwendung:"
    echo "  ./scripts/import-staging.sh test prod              # Neuestes test Backup → prod"
    echo "  ./scripts/import-staging.sh test prod --latest"
    echo "  ./scripts/import-staging.sh test prod 2026-02-19"
    echo "  ./scripts/import-staging.sh test prod --list"
    echo ""
    exit 1
fi

if [ -z "$TARGET_STAGE" ]; then
    echo "❌ ERROR: Ziel-Stage erforderlich (test oder prod)"
    echo ""
    echo "Verwendung:"
    echo "  ./scripts/import-staging.sh test prod              # Neuestes test Backup → prod"
    echo "  ./scripts/import-staging.sh test prod --latest"
    echo "  ./scripts/import-staging.sh test prod 2026-02-19"
    echo "  ./scripts/import-staging.sh test prod --list"
    echo ""
    exit 1
fi

if [[ ! "$SOURCE_STAGE" =~ ^(test|prod)$ ]]; then
    echo "❌ ERROR: Ungültige Quell-Stage '$SOURCE_STAGE'. Nur 'test' oder 'prod' erlaubt."
    exit 1
fi

if [[ ! "$TARGET_STAGE" =~ ^(test|prod)$ ]]; then
    echo "❌ ERROR: Ungültige Ziel-Stage '$TARGET_STAGE'. Nur 'test' oder 'prod' erlaubt."
    exit 1
fi

if [ "$SOURCE_STAGE" == "$TARGET_STAGE" ]; then
    echo "❌ ERROR: Quell-Stage und Ziel-Stage müssen unterschiedlich sein!"
    exit 1
fi

# ============================================================================
# LIST BACKUPS AND EXIT
# ============================================================================

if [ "$BACKUP_SPEC" == "--list" ]; then
    echo ""
    echo "📁 Verfügbare Backups von '$SOURCE_STAGE':"
    echo ""
    if ls "${PROJECT_ROOT}/backups/${SOURCE_STAGE}"_*.sql.gz 2>/dev/null | head -1 > /dev/null; then
        ls -lth "${PROJECT_ROOT}/backups/${SOURCE_STAGE}"_*.sql.gz | awk '{printf "  %-50s %6s  %s %s\n", $9, $5, $6, $7}'
    else
        echo "  Keine Backups gefunden!"
    fi
    echo ""
    exit 0
fi

# ============================================================================
# FIND BACKUP FILE
# ============================================================================

BACKUP_FILE=""

if [ "$BACKUP_SPEC" == "--latest" ] || [ "$BACKUP_SPEC" == "latest" ]; then
    echo "🔍 Suche neuestes Backup von '$SOURCE_STAGE'..."
    BACKUP_FILE=$(ls -t "${PROJECT_ROOT}/backups/${SOURCE_STAGE}"_*.sql.gz 2>/dev/null | head -1)
    if [ -z "$BACKUP_FILE" ]; then
        echo "❌ ERROR: Keine Backups gefunden für '$SOURCE_STAGE'"
        exit 1
    fi
elif [[ "$BACKUP_SPEC" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2} ]]; then
    # Backup by date pattern
    echo "🔍 Suche Backup von '$SOURCE_STAGE' mit Datum '$BACKUP_SPEC'..."
    BACKUP_FILE=$(ls -t "${PROJECT_ROOT}/backups/${SOURCE_STAGE}_${BACKUP_SPEC}"*.sql.gz 2>/dev/null | head -1)
    if [ -z "$BACKUP_FILE" ]; then
        echo "❌ ERROR: Kein Backup mit Datum '$BACKUP_SPEC' gefunden!"
        ./scripts/import-staging.sh "$SOURCE_STAGE" "$TARGET_STAGE" --list
        exit 1
    fi
else
    # Specific backup file
    if [[ "$BACKUP_SPEC" != /* ]]; then
        BACKUP_FILE="${PROJECT_ROOT}/backups/$BACKUP_SPEC"
    else
        BACKUP_FILE="$BACKUP_SPEC"
    fi
fi

# Validate backup exists
if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ ERROR: Backup-Datei nicht gefunden: $BACKUP_FILE"
    exit 1
fi

# ============================================================================
# LOAD ENVIRONMENT FOR TARGET STAGE
# ============================================================================

TARGET_ENV_FILE="${PROJECT_ROOT}/.env.${TARGET_STAGE}"
if [ ! -f "$TARGET_ENV_FILE" ]; then
    echo "❌ ERROR: Environment-Datei nicht gefunden: $TARGET_ENV_FILE"
    exit 1
fi

echo "📄 Lade Environment: $TARGET_ENV_FILE"
source "$TARGET_ENV_FILE"

TARGET_COMPOSE_FILE="${PROJECT_ROOT}/compose.${TARGET_STAGE}.yml"
if [ ! -f "$TARGET_COMPOSE_FILE" ]; then
    echo "❌ ERROR: Compose-Datei nicht gefunden: $TARGET_COMPOSE_FILE"
    exit 1
fi

# Get database container name and credentials
TARGET_DATABASE_CONTAINER="${COMPOSE_PROJECT_NAME:-physio-${TARGET_STAGE}}-db"
TARGET_DB_NAME="${DB_NAME:-physiocalendar}"
if [ "$TARGET_STAGE" == "test" ]; then
    TARGET_DB_NAME="${DB_NAME:=physiocalendar_test}"
else
    TARGET_DB_NAME="${DB_NAME:=physiocalendar}"
fi

# ============================================================================
# SHOW SUMMARY
# ============================================================================

BACKUP_BASENAME=$(basename "$BACKUP_FILE")
BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║         IMPORT STAGING SCRIPT                                  ║"
echo "║                                                                ║"
echo "║  ⚠️  WARNUNG: Diese Aktion wird die Datenbank ÜBERSCHREIBEN!   ║"
echo "║                                                                ║"
echo "║  Quelle:          $SOURCE_STAGE (Backup)"
echo "║  Ziel:            $TARGET_STAGE (Datenbank: $TARGET_DB_NAME)"
echo "║  Backup-Datei:    $BACKUP_BASENAME"
echo "║  Größe:           $BACKUP_SIZE"
echo "║  Ziel-Container:  $TARGET_DATABASE_CONTAINER"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# ============================================================================
# CONFIRM ACTION
# ============================================================================

read -p "Möchten Sie wirklich fortfahren? (Geben Sie 'JA' ein) " -r confirmation
if [ "$confirmation" != "JA" ]; then
    echo "❌ Abgebrochen."
    exit 1
fi

# ============================================================================
# EXECUTE IMPORT
# ============================================================================

echo ""
echo "🔄 Starte Import-Prozess..."
echo ""

# Check if target database container is running
if ! docker ps | grep -q "$TARGET_DATABASE_CONTAINER"; then
    echo "⚠️  Ziel-Datenbank-Container ist nicht laufend. Starte Stack für '$TARGET_STAGE'..."
    docker compose -f "$TARGET_COMPOSE_FILE" --env-file "$TARGET_ENV_FILE" up -d --build
    echo "⏳ Warte 10 Sekunden bis Datenbank bereit ist..."
    sleep 10
fi

# Perform import
echo "📥 Importiere Backup in '$TARGET_STAGE' Datenbank: $TARGET_DB_NAME"
echo ""

if gunzip -c "$BACKUP_FILE" | \
    docker exec -i "$TARGET_DATABASE_CONTAINER" \
    mysql -h localhost -u "${DB_USER:-physiouser}" -p"${DB_PASSWORD:-testpassword}" "$TARGET_DB_NAME"; then

    echo ""
    echo "✅ Import erfolgreich abgeschlossen!"
    echo ""
    echo "📊 Datenbank-Statistiken ($TARGET_STAGE):"
    docker exec "$TARGET_DATABASE_CONTAINER" \
        mysql -u "${DB_USER:-physiouser}" -p"${DB_PASSWORD:-testpassword}" "$TARGET_DB_NAME" \
        -e "SELECT 'Patienten' as Item, COUNT(*) as Count FROM patients UNION SELECT 'Termine', COUNT(*) FROM appointments UNION SELECT 'Therapeuten', COUNT(*) FROM therapist;" \
        || echo "   (Kann Statistiken nicht abrufen)"
    echo ""

else
    echo ""
    echo "❌ Import fehlgeschlagen!"
    echo ""
    echo "📋 Container-Logs:"
    docker logs "$TARGET_DATABASE_CONTAINER" | tail -20
    exit 1
fi

# ============================================================================
# SUMMARY
# ============================================================================

echo "🎉 Daten von '$SOURCE_STAGE' erfolgreich in '$TARGET_STAGE' importiert!"
echo ""
echo "   Quell-Backup: $BACKUP_BASENAME"
echo "   Ziel-Datenbank: $TARGET_DB_NAME"
echo ""
echo "⚠️  Bitte überprüfen Sie die Daten in '$TARGET_STAGE' vor weiterer Verwendung!"
echo ""
