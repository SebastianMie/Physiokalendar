#!/bin/bash

################################################################################
# PHYSIOKALENDAR MANUAL BACKUP TRIGGER SCRIPT
#
# Ermöglicht manuelles Triggering von Backups für TEST und PROD
#
# Verwendung:
#   ./scripts/backup.sh test                 # Auto-detect (full/incremental)
#   ./scripts/backup.sh test full            # Force full backup
#   ./scripts/backup.sh test incremental     # Force incremental
#   ./scripts/backup.sh prod full
#   ./scripts/backup.sh prod cleanup         # Cleanup old backups
#
# Parameter:
#   STAGE: test oder prod (erforderlich)
#   TYPE: full, incremental, cleanup, oder auto (default: auto)
################################################################################

set -e

# ============================================================================
# CONFIGURATION
# ============================================================================

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

# Parse arguments
STAGE="${1:-}"
BACKUP_TYPE="${2:-auto}"

# Validate stage
if [ -z "$STAGE" ]; then
    echo "❌ ERROR: Stage erforderlich (test oder prod)"
    echo ""
    echo "Verwendung:"
    echo "  ./scripts/backup.sh test [full|incremental|cleanup|auto]"
    echo "  ./scripts/backup.sh prod [full|incremental|cleanup|auto]"
    echo ""
    exit 1
fi

if [[ ! "$STAGE" =~ ^(test|prod)$ ]]; then
    echo "❌ ERROR: Ungültige Stage '$STAGE'. Nur 'test' oder 'prod' erlaubt."
    exit 1
fi

if [[ ! "$BACKUP_TYPE" =~ ^(full|incremental|cleanup|auto)$ ]]; then
    echo "❌ ERROR: Ungültiger Backup-Type '$BACKUP_TYPE'. Erlaubt: full, incremental, cleanup, auto"
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

# Get container name
CONTAINER_NAME="${COMPOSE_PROJECT_NAME:-physio-${STAGE}}-backup"

# Check if container is running
if ! docker ps | grep -q "$CONTAINER_NAME"; then
    echo "⚠️  WARNING: Backup-Container ist nicht laufend: $CONTAINER_NAME"
    echo ""
    echo "Starten Sie den Stack mit:"
    echo "  docker compose -f compose.${STAGE}.yml --env-file .env.${STAGE} up -d"
    echo ""
    read -p "Trotzdem fortfahren und Container starten? (j/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Jj]$ ]]; then
        echo "🚀 Starte ${STAGE} Stack..."
        docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build
    else
        exit 1
    fi
fi

# ============================================================================
# EXECUTE BACKUP
# ============================================================================

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║              BACKUP TRIGGER SCRIPT                             ║"
echo "║                                                                ║"
echo "║  Stage:           $STAGE"
echo "║  Container:       $CONTAINER_NAME"
echo "║  Backup-Type:     $BACKUP_TYPE"
echo "║  Verzeichnis:     ./backups"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Execute backup script in container
echo "🔄 Führe Backup aus: /usr/local/bin/mysql_backup.sh $BACKUP_TYPE"
echo ""

if MSYS_NO_PATHCONV=1 docker exec "$CONTAINER_NAME" /usr/local/bin/mysql_backup.sh "$BACKUP_TYPE"; then
    echo ""
    echo "✅ Backup erfolgreich durchgeführt!"
    echo ""
    echo "📁 Verfügbare Backups für $STAGE:"
    ls -lh "${PROJECT_ROOT}/backups/${STAGE}_"* 2>/dev/null | tail -5 | awk '{print "   " $9 " (" $5 ")"}'
    echo ""
else
    echo ""
    echo "❌ Backup fehlgeschlagen!"
    echo ""
    echo "📋 Container-Logs:"
    docker logs "$CONTAINER_NAME" | tail -20
    exit 1
fi

# ============================================================================
# BACKUP SUMMARY
# ============================================================================

LATEST_BACKUP=$(ls -t "${PROJECT_ROOT}/backups/${STAGE}"_* 2>/dev/null | head -1)

if [ -n "$LATEST_BACKUP" ]; then
    BACKUP_SIZE=$(du -h "$LATEST_BACKUP" | cut -f1)
    BACKUP_NAME=$(basename "$LATEST_BACKUP")
    echo "💾 Neuestes Backup: $BACKUP_NAME ($BACKUP_SIZE)"
    echo ""
fi

echo "Weitere Infos: https://github.com/yourusername/physiokalender#-backup--recovery-system"
