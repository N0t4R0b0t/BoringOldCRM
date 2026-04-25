#!/usr/bin/env bash
# =============================================================================
# BOCRM — update script
# Pulls latest code, rebuilds changed images, and restarts with minimal downtime.
# Usage:  bash scripts/update.sh
# =============================================================================
set -euo pipefail

COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env"

CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

[[ -f "$COMPOSE_FILE" ]] || error "Run this script from the repository root."
[[ -f "$ENV_FILE"      ]] || error ".env not found — run scripts/install.sh first."

info "Pulling latest code…"
git pull --ff-only

info "Building updated images…"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build --parallel

info "Restarting services…"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

info "Removing unused images…"
docker image prune -f --filter "until=24h" >/dev/null

success "Update complete. Logs: docker compose -f $COMPOSE_FILE logs -f"
