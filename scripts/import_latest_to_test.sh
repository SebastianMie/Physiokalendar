#!/usr/bin/env bash
# Import the latest dev dump (backups/dev_dump_*.sql.gz) into the test DB (physio-test-db)
# - creates a backup of the test DB before import
# - disables FK checks during import
# - uses --force to continue on SQL errors
# Usage: ./scripts/import_latest_to_test.sh [-f <file>] [-y]

set -euo pipefail
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
BACKUP_DIR="$ROOT_DIR/backups"
mkdir -p "$BACKUP_DIR"

FORCE=false
FILE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    -y) FORCE=true; shift ;;
    -f|--file) FILE="$2"; shift 2 ;;
    -h|--help) echo "Usage: $0 [-f <dump-file>] [-y]"; exit 0 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

if [[ -z "$FILE" ]]; then
  FILE=$(ls -1t "$BACKUP_DIR"/dev_dump_*.sql.gz 2>/dev/null | head -n1 || true)
  if [[ -z "$FILE" ]]; then
    echo "ERROR: No dev dump found in $BACKUP_DIR (look for dev_dump_*.sql.gz)." >&2
    exit 2
  fi
fi

echo "Selected dump: $FILE"

if [[ "$FORCE" != true ]]; then
  read -p "Import $FILE into test DB (physio-test-db)? This will overwrite test DB data. Continue? [y/N] " ans
  case "$ans" in
    [Yy]*) ;;
    *) echo "Aborted."; exit 0;;
  esac
fi

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
TEST_BACKUP="$BACKUP_DIR/test_backup_before_import_$TIMESTAMP.sql.gz"

# ensure test DB container is running
if ! docker ps --format '{{.Names}}' | grep -q '^physio-test-db$'; then
  echo "physio-test-db not running. Starting test DB via docker compose..."
  docker compose -f compose.test.yml --env-file .env.test up -d physio-test-db
  echo "Waiting 3s for DB to accept connections..."
  sleep 3
fi

echo "Backing up current test DB to: $TEST_BACKUP"
docker exec -i physio-test-db sh -c 'exec mysqldump -uphysiouser -p"testpassword" --single-transaction --quick --no-tablespaces --routines --triggers --events physiocalendar_test' | gzip > "$TEST_BACKUP" || echo "Warning: test DB backup failed (continuing)"

# Import: replace DB name and import with disabled FK checks
echo "Importing $FILE into physio-test-db (errors will be skipped)..."
(gunzip -c "$FILE" | sed 's/\bphysiocalendar_dev\b/physiocalendar_test/g') | docker exec -i physio-test-db sh -c "bash -lc 'echo \"SET FOREIGN_KEY_CHECKS=0;\"; cat -; echo \"SET FOREIGN_KEY_CHECKS=1;\"' | mysql -uphysiouser -p\"testpassword\" --force physiocalendar_test"

echo "Import finished. Restarting backend and showing last logs..."
docker compose -f compose.test.yml --env-file .env.test restart physio-test-backend || true

docker compose -f compose.test.yml --env-file .env.test logs physio-test-backend --tail=200

echo "Done. Test DB backup before import: $TEST_BACKUP"
exit 0
