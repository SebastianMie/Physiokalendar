#!/usr/bin/env bash
# scripts/redeploy-stack.sh
# Usage: ./scripts/redeploy-stack.sh [test|prod] [--mvn] [--frontend] [--no-cache] [--recreate-db]
# Example: ./scripts/redeploy-stack.sh test --mvn --frontend --no-cache
# To force DB recreation (dangerous - will recreate container): add --recreate-db

set -euo pipefail
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$ROOT_DIR"

ENV="test"
DO_MVN=false
DO_FE=false
NO_CACHE=""
RECREATE_DB=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    test|prod) ENV="$1"; shift ;;
    --mvn) DO_MVN=true; shift ;;
    --frontend) DO_FE=true; shift ;;
    --no-cache) NO_CACHE="--no-cache"; shift ;;
    --recreate-db) RECREATE_DB=true; shift ;;
    -h|--help) echo "Usage: $0 [test|prod] [--mvn] [--frontend] [--no-cache] [--recreate-db]"; exit 0 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

COMPOSE_FILES="-f compose.${ENV}.yml"
COMPOSE_PROJECT="physio-${ENV}"

# detect mvnw
if [[ -f ./mvnw ]]; then
  MVNW=./mvnw
elif [[ -f ./mvnw.cmd ]]; then
  MVNW=./mvnw.cmd
else
  MVNW=""
fi

echo "[redeploy] env=$ENV  mvn=$DO_MVN  frontend=$DO_FE  no-cache=${NO_CACHE:-false}  recreate-db=${RECREATE_DB}"

if $DO_MVN; then
  if [[ -z "$MVNW" ]]; then
    echo "mvnw not found in repo root"; exit 2
  fi
  echo "[redeploy] Running Maven package (skip tests)..."
  "$MVNW" -DskipTests clean package
  echo "[redeploy] Maven build finished. Listing target/*.jar:"
  ls -l target/*.jar || true
  echo "[redeploy] Displaying JAR file details (size + timestamp):"
  for f in target/*.jar; do echo " - $f -> $(stat -c '%y %s' "$f" 2>/dev/null || date -r "$f" '+%F %T')"; done || true
fi

if $DO_FE; then
  FE_DIR=""
  if [[ -d Physiokalender-v2-UI ]]; then
    FE_DIR="Physiokalender-v2-UI"
  elif [[ -d ../Physiokalender-v2-UI ]]; then
    FE_DIR="../Physiokalender-v2-UI"
  fi
  if [[ -n "$FE_DIR" ]]; then
    echo "[redeploy] Building frontend from: $FE_DIR"
    pushd "$FE_DIR" >/dev/null
    npm ci
    npm run build
    echo "[redeploy] Frontend build finished. Listing dist/ directory:"
    ls -la dist/ || true
    echo "[redeploy] Dist summary (top files):"
    ls -la dist/physiokalender-v2-ui | head -n 40 || true
    popd >/dev/null
  else
    echo "[redeploy] Frontend folder not found: Physiokalender-v2-UI or ../Physiokalender-v2-UI"; exit 3
  fi
fi

echo "[redeploy] Building Docker images (compose)..."
COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT docker compose $COMPOSE_FILES build $NO_CACHE --parallel

# show the images that were just built (helpful for CI logs)
echo "[redeploy] Docker images after build (recent matches):"
docker images --format '{{.Repository}}:{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}' | egrep 'physio|physiokalender' || docker images --format '{{.Repository}}:{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}'

echo "[redeploy] Deploying stack (force recreate — DB excluded by default)..."
# Recreate only application containers by default (do NOT touch DB unless explicitly requested)
SERVICES="physio-${ENV}-backend physio-${ENV}-frontend physio-${ENV}-backup"
if $RECREATE_DB; then
  SERVICES="physio-${ENV}-db $SERVICES"
fi
COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT docker compose $COMPOSE_FILES up -d --remove-orphans --force-recreate $SERVICES

echo "[redeploy] Done — check status:"
COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT docker compose $COMPOSE_FILES ps

echo "Tip: tail logs with: COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT docker compose $COMPOSE_FILES logs -f --tail=200 physio-${ENV}-backend"