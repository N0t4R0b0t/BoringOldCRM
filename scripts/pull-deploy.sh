#!/usr/bin/env bash
# =============================================================================
# BOCRM — auto-deploy from GitHub releases
# Run on the Proxmox HOST. Polls GitHub for the latest release and deploys if
# a newer version is available. Designed to run from cron on a schedule.
#
# Prerequisites:
#   1. LXC provisioned:  bash scripts/proxmox-app.sh [CT_ID]
#   2. curl, jq installed on Proxmox host
#   3. Proxmox host has SSH key-based access to its own LXC (pct works)
#
# Usage:
#   bash scripts/pull-deploy.sh                    # check snapshot release
#   CT_ID=202 bash scripts/pull-deploy.sh          # deploy to specific CT
#   bash scripts/pull-deploy.sh --force            # force redeploy even if up-to-date
#   bash scripts/pull-deploy.sh --release tag:v1.0 # deploy specific release
#
# Cron (runs every 15 minutes on Proxmox host):
#   */15 * * * * root CT_ID=201 bash /root/scripts/pull-deploy.sh >> /var/log/bocrm-pull-deploy.log 2>&1
#
# Environment variables:
#   GITHUB_REPO           — default: rsalvador/BoringOldCRM
#   RELEASE_TAG           — default: snapshot (or set with --release flag)
#   CT_ID                 — default: 201 (LXC container ID)
#   GITHUB_TOKEN          — optional, for private repos (read:packages scope)
#   BOCRM_VERSION_FILE    — default: /opt/bocrm/.deployed-version (tracks deployed release ID)
# =============================================================================
set -euo pipefail

GITHUB_REPO="${GITHUB_REPO:-rsalvador/BoringOldCRM}"
RELEASE_TAG="${RELEASE_TAG:-snapshot}"
CT_ID="${CT_ID:-201}"
VERSION_FILE="${BOCRM_VERSION_FILE:-/opt/bocrm/.deployed-version}"
FORCE="${FORCE:-0}"

CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

# ── Parse arguments ───────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --force)
      FORCE=1
      shift
      ;;
    --release)
      RELEASE_TAG="$2"
      shift 2
      ;;
    *)
      error "Unknown option: $1"
      ;;
  esac
done

# ── Sanity checks ─────────────────────────────────────────────────────────────
command -v curl &>/dev/null || error "curl not found"
command -v jq &>/dev/null || error "jq not found"
command -v pct &>/dev/null || error "pct not found — run this on the Proxmox host"
pct status "$CT_ID" | grep -q running \
  || error "CT $CT_ID is not running. Start it with: pct start $CT_ID"

info "Checking GitHub for new release…"
info "  Repo: $GITHUB_REPO"
info "  Tag:  $RELEASE_TAG"
info "  CT:   $CT_ID"

# ── Fetch release from GitHub API ─────────────────────────────────────────────
CURL_OPTS=("-sf")
if [[ -n "${GITHUB_TOKEN:-}" ]]; then
  CURL_OPTS+=("-H" "Authorization: Bearer $GITHUB_TOKEN")
fi

RELEASE_JSON=$(curl "${CURL_OPTS[@]}" "https://api.github.com/repos/$GITHUB_REPO/releases/tags/$RELEASE_TAG" 2>/dev/null || echo "")

if [[ -z "$RELEASE_JSON" ]]; then
  error "Failed to fetch release from GitHub. Check GITHUB_REPO, RELEASE_TAG, and network."
fi

# Extract metadata from release
RELEASE_ID=$(echo "$RELEASE_JSON" | jq -r '.id // empty')
RELEASE_NAME=$(echo "$RELEASE_JSON" | jq -r '.name // empty')
ARTIFACT_URL=$(echo "$RELEASE_JSON" | jq -r '.assets[0].browser_download_url // empty')

if [[ -z "$RELEASE_ID" ]] || [[ -z "$ARTIFACT_URL" ]]; then
  error "Release found but missing ID or artifact. Response: $RELEASE_JSON"
fi

# Detect archive format from the URL
if [[ "$ARTIFACT_URL" == *.tar.xz ]]; then
  ARCHIVE_EXT="tar.xz"
  TAR_FLAGS="-xJf"
else
  ARCHIVE_EXT="tar.gz"
  TAR_FLAGS="-xzf"
fi

info "Found release: $RELEASE_NAME (ID: $RELEASE_ID)"

# ── Check if already deployed ─────────────────────────────────────────────────
CURRENT_ID=$(cat "$VERSION_FILE" 2>/dev/null || echo "")

if [[ "$CURRENT_ID" == "$RELEASE_ID" && "$FORCE" != "1" ]]; then
  info "Already deployed (ID: $CURRENT_ID). No action needed."
  exit 0
fi

if [[ "$FORCE" == "1" ]] && [[ "$CURRENT_ID" == "$RELEASE_ID" ]]; then
  warn "Forcing redeploy of current release (ID: $RELEASE_ID)"
fi

# ── Download tarball to temp location ─────────────────────────────────────────
TMPFILE=$(mktemp "/tmp/bocrm-pull-deploy-XXXXXX.${ARCHIVE_EXT}")
trap "rm -f '$TMPFILE'" EXIT

info "Downloading artifact ($(echo "$ARTIFACT_URL" | sed 's|.*/||'))…"
if ! curl -sL "$ARTIFACT_URL" -o "$TMPFILE"; then
  error "Failed to download artifact from $ARTIFACT_URL"
fi

PKG_SIZE=$(du -sh "$TMPFILE" | cut -f1)
info "Downloaded: $PKG_SIZE"

# ── Stop service ──────────────────────────────────────────────────────────────
info "Stopping bocrm service in CT $CT_ID…"
if ! pct exec "$CT_ID" -- bash -c "systemctl stop bocrm 2>/dev/null || true"; then
  error "Failed to stop bocrm service"
fi

sleep 2  # grace period for clean shutdown

# ── Push and extract package ──────────────────────────────────────────────────
REMOTE_ARCHIVE="/tmp/bocrm-release.${ARCHIVE_EXT}"

info "Uploading package…"
if ! pct push "$CT_ID" "$TMPFILE" "$REMOTE_ARCHIVE"; then
  error "Failed to push package to CT"
fi

info "Extracting package…"
if ! pct exec "$CT_ID" -- bash -c "
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
"; then
  error "Failed to extract package in CT"
fi

success "Package extracted"

# ── Restart service ───────────────────────────────────────────────────────────
info "Starting bocrm service…"
if ! pct exec "$CT_ID" -- systemctl start bocrm; then
  error "Failed to start bocrm service"
fi

info "Waiting for service to become active…"
for i in $(seq 1 12); do
  sleep 5
  STATUS=$(pct exec "$CT_ID" -- systemctl is-active bocrm 2>/dev/null || echo "unknown")
  if [[ "$STATUS" == "active" ]]; then
    break
  fi
  if [[ "$STATUS" == "failed" ]]; then
    error "bocrm service failed to start. Run: pct exec $CT_ID -- journalctl -u bocrm -n 50"
  fi
  printf "."
done
echo

STATUS=$(pct exec "$CT_ID" -- systemctl is-active bocrm 2>/dev/null || echo "unknown")
if [[ "$STATUS" != "active" ]]; then
  warn "Service status is '$STATUS' (expected 'active'). Check: pct exec $CT_ID -- systemctl status bocrm"
fi

# ── Save deployed version ─────────────────────────────────────────────────────
if mkdir -p "$(dirname "$VERSION_FILE")" && echo "$RELEASE_ID" > "$VERSION_FILE"; then
  success "Deployment complete"
  info "Deployed release: $RELEASE_NAME (ID: $RELEASE_ID)"
else
  warn "Failed to save version file at $VERSION_FILE"
fi

CT_IP=$(pct exec "$CT_ID" -- hostname -I | awk '{print $1}' 2>/dev/null || echo "unknown")

echo
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  Pull-deploy complete!${NC}"
echo -e "  Release  : $RELEASE_NAME"
echo -e "  App URL  : ${CYAN}http://${CT_IP}${NC}"
echo
echo -e "  Logs  : ${CYAN}pct exec ${CT_ID} -- journalctl -u bocrm -f${NC}"
echo -e "  Status: ${CYAN}pct exec ${CT_ID} -- systemctl status bocrm${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
