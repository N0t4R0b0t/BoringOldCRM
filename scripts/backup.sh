#!/usr/bin/env bash
# =============================================================================
# BOCRM — database backup script
# Creates a timestamped pg_dump and optionally prunes old backups.
# Usage:  bash scripts/backup.sh [backup-dir] [keep-days]
# =============================================================================
set -euo pipefail

COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env"
BACKUP_DIR="${1:-./backups}"
KEEP_DAYS="${2:-7}"

GREEN='\033[0;32m'; CYAN='\033[0;36m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

[[ -f "$COMPOSE_FILE" ]] || error "Run this script from the repository root."
[[ -f "$ENV_FILE"      ]] || error ".env not found."

# Read DB password from .env
DB_PASSWORD=$(grep '^DB_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)

mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTFILE="${BACKUP_DIR}/bocrm_${TIMESTAMP}.sql.gz"

info "Backing up database to ${OUTFILE}…"
docker compose -f "$COMPOSE_FILE" exec -T postgres \
  env PGPASSWORD="$DB_PASSWORD" \
  pg_dump -U crm crm \
  | gzip > "$OUTFILE"

success "Backup saved: $OUTFILE ($(du -sh "$OUTFILE" | cut -f1))"

# Prune old backups
if [[ "$KEEP_DAYS" -gt 0 ]]; then
  info "Pruning backups older than ${KEEP_DAYS} days…"
  find "$BACKUP_DIR" -name "bocrm_*.sql.gz" -mtime "+${KEEP_DAYS}" -delete
fi

success "Done."
