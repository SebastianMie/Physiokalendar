#!/usr/bin/env bash
# Usage: ./scripts/restore-backup.sh --env prod|test --file backups/<file>.sql.gz [-y]
set -euo pipefail
ENV= prod
FILE=
FORCE=false
while [[ $# -gt 0 ]]; do
  case $1 in
    --env) ENV="$2"; shift 2;;
    --file) FILE="$2"; shift 2;;
    -y) FORCE=true; shift;;
    -h) echo "Usage: $0 --env prod|test --file <path> [-y]"; exit 0;;
    *) echo "Unknown arg $1"; exit 1;;
  esac
done
[[ -f "$FILE" ]] || { echo "Backup file not found: $FILE"; exit 1; }

if [[ "$ENV" == "prod" ]]; then
  COMPOSE_F="compose.prod.yml"; ENVFILE=".env.prod"; PROJ=physio-prod; DB=physiocalendar_prod; BACKEND=physio-prod-backend; DBC=${PROJ}-db
else
  COMPOSE_F="compose.test.yml"; ENVFILE=".env.test"; PROJ=physio-test; DB=physiocalendar_test; BACKEND=physio-test-backend; DBC=${PROJ}-db
fi

echo "Selected: $FILE → $ENV (DB=$DB)"
if [[ "$FORCE" != true ]]; then
  read -p "Proceed and overwrite ${ENV} DB? [y/N] " a; [[ $a =~ ^[Yy] ]] || exit 0
fi

# 1) snapshot current DB
docker compose -f $COMPOSE_F --env-file $ENVFILE exec -T "$DBC" \
  sh -c "exec mysqldump -uroot -p\"\$MYSQL_ROOT_PASSWORD\" $DB --single-transaction --routines --triggers --events" \
  | gzip > "backups/${ENV}_backup_before_restore_$(date +%Y%m%d_%H%M%S).sql.gz"

# 2) stop backend
COMPOSE_PROJECT_NAME=$PROJ docker compose -f $COMPOSE_F --env-file $ENVFILE stop $BACKEND || true

# 3) ensure DB exists
docker compose -f $COMPOSE_F --env-file $ENVFILE exec -T "$DBC" \
  sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" -e \"CREATE DATABASE IF NOT EXISTS \\\`$DB\\\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\""

# 4) import (replace common dev/test names → target DB)
# detect if dump contains 'physiocalendar_dev' and replace if needed
if gunzip -c "$FILE" | egrep -q 'physiocalendar_dev|physiocalendar_test'; then
  (gunzip -c "$FILE" | sed 's/\\bphysiocalendar_dev\\b/'"$DB"'/g; s/\\bphysiocalendar_test\\b/'"$DB"'/g') \
    | docker compose -f $COMPOSE_F --env-file $ENVFILE exec -T "$DBC" \
      sh -c "bash -lc 'echo \"SET FOREIGN_KEY_CHECKS=0;\"; cat -; echo \"SET FOREIGN_KEY_CHECKS=1;\"' | mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --force $DB"
else
  gunzip -c "$FILE" | docker compose -f $COMPOSE_F --env-file $ENVFILE exec -T "$DBC" \
    sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" $DB"
fi

# 5) restart backend + health
COMPOSE_PROJECT_NAME=$PROJ docker compose -f $COMPOSE_F --env-file $ENVFILE up -d --no-deps $BACKEND
docker compose -f $COMPOSE_F --env-file $ENVFILE logs --tail=200 $BACKEND
echo "Health:"
curl -sS http://localhost:${BACKEND_PORT:-8080}/actuator/health || true