#!/usr/bin/env bash

################################################################################
# PHYSIOKALENDAR DEPLOYMENT SCRIPT
#
# Deployiert Backend (+ optional Frontend + optional MVN Build) zu test/prod
#
# Usage:
#   ./scripts/deploy.sh --from dev --to test
#   ./scripts/deploy.sh --from dev --to prod --confirm-prod
#   ./scripts/deploy.sh --from dev --to test --mvn
#   ./scripts/deploy.sh --from dev --to test --mvn --frontend
#   ./scripts/deploy.sh --from dev --to prod --no-cache --confirm-prod
#
# Optionen:
#   --from dev|test|prod      Source environment (default: dev)
#   --to test|prod            Target environment (erforderlich: test oder prod)
#   --mvn                     Lokalen Maven build vor Docker Image Build
#   --frontend                Frontend bauen (aus Physiokalender-v2-UI repo)
#   --no-cache                Docker build ohne Cache
#   --recreate-db             Datenbank auch neu bauen (normalerweise ignoriert)
#   --confirm-prod            Erforderlich für Deployment zu prod (Safety)
################################################################################

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$ROOT_DIR"

# Defaults
FROM_ENV="dev"
TO_ENV=""
DO_MVN=false
DO_FE=false
NO_CACHE=""
CONFIRM_PROD=false

print_usage() {
  cat <<EOF
Usage: $0 --to test|prod [--from dev|test|prod] [--mvn] [--frontend] [--no-cache] [--confirm-prod]

Erforderlich:
  --to test|prod              Ziel-Stage (erforderlich)

Optional:
  --from dev|test|prod        Quell-Environment (default: dev)
  --mvn                       Lokalen Maven build ausführen
  --frontend                  Frontend bauen (Physiokalender-v2-UI)
  --no-cache                  Docker build ohne Cache
  --confirm-prod              Bestätigung für prod (erforderlich bei --to prod)

Beispiele:
  ./scripts/deploy.sh --to test
  ./scripts/deploy.sh --to test --mvn --frontend
  ./scripts/deploy.sh --to prod --mvn --frontend --no-cache --confirm-prod
EOF
}

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --from)
      FROM_ENV="$2"; shift 2 ;;
    --to)
      TO_ENV="$2"; shift 2 ;;
    --mvn)
      DO_MVN=true; shift ;;
    --frontend)
      DO_FE=true; shift ;;
    --no-cache)
      NO_CACHE="--no-cache"; shift ;;
    --confirm-prod)
      CONFIRM_PROD=true; shift ;;
    --recreate-db)
      shift ;; # Ignore, kept for backwards compatibility
    -h|--help)
      print_usage; exit 0 ;;
    *)
      echo "Unbekannter Parameter: $1"; print_usage; exit 1 ;;
  esac
done

# ============================================================================
# VALIDIERUNG
# ============================================================================

if [ -z "$TO_ENV" ]; then
  echo "❌ ERROR: --to ist erforderlich (test oder prod)"
  echo ""
  print_usage
  exit 1
fi

case "$FROM_ENV" in
  dev|test|prod) ;;
  *) echo "❌ ERROR: Ungültiges --from: $FROM_ENV"; exit 1 ;;
esac

case "$TO_ENV" in
  test|prod) ;;
  *) echo "❌ ERROR: Ungültiges --to: $TO_ENV (muss 'test' oder 'prod' sein)"; exit 1 ;;
esac

# Safety: Production erfordert explizite Bestätigung
if [[ "$TO_ENV" == "prod" ]] && ! $CONFIRM_PROD; then
  echo "❌ ERROR: Deployment zu 'prod' erfordert --confirm-prod Flag!"
  exit 2
fi

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║              DEPLOYMENT SCRIPT                                 ║"
echo "║                                                                ║"
echo "║  Von:           $FROM_ENV"
echo "║  Nach:          $TO_ENV"
echo "║  Maven Build:   $DO_MVN"
echo "║  Frontend:      $DO_FE"
echo "║  No Cache:      ${NO_CACHE:-false}"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# ============================================================================
# OPTIONAL: Lokale Maven Build
# ============================================================================

if $DO_MVN; then
  echo "🔨 Starte lokalen Maven Build..."
  echo ""

  if [[ -f ./mvnw.cmd ]]; then
    ./mvnw.cmd -DskipTests clean package
  elif [[ -f ./mvnw ]]; then
    ./mvnw -DskipTests clean package
  else
    echo "❌ ERROR: mvnw nicht gefunden"
    exit 2
  fi

  echo ""
  echo "✅ Maven Build abgeschlossen"
  echo ""
fi

# ============================================================================
# OPTIONAL: Frontend Build (aus Physiokalender-v2-UI repo)
# ============================================================================

if $DO_FE; then
  echo "🎨 Frontend Flag erkannt..."
  echo ""
  echo "⚠️  Frontend wird über Docker Container gebaut (keine lokalen npm commands)"
  echo ""
  echo "💡 Wenn du npm install / npm run build manuell brauchst, führe aus:"
  echo "   cd Physiokalender-v2-UI"
  echo "   npm install"
  echo "   npm run build"
  echo ""
fi

# ============================================================================
# DOCKER COMPOSE UP - Haupt-Deployment
# ============================================================================

ENV_FILE=".env.${TO_ENV}"
COMPOSE_FILE="compose.${TO_ENV}.yml"

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ ERROR: Environment-Datei nicht gefunden: $ENV_FILE"
  exit 1
fi

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "❌ ERROR: Compose-Datei nicht gefunden: $COMPOSE_FILE"
  exit 1
fi

echo "🚀 Starte Docker Compose für $TO_ENV..."
echo ""
echo "   Befehl: docker compose -f $COMPOSE_FILE --env-file $ENV_FILE up -d --build $NO_CACHE"
echo ""

docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build $NO_CACHE

# Frontend Docker Compose (wenn --frontend Flag gesetzt)
if $DO_FE; then
  echo ""
  echo "🚀 Starte auch Frontend Docker Compose für $TO_ENV..."
  echo ""

  FE_DIR="Physiokalender-v2-UI"
  FE_COMPOSE_FILE="$FE_DIR/compose.${TO_ENV}.yml"
  FE_ENV_FILE="$ENV_FILE"  # Environment File ist im Root-Verzeichnis

  if [ ! -d "$FE_DIR" ]; then
    echo "❌ ERROR: Frontend Verzeichnis nicht gefunden: $FE_DIR"
    exit 3
  fi

  if [ ! -f "$FE_COMPOSE_FILE" ]; then
    echo "❌ ERROR: Frontend Compose-Datei nicht gefunden: $FE_COMPOSE_FILE"
    exit 3
  fi

  echo "   Befehl: docker compose -f $FE_COMPOSE_FILE --env-file $FE_ENV_FILE up -d --build $NO_CACHE"
  echo ""

  # Frontend compose hat ein externes Netzwerk, daher muss Backend vorher starten
  docker compose -f "$FE_COMPOSE_FILE" --env-file "$FE_ENV_FILE" up -d --build $NO_CACHE
fi

echo ""
echo "✅ Deployment zu '$TO_ENV' abgeschlossen!"
echo ""
echo "📊 Service Status:"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
echo ""

exit 0
