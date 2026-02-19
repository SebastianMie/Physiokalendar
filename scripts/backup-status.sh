#!/bin/bash

################################################################################
# PHYSIOKALENDAR BACKUP STATUS SCRIPT
#
# Zeigt Backup-Status für alle Umgebungen an
#
# Verwendung:
#   ./scripts/backup-status.sh          # Show all
#   ./scripts/backup-status.sh test     # Show only test
#   ./scripts/backup-status.sh prod     # Show only prod
#   ./scripts/backup-status.sh --clean  # Cleanup overview
################################################################################

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

STAGE="${1:-all}"
BACKUP_DIR="${PROJECT_ROOT}/backups"

# ============================================================================
# COLOR & FORMATTING
# ============================================================================

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_stage_header() {
    local stage=$1
    echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  📊 BACKUP STATUS: $stage (Umgebung)${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
    echo ""
}

# ============================================================================
# SHOW BACKUP STATUS FOR ONE STAGE
# ============================================================================

show_stage_status() {
    local stage=$1
    local pattern="${BACKUP_DIR}/${stage}_*.sql.gz"

    print_stage_header "$stage"

    if ! ls $pattern 2>/dev/null | head -1 > /dev/null; then
        echo -e "${YELLOW}  ⚠️  Keine Backups gefunden${NC}"
        echo ""
        return
    fi

    echo "  📁 FULL BACKUPS"
    echo "  ─────────────────────────────────────────────────────────────"

    # Full backups
    if ls "${BACKUP_DIR}/${stage}_full_"*.sql.gz 2>/dev/null | head -1 > /dev/null; then
        ls -t "${BACKUP_DIR}/${stage}_full_"*.sql.gz 2>/dev/null | while read file; do
            basename="$(basename "$file")"
            size="$(du -h "$file" | cut -f1)"
            date="$(stat -f%Sm -t"%Y-%m-%d %H:%M:%S" "$file" 2>/dev/null || stat -c%y "$file" 2>/dev/null | cut -d' ' -f1-2)"
            printf "  ${GREEN}✓${NC}  %-40s  %7s  %s\n" "$basename" "$size" "$date"
        done
    else
        echo -e "  ${RED}✗${NC}  Keine Full Backups vorhanden"
    fi

    echo ""
    echo "  📁 INCREMENTAL BACKUPS"
    echo "  ─────────────────────────────────────────────────────────────"

    # Incremental backups
    if ls "${BACKUP_DIR}/${stage}_inc_"*.sql.gz 2>/dev/null | head -1 > /dev/null; then
        ls -t "${BACKUP_DIR}/${stage}_inc_"*.sql.gz 2>/dev/null | while read file; do
            basename="$(basename "$file")"
            size="$(du -h "$file" | cut -f1)"
            date="$(stat -f%Sm -t"%Y-%m-%d %H:%M:%S" "$file" 2>/dev/null || stat -c%y "$file" 2>/dev/null | cut -d' ' -f1-2)"
            printf "  ${GREEN}✓${NC}  %-40s  %7s  %s\n" "$basename" "$size" "$date"
        done
    else
        echo -e "  ${YELLOW}⚠${NC}  Keine Incremental Backups vorhanden"
    fi

    echo ""

    # Storage usage
    TOTAL_SIZE=$(du -sh "${BACKUP_DIR}" 2>/dev/null | cut -f1)
    STAGE_SIZE=$(du -sh "${BACKUP_DIR}/${stage}"* 2>/dev/null | awk '{s+=$1} END {print s}' || echo "0B")

    echo "  💾 SPEICHERVORKOMMEN"
    echo "  ─────────────────────────────────────────────────────────────"
    printf "  Gesamt (alle Stages):  %s\n" "$TOTAL_SIZE"
    printf "  Nur $stage Backups:      %s\n" "$STAGE_SIZE"
    echo ""

    # Latest backups summary
    echo "  🕐 NEUESTE BACKUPS"
    echo "  ─────────────────────────────────────────────────────────────"

    LATEST_FULL=$(ls -t "${BACKUP_DIR}/${stage}_full_"*.sql.gz 2>/dev/null | head -1)
    LATEST_INC=$(ls -t "${BACKUP_DIR}/${stage}_inc_"*.sql.gz 2>/dev/null | head -1)

    if [ -n "$LATEST_FULL" ]; then
        FULL_DATE=$(stat -f%Sm -t"%Y-%m-%d %H:%M:%S" "$LATEST_FULL" 2>/dev/null || stat -c%y "$LATEST_FULL" 2>/dev/null | cut -d' ' -f1-2)
        printf "  Full:         %s\n" "$FULL_DATE"
    fi

    if [ -n "$LATEST_INC" ]; then
        INC_DATE=$(stat -f%Sm -t"%Y-%m-%d %H:%M:%S" "$LATEST_INC" 2>/dev/null || stat -c%y "$LATEST_INC" 2>/dev/null | cut -d' ' -f1-2)
        printf "  Incremental:  %s\n" "$INC_DATE"
    fi

    echo ""
}

# ============================================================================
# SHOW CLEANUP ANALYSIS
# ============================================================================

show_cleanup_analysis() {
    echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  🧹 CLEANUP ANALYSE${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
    echo ""

    RETENTION_DAYS=28
    CUTOFF_DATE=$(date -d "$RETENTION_DAYS days ago" +%s 2>/dev/null || date -v-${RETENTION_DAYS}d +%s 2>/dev/null)

    for stage in test prod; do
        echo "  📊 Stage: $stage (Retention: $RETENTION_DAYS Tage)"
        echo "  ─────────────────────────────────────────────────────────────"

        old_count=0
        old_size=0

        if ls "${BACKUP_DIR}/${stage}_full_"*.sql.gz 2>/dev/null | head -1 > /dev/null; then
            while read file; do
                file_time=$(stat -f%m "$file" 2>/dev/null || stat -c%Y "$file" 2>/dev/null)

                if [ "$file_time" -lt "$CUTOFF_DATE" ]; then
                    file_size=$(du -h "$file" | cut -f1)
                    printf "    ${RED}✗${NC}  %s (%s) - würde gelöscht\n" "$(basename "$file")" "$file_size"
                    old_count=$((old_count + 1))
                fi
            done < <(ls -t "${BACKUP_DIR}/${stage}_full_"*.sql.gz 2>/dev/null)
        fi

        if [ $old_count -eq 0 ]; then
            echo -e "    ${GREEN}✓${NC}  Keine Backups älter als $RETENTION_DAYS Tage"
        fi
        echo ""
    done
}

# ============================================================================
# MAIN
# ============================================================================

if [ "$STAGE" == "--clean" ]; then
    show_cleanup_analysis
    echo ""
    echo "  💡 Um alte Backups zu löschen, führen Sie aus:"
    echo "     docker exec physio-test-backup /usr/local/bin/mysql_backup.sh cleanup"
    echo "     docker exec physio-prod-backup /usr/local/bin/mysql_backup.sh cleanup"
    echo ""
elif [ "$STAGE" == "all" ]; then
    echo ""
    show_stage_status "test"
    show_stage_status "prod"
    echo ""
else
    if [[ ! "$STAGE" =~ ^(test|prod)$ ]]; then
        echo "❌ Ungültige Stage '$STAGE'. Nur 'test' oder 'prod'."
        exit 1
    fi
    echo ""
    show_stage_status "$STAGE"
    echo ""
fi

echo "  📚 Dokumentation: siehe README.md - Backup & Recovery System"
