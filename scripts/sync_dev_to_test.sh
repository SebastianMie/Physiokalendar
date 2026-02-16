#!/usr/bin/env bash
# Synchronize dev MySQL database -> test MySQL database
# - creates a dump from DEV
# - creates a backup of TEST
# - imports DEV data into TEST
# - import uses --force to skip SQL errors
# Usage: ./scripts/sync_dev_to_test.sh [-y]

set -u
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
BACKUP_DIR="$ROOT_DIR/backups"
mkdir -p "$BACKUP_DIR"

CONFIRM=true
if [ "${1:-}" = "-y" ]; then
  CONFIRM=false
fi

# Load DB settings from .env.dev and .env.test if present
DEV_DB_NAME="physiocalendar_dev"
DEV_DB_USER="physiouser"
DEV_DB_PASS="devpassword"
DEV_DB_PORT=3306
TEST_DB_NAME="physiocalendar_test"
TEST_DB_USER="physiouser"
TEST_DB_PASS="testpassword"
TEST_DB_PORT=3307

load_env_values() {
  local file="$1" prefix="$2"
  [ -f "$file" ] || return
  # read only DB_* keys
  while IFS='=' read -r key val; do
    key=$(echo "$key" | tr -d ' '\n)
    case "$key" in
      DB_NAME) eval "$prefix_DB_NAME=\"${val}\"" ;;
      DB_USER) eval "$prefix_DB_USER=\"${val}\"" ;;
      DB_PASSWORD) eval "$prefix_DB_PASS=\"${val}\"" ;;
      DB_PORT) eval "$prefix_DB_PORT=${val}" ;;
    esac
  done < <(grep -E '^(DB_NAME|DB_USER|DB_PASSWORD|DB_PORT)=' "$file" 2>/dev/null || true)
}

load_env_values "$ROOT_DIR/.env.dev" DEV
load_env_values "$ROOT_DIR/.env.test" TEST

echo "Dev DB:    $DEV_DB_USER@$DEV_DB_PORT/$DEV_DB_NAME"
echo "Test DB:   $TEST_DB_USER@$TEST_DB_PORT/$TEST_DB_NAME"

if $CONFIRM; then
  read -p "Continue and overwrite Test DB with Dev DB? [y/N] " ans
  case "$ans" in
    [Yy]*) ;;
    *) echo "Aborted."; exit 1;;
  esac
fi

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DEV_DUMP="$BACKUP_DIR/dev_dump_$TIMESTAMP.sql.gz"
TEST_BACKUP="$BACKUP_DIR/test_backup_before_import_$TIMESTAMP.sql.gz"

# Helper: run mysqldump on host or inside Docker container if client not available
have_cmd() { command -v "$1" >/dev/null 2>&1; }

# Try host mysqldump first
if have_cmd mysqldump; then
  echo "Dumping dev DB with local mysqldump..."
  mysqldump -h 127.0.0.1 -P "$DEV_DB_PORT" -u "$DEV_DB_USER" -p"$DEV_DB_PASS" \
    --single-transaction --routines --triggers --events "$DEV_DB_NAME" | gzip > "$DEV_DUMP"
else
  # fallback to docker exec (container must be named in compose: physio-dev-db)
  if docker ps --format '{{.Names}}' | grep -q "physio-dev-db"; then
    echo "Local mysqldump not found — using docker exec on 'physio-dev-db' container..."
    docker exec -i physio-dev-db sh -c "exec mysqldump -u$DEV_DB_USER -p\"$DEV_DB_PASS\" --single-transaction --routines --triggers --events $DEV_DB_NAME" | gzip > "$DEV_DUMP"
  else
    echo "ERROR: mysqldump not available and 'physio-dev-db' container not found."; exit 2
  fi
fi

if [ ! -s "$DEV_DUMP" ]; then
  echo "ERROR: dev dump failed or is empty: $DEV_DUMP"; exit 3
fi

echo "Backing up current test DB to $TEST_BACKUP ..."
if have_cmd mysqldump; then
  mysqldump -h 127.0.0.1 -P "$TEST_DB_PORT" -u "$TEST_DB_USER" -p"$TEST_DB_PASS" \
    --single-transaction --routines --triggers --events "$TEST_DB_NAME" | gzip > "$TEST_BACKUP" || echo "Warning: test DB backup failed (continuing)"
else
  if docker ps --format '{{.Names}}' | grep -q "physio-test-db"; then
    docker exec -i physio-test-db sh -c "exec mysqldump -u$TEST_DB_USER -p\"$TEST_DB_PASS\" --single-transaction --routines --triggers --events $TEST_DB_NAME" | gzip > "$TEST_BACKUP" || echo "Warning: test DB backup failed (continuing)"
  else
    echo "Warning: cannot backup test DB (mysql client missing and physio-test-db not found)"
  fi
fi

# Ensure test database exists
if have_cmd mysql; then
  mysql -h 127.0.0.1 -P "$TEST_DB_PORT" -u "$TEST_DB_USER" -p"$TEST_DB_PASS" -e "CREATE DATABASE IF NOT EXISTS \`$TEST_DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" || true
else
  if docker ps --format '{{.Names}}' | grep -q "physio-test-db"; then
    docker exec -i physio-test-db sh -c "mysql -u$TEST_DB_USER -p\"$TEST_DB_PASS\" -e \"CREATE DATABASE IF NOT EXISTS \\\\`$TEST_DB_NAME\\\\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\"" || true
  fi
fi

# Import: replace DB name in dump (if present), disable FK checks, use --force to skip errors
echo "Importing dev dump into test DB (errors will be skipped)..."
if have_cmd mysql; then
  gunzip -c "$DEV_DUMP" | sed "s/\b$DEV_DB_NAME\b/$TEST_DB_NAME/g" | awk 'BEGIN{print "SET FOREIGN_KEY_CHECKS=0;"} {print} END{print "SET FOREIGN_KEY_CHECKS=1;"}' | mysql -h 127.0.0.1 -P "$TEST_DB_PORT" -u "$TEST_DB_USER" -p"$TEST_DB_PASS" --force "$TEST_DB_NAME"
else
  if docker ps --format '{{.Names}}' | grep -q "physio-test-db"; then
    gunzip -c "$DEV_DUMP" | sed "s/\b$DEV_DB_NAME\b/$TEST_DB_NAME/g" | awk 'BEGIN{print "SET FOREIGN_KEY_CHECKS=0;"} {print} END{print "SET FOREIGN_KEY_CHECKS=1;"}' | docker exec -i physio-test-db sh -c "mysql -u$TEST_DB_USER -p\"$TEST_DB_PASS\" --force $TEST_DB_NAME"
  else
    echo "ERROR: cannot import — mysql client not available and physio-test-db not found."; exit 4
  fi
fi

echo "Import finished. Test DB is updated (dump saved: $DEV_DUMP; test backup: $TEST_BACKUP)."

echo "You may now restart the test backend:"
echo "  docker compose -f compose.test.yml --env-file .env.test restart physio-test-backend"

exit 0
