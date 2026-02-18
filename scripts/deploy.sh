#!/usr/bin/env bash
# scripts/deploy.sh
# Zweck: "deploy" (preferred) — wählt Quell‑Instanz (z. B. dev) und Ziel (test/prod)
# Standardverhalten: baut **nur** die Ziel‑Docker‑Container (keine lokalen mvn/ng builds),
# optional können lokale Builds mit --mvn / --frontend vor dem Image‑Build ausgeführt werden.
# Usage: ./scripts/deploy.sh [--from dev|test|prod] [--to test|prod] [--mvn] [--frontend] [--no-cache]
# Examples:
#   ./scripts/deploy.sh --from dev --to test
#   ./scripts/deploy.sh --from dev --to prod --no-cache
#   ./scripts/deploy.sh --from dev --to test --mvn --frontend

set -euo pipefail
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$ROOT_DIR"

# defaults
FROM_ENV="dev"
TO_ENV="test"
DO_MVN=false
DO_FE=false
NO_CACHE=""
RECREATE_DB=false
BRANCH=""
# safety: require explicit confirmation to deploy to production
CONFIRM_PROD=false

print_usage() {
  cat <<EOF
Usage: $0 [--from dev|test|prod] [--to test|prod] [--mvn] [--frontend] [--no-cache] [--recreate-db] [--confirm-prod] [--branch <name>]

Default: --from dev --to test
Default behavior: only target Docker containers (backend/frontend) are rebuilt and restarted.
The DB container is NOT recreated by default to preserve data; use --recreate-db to explicitly rebuild DB.
Note: the `backup` service is no longer included by default to avoid unintentionally starting DB dependencies.
**Safety:** deploying to `prod` requires the explicit `--confirm-prod` flag to avoid accidental production builds.
Use --mvn / --frontend to run local builds before building images.
--branch <name> : use the specified git branch as the build/deploy source (uses a temporary worktree)
EOF
}

# parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    --from)
      FROM_ENV="$2"; shift 2 ;;
    --to)
      TO_ENV="$2"; shift 2 ;;
    --branch)
      BRANCH="$2"; shift 2 ;;
    --mvn)
      DO_MVN=true; shift ;;
    --frontend)
      DO_FE=true; shift ;;
    --no-cache)
      NO_CACHE="--no-cache"; shift ;;
    --confirm-prod)
      CONFIRM_PROD=true; shift ;;
    --recreate-db)
      RECREATE_DB=true; shift ;;
    -h|--help)
      print_usage; exit 0 ;;
    dev|test|prod)
      # positional convenience: if single value provided, treat as target
      if [[ -z "$TO_ENV" || "$TO_ENV" == "test" ]]; then
        TO_ENV="$1"
      else
        FROM_ENV="$1"
      fi
      shift ;;
    *)
      echo "Unknown arg: $1"; print_usage; exit 1 ;;
  esac
done

# validation
case "$FROM_ENV" in
  dev|test|prod) ;;
  *) echo "Invalid --from: $FROM_ENV"; exit 1 ;;
esac
case "$TO_ENV" in
  test|prod) ;;
  *) echo "Invalid --to: $TO_ENV (must be 'test' or 'prod')"; exit 1 ;;
esac
if [[ "$FROM_ENV" == "$TO_ENV" ]]; then
  echo "Source and target must differ (from=$FROM_ENV, to=$TO_ENV)"; exit 1
fi

# Safety: require explicit confirmation before touching production
if [[ "$TO_ENV" == "prod" ]]; then
  if ! $CONFIRM_PROD; then
    echo "Refusing to deploy to 'prod' without explicit --confirm-prod flag. Use --confirm-prod to proceed." >&2
    exit 2
  fi
fi

echo "[deploy] from=$FROM_ENV  to=$TO_ENV  mvn=$DO_MVN  frontend=$DO_FE  no-cache=${NO_CACHE:-false}  recreate-db=${RECREATE_DB}"

# If branch is specified: create temporary worktree and operate from there
TMP_WORKDIR=""
cleanup_worktree() {
  if [[ -n "$TMP_WORKDIR" && -d "$TMP_WORKDIR" ]]; then
    echo "[deploy] Cleaning up temporary worktree: $TMP_WORKDIR"
    git worktree remove -f "$TMP_WORKDIR" 2>/dev/null || rm -rf "$TMP_WORKDIR"
  fi
}
if [[ -n "$BRANCH" ]]; then
  # verify branch exists locally
  if ! git show-ref --verify --quiet "refs/heads/$BRANCH"; then
    echo "Branch '$BRANCH' not found locally. Please fetch or create it locally."; exit 2
  fi
  TMP_WORKDIR=$(mktemp -d -t deploy-branch-XXXXXX 2>/dev/null || mktemp -d)
  echo "[deploy] Creating temporary worktree for branch '$BRANCH' at: $TMP_WORKDIR"
  git worktree add -f "$TMP_WORKDIR" "$BRANCH"
  trap cleanup_worktree EXIT INT TERM
  pushd "$TMP_WORKDIR" >/dev/null
fi

# Local builds (optional)
if $DO_MVN; then
  if [[ -f ./mvnw ]]; then
    ./mvnw -DskipTests clean package
  elif [[ -f ./mvnw.cmd ]]; then
    ./mvnw.cmd -DskipTests clean package
  else
    echo "mvnw not found; cannot run local mvn build"; exit 2
  fi
fi

if $DO_FE; then
  FE_DIR=""
  if [[ -d Physiokalender-v2-UI ]]; then
    FE_DIR="Physiokalender-v2-UI"
  elif [[ -d ../Physiokalender-v2-UI ]]; then
    FE_DIR="../Physiokalender-v2-UI"
  fi
  if [[ -n "$FE_DIR" ]]; then
    pushd "$FE_DIR" >/dev/null
    npm ci
    npm run build
    popd >/dev/null
  else
    echo "Frontend folder not found: Physiokalender-v2-UI or ../Physiokalender-v2-UI"; exit 3
  fi
fi

# Docker Compose — operate only on target services (do NOT touch DB by default)
COMPOSE_FILES="-f docker-compose.yml -f compose.${TO_ENV}.yml"
COMPOSE_PROJECT="physio-${TO_ENV}"
BACKEND_SERVICE="physio-${TO_ENV}-backend"
FRONTEND_SERVICE="physio-${TO_ENV}-frontend"
BACKUP_SERVICE="physio-${TO_ENV}-backup"
DB_SERVICE="physio-${TO_ENV}-db"

echo "[deploy] Building images for: $BACKEND_SERVICE $FRONTEND_SERVICE"
COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT docker compose $COMPOSE_FILES build $NO_CACHE $BACKEND_SERVICE $FRONTEND_SERVICE

# Up: restart only application services; include DB only if explicitly requested
# NOTE: `backup` is intentionally NOT included by default (prevents DB being started by dependency).
SERVICES_TO_UP="$BACKEND_SERVICE $FRONTEND_SERVICE"
if $RECREATE_DB; then
  SERVICES_TO_UP="$DB_SERVICE $SERVICES_TO_UP"
fi

echo "[deploy] Deploying target services: $SERVICES_TO_UP"

# Ensure no conflicting containers with the same names exist (stop + remove if present)
for svc in $SERVICES_TO_UP; do
  if docker ps -a --format '{{.Names}}' | grep -xq "$svc"; then
    echo "[deploy] Found existing container named $svc — stopping and removing to avoid name conflict"
    docker rm -f "$svc" >/dev/null 2>&1 || true
    echo "[deploy] removed $svc"
  fi
done

# Start services. Use --no-deps to avoid automatically starting dependent services (DB/backup)
# unless user explicitly requested DB recreation via --recreate-db.
if $RECREATE_DB; then
  COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT docker compose $COMPOSE_FILES up -d --remove-orphans --force-recreate $SERVICES_TO_UP
else
  COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT docker compose $COMPOSE_FILES up -d --no-deps --remove-orphans --force-recreate $SERVICES_TO_UP
fi

echo "[deploy] Done — status:"
COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT docker compose $COMPOSE_FILES ps

# if we used a temporary worktree, pop back to original repo dir before exit
if [[ -n "$TMP_WORKDIR" ]]; then
  popd >/dev/null || true
  cleanup_worktree
  trap - EXIT INT TERM
fi

exit 0