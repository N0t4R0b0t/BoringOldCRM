#!/usr/bin/env bash
# =============================================================================
# BOCRM — build images and push to local registry
# Run this from your dev machine after making changes.
# Usage:  bash scripts/push.sh <registry-host> [version-tag]
# Example: bash scripts/push.sh 192.168.1.10:5000
#          bash scripts/push.sh 192.168.1.10:5000 v1.2
# =============================================================================
set -euo pipefail

REGISTRY="${1:?Usage: $0 <registry-host:port> [version-tag]}"
VERSION="${2:-latest}"

CYAN='\033[0;36m'; GREEN='\033[0;32m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }

BACKEND_IMAGE="${REGISTRY}/bocrm-backend:${VERSION}"
FRONTEND_IMAGE="${REGISTRY}/bocrm-frontend:${VERSION}"

# ── Build ─────────────────────────────────────────────────────────────────────
info "Building backend (${BACKEND_IMAGE})…"
docker build --platform linux/amd64 -t "$BACKEND_IMAGE" ./backend

info "Building frontend (${FRONTEND_IMAGE})…"
docker build --platform linux/amd64 -t "$FRONTEND_IMAGE" ./frontend

# Also tag as :latest when pushing a versioned tag so the server can use either
if [[ "$VERSION" != "latest" ]]; then
  docker tag "$BACKEND_IMAGE"  "${REGISTRY}/bocrm-backend:latest"
  docker tag "$FRONTEND_IMAGE" "${REGISTRY}/bocrm-frontend:latest"
fi

# ── Push ──────────────────────────────────────────────────────────────────────
info "Pushing to registry…"
docker push "$BACKEND_IMAGE"
docker push "$FRONTEND_IMAGE"
[[ "$VERSION" != "latest" ]] && {
  docker push "${REGISTRY}/bocrm-backend:latest"
  docker push "${REGISTRY}/bocrm-frontend:latest"
}

echo
success "Images pushed:"
echo "  ${BACKEND_IMAGE}"
echo "  ${FRONTEND_IMAGE}"
echo
echo "Deploy on the server:"
echo "  VERSION=${VERSION} bash scripts/deploy.sh"
