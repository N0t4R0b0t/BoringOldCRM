#!/usr/bin/env bash
# =============================================================================
# BOCRM — Proxmox helper: create the Docker registry LXC
# The registry is just a single static binary — LXC without Docker is fine here.
# Run on the Proxmox HOST.
# Usage:  bash scripts/proxmox-registry.sh [CT_ID]
# =============================================================================
set -euo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────
CT_ID=${1:-200}
CT_NAME="bocrm-registry"
CT_PASSWORD="$(openssl rand -hex 12)"
STORAGE="local-lvm"
TEMPLATE_STORAGE="local"
BRIDGE="vmbr0"
DISK_SIZE="15"   # GB — registry image storage
MEMORY="512"
CORES="1"

TEMPLATE_NAME="debian-12-standard_12.7-1_amd64.tar.zst"
TEMPLATE="${TEMPLATE_STORAGE}:vztmpl/${TEMPLATE_NAME}"
# ─────────────────────────────────────────────────────────────────────────────

CYAN='\033[0;36m'; GREEN='\033[0;32m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }

command -v pct &>/dev/null || { echo "ERROR: run on the Proxmox host."; exit 1; }
pct status "$CT_ID" &>/dev/null && { echo "ERROR: CT $CT_ID already exists."; exit 1; }

# Download template if missing
if ! pveam list "$TEMPLATE_STORAGE" | grep -q "$TEMPLATE_NAME"; then
  info "Downloading Debian 12 template…"
  pveam update && pveam download "$TEMPLATE_STORAGE" "$TEMPLATE_NAME"
fi

info "Creating LXC ${CT_ID} (${CT_NAME})…"
pct create "$CT_ID" "$TEMPLATE" \
  --hostname  "$CT_NAME" \
  --storage   "$STORAGE" \
  --rootfs    "${STORAGE}:${DISK_SIZE}" \
  --password  "$CT_PASSWORD" \
  --net0      "name=eth0,bridge=${BRIDGE},ip=dhcp,type=veth" \
  --memory    "$MEMORY" \
  --cores     "$CORES" \
  --onboot    1 \
  --unprivileged 1
  # No Docker, no nesting needed — registry runs as a native Go binary

pct start "$CT_ID"
info "Waiting for network…"; sleep 8

# Install the registry binary directly — no Docker needed
info "Installing registry…"
pct exec "$CT_ID" -- bash -c "
  set -e
  apt-get update -qq
  apt-get install -y -qq wget

  # Download the official registry binary (Go static binary — no deps)
  REGISTRY_VERSION=2.8.3
  wget -qO /usr/local/bin/registry \
    https://github.com/distribution/distribution/releases/download/v\${REGISTRY_VERSION}/registry_\${REGISTRY_VERSION}_linux_amd64.tar.gz \
    || true

  # Fall back to the Docker-packaged static binary if direct download fails
  if [[ ! -x /usr/local/bin/registry ]]; then
    apt-get install -y -qq docker.io
    docker run --rm --entrypoint cat registry:2 /bin/registry > /usr/local/bin/registry
    apt-get purge -y -qq docker.io
  fi

  chmod +x /usr/local/bin/registry
  mkdir -p /var/lib/registry /etc/distribution

  cat > /etc/distribution/config.yml << 'CFG'
version: 0.1
storage:
  filesystem:
    rootdirectory: /var/lib/registry
  delete:
    enabled: true
http:
  addr: :5000
CFG

  # Run as a systemd service
  cat > /etc/systemd/system/registry.service << 'SVC'
[Unit]
Description=Docker Distribution Registry
After=network.target

[Service]
ExecStart=/usr/local/bin/registry serve /etc/distribution/config.yml
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
SVC

  systemctl daemon-reload
  systemctl enable --now registry
"

CT_IP=$(pct exec "$CT_ID" -- hostname -I | awk '{print $1}')

echo
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  Registry LXC ready (native — no Docker)${NC}"
echo -e "  Container ID : ${CT_ID}"
echo -e "  Registry     : ${CYAN}${CT_IP}:5000${NC}"
echo -e "  Root password: ${CT_PASSWORD}"
echo
echo -e "  Push images from your dev machine:"
echo -e "  ${CYAN}bash scripts/push.sh ${CT_IP}:5000${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
