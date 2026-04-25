#!/usr/bin/env bash
# =============================================================================
# BOCRM — deploy a release package to a native LXC container
# Run on the Proxmox HOST.
#
# Prerequisites:
#   1. LXC provisioned:  bash scripts/proxmox-app.sh [CT_ID]
#   2. App built:        bash scripts/build.sh [version]
#
# Usage:
#   CT_ID=201 PACKAGE=bocrm-20240101-120000.tar.gz bash scripts/deploy.sh
#   CT_ID=201 bash scripts/deploy.sh   # uses latest package in dist/
# =============================================================================
set -euo pipefail

CT_ID="${CT_ID:-201}"

CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# ── Resolve package ───────────────────────────────────────────────────────────
if [[ -n "${PACKAGE:-}" ]]; then
  PKG_PATH="$PACKAGE"
else
  PKG_PATH=$(ls -t "$REPO_ROOT/dist"/bocrm-*.tar.{xz,gz} 2>/dev/null | head -1)
  [[ -n "$PKG_PATH" ]] || error "No package found in dist/ — run: bash scripts/build.sh"
fi

[[ -f "$PKG_PATH" ]] || error "Package not found: $PKG_PATH"
PKG_NAME="$(basename "$PKG_PATH")"

# ── Sanity checks ─────────────────────────────────────────────────────────────
command -v pct &>/dev/null || error "pct not found — run this on the Proxmox host."
pct status "$CT_ID" | grep -q running \
  || error "CT $CT_ID is not running. Start it with: pct start $CT_ID"

info "Deploying ${PKG_NAME} → CT ${CT_ID}…"

# ── Stop service ──────────────────────────────────────────────────────────────
info "Stopping bocrm service…"
pct exec "$CT_ID" -- bash -c "systemctl stop bocrm 2>/dev/null || true"

# ── Push and extract package ──────────────────────────────────────────────────
# Detect archive format from the file extension
if [[ "$PKG_PATH" == *.tar.xz ]]; then
  ARCHIVE_EXT="tar.xz"
  TAR_FLAGS="-xJf"
else
  ARCHIVE_EXT="tar.gz"
  TAR_FLAGS="-xzf"
fi
REMOTE_ARCHIVE="/tmp/bocrm-release.${ARCHIVE_EXT}"

info "Uploading package ($(du -sh "$PKG_PATH" | cut -f1))…"
pct push "$CT_ID" "$PKG_PATH" "$REMOTE_ARCHIVE"

info "Extracting…"
pct exec "$CT_ID" -- bash -c "
  set -e
  # Install new JAR
  tar ${TAR_FLAGS} '${REMOTE_ARCHIVE}' -C /tmp --no-same-owner ./bocrm.jar
  mv /tmp/bocrm.jar /opt/bocrm/bocrm.jar
  chown bocrm:bocrm /opt/bocrm/bocrm.jar

  # Install new frontend
  rm -rf /opt/bocrm/frontend/*
  tar ${TAR_FLAGS} '${REMOTE_ARCHIVE}' -C /opt/bocrm/frontend --no-same-owner --strip-components=2 ./frontend
  chown -R bocrm:bocrm /opt/bocrm/frontend

  rm -f '${REMOTE_ARCHIVE}'
"
success "Package extracted"

# ── Restart service ───────────────────────────────────────────────────────────
info "Starting bocrm service…"
pct exec "$CT_ID" -- systemctl start bocrm

info "Waiting for service to start…"
for i in $(seq 1 12); do
  sleep 5
  STATUS=$(pct exec "$CT_ID" -- systemctl is-active bocrm 2>/dev/null || true)
  if [[ "$STATUS" == "active" ]]; then break; fi
  if [[ "$STATUS" == "failed" ]]; then
    error "bocrm service failed. Logs: pct exec $CT_ID -- journalctl -u bocrm -n 50"
  fi
  echo -n "."
done
echo

STATUS=$(pct exec "$CT_ID" -- systemctl is-active bocrm 2>/dev/null || true)
[[ "$STATUS" == "active" ]] \
  || warn "Service may still be starting — check: pct exec $CT_ID -- systemctl status bocrm"

CT_IP=$(pct exec "$CT_ID" -- hostname -I | awk '{print $1}')

echo
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  Deploy complete!${NC}"
echo -e "  Package : ${PKG_NAME}"
echo -e "  App URL : ${CYAN}http://${CT_IP}${NC}"
echo
echo -e "  Logs  : ${CYAN}pct exec ${CT_ID} -- journalctl -u bocrm -f${NC}"
echo -e "  Status: ${CYAN}pct exec ${CT_ID} -- systemctl status bocrm${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
