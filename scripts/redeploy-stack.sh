#!/usr/bin/env bash
# scripts/redeploy-stack.sh
# Usage: ./scripts/redeploy-stack.sh [test|prod] [--mvn] [--frontend] [--no-cache]
# Example: ./scripts/redeploy-stack.sh test --mvn --frontend --no-cache

set -euo pipefail
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$ROOT_DIR"

ENV="test"
DO_MVN=false
DO_FE=false
NO_CACHE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    test|prod) ENV="$1"; shift ;;
    --mvn) DO_MVN=true; shift ;;
    --frontend) DO_FE=true; shift ;;
    --no-cache) NO_CACHE="--no-cache"; shift ;;
    -h|--help) echo "Usage: $0 [test|prod] [--mvn] [--frontend] [--no-cache]"; exit 0 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

COMPOSE_FILES="-f docker-compose.yml -f compose.${ENV}.yml"
COMPOSE_PROJECT="physio-${ENV}"

# detect mvnw
if [[ -f ./mvnw ]]; then
  MVNW=./mvnw
elif [[ -f ./mvnw.cmd ]]; then
  MVNW=./mvnw.cmd
else
  MVNW=""
fi

echo "[redeploy] env=$ENV  mvn=$DO_MVN  frontend=$DO_FE  no-cache=${NO_CACHE:-false}"

if $DO_MVN; then
  if [[ -z "$MVNW" ]]; then
    echo "mvnw not found in repo root"; exit 2
  fi
  echo "[redeploy] Running Maven package (skip tests)..."
  "$MVNW" -DskipTests clean package
fi

if $DO_FE; then
  if [[ -d Physiokalender-v2-UI ]]; then
    echo "[redeploy] Building frontend..."
    pushd Physiokalender-v2-UI >/dev/null
    npm ci
    npm run build
    popd >/dev/null
  else
    echo "[redeploy] Frontend folder not found: Physiokalender-v2-UI"; exit 3
  fi
fi

echo "[redeploy] Building Docker images (compose)..."
COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT docker compose $COMPOSE_FILES build $NO_CACHE --parallel

echo "[redeploy] Deploying stack (force recreate)..."
COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT docker compose $COMPOSE_FILES up -d --remove-orphans --force-recreate

echo "[redeploy] Done — check status:"
COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT docker compose $COMPOSE_FILES ps

echo "Tip: tail logs with: COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT docker compose $COMPOSE_FILES logs -f --tail=200 physio-${ENV}-backend"