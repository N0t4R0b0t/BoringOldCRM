#!/usr/bin/env bash
# =============================================================================
# BOCRM — Proxmox helper: create and provision the application LXC
# Runs everything natively — no Docker.
# Run on the Proxmox HOST.
# Usage:  bash scripts/proxmox-app.sh [CT_ID]
# =============================================================================
set -euo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────
CT_ID=${1:-201}
CT_NAME="bocrm-app"
CT_PASSWORD="$(openssl rand -hex 12)"
TEMPLATE_STORAGE="local"
BRIDGE="vmbr0"
DISK_SIZE="20"   # GB
MEMORY="2048"    # MB
CORES="2"

# Auto-detect a suitable storage for container rootfs (prefer local-lvm, then zfs pool, then local)
if [ -z "${STORAGE:-}" ]; then
  STORAGE=$(pvesm status --content rootdir 2>/dev/null | awk 'NR>1 && $2=="active" {print $1}' | grep -E 'local-lvm|zfs|pve' | head -1 || true)
  if [ -z "$STORAGE" ]; then
    STORAGE=$(pvesm status --content rootdir 2>/dev/null | awk 'NR>1 && $2=="active" {print $1}' | head -1 || true)
  fi
fi
if [ -z "$STORAGE" ]; then
  echo "ERROR: Could not auto-detect a storage that supports container rootfs."
  echo "       Run: pvesm status --content rootdir"
  echo "       Then re-run with: STORAGE=<name> bash scripts/proxmox-app.sh"
  exit 1
fi
echo -e "\033[0;36m[INFO]\033[0m  Using storage: ${STORAGE}"

# Ensure template list is current before searching
pveam update -q 2>/dev/null || pveam update
TEMPLATE_NAME="${TEMPLATE_NAME:-$(pveam available --section system | awk '/debian-12-standard/{print $2}' | sort -V | tail -1)}"
if [ -z "$TEMPLATE_NAME" ]; then
  echo "ERROR: Could not find a debian-12-standard template in pveam available."
  echo "       Set TEMPLATE_NAME manually, e.g.:"
  echo "       TEMPLATE_NAME=debian-12-standard_12.7-1_amd64.tar.zst bash scripts/proxmox-app.sh"
  exit 1
fi
TEMPLATE="${TEMPLATE_STORAGE}:vztmpl/${TEMPLATE_NAME}"
# ─────────────────────────────────────────────────────────────────────────────

CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }

command -v pct &>/dev/null || { echo "ERROR: run on the Proxmox host."; exit 1; }
pct status "$CT_ID" &>/dev/null && { echo "ERROR: CT $CT_ID already exists."; exit 1; }

if ! pveam list "$TEMPLATE_STORAGE" | grep -q "$TEMPLATE_NAME"; then
  info "Downloading Debian 12 template…"
  pveam download "$TEMPLATE_STORAGE" "$TEMPLATE_NAME"
fi

# ── Collect config before we start ───────────────────────────────────────────
echo
echo -e "${CYAN}━━━ Configuration ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo "Secrets are auto-generated if you press Enter."
echo

gen()     { openssl rand -hex 32; }
ask()     { local v; read -rp "$(echo -e "${YELLOW}$1${NC} [$2]: ")" v; echo "${v:-$2}"; }
ask_sec() { local v; read -rsp "$(echo -e "${YELLOW}$1${NC} (Enter to auto-generate): ")" v; echo >&2; echo "${v:-$(gen)}"; }
ask_opt() { local v; read -rp "$(echo -e "${YELLOW}$1${NC} (optional, Enter to skip): ")" v; echo "${v:-}"; }

JWT=$(ask_sec     "JWT signing secret")
DB_PASS=$(ask_sec "Database password")
RMQ_PASS=$(ask_sec "RabbitMQ password")
REDIS_PASS=$(ask_sec "Redis password")
echo
echo -e "${CYAN}AI provider keys (at least one for the assistant):${NC}"
ANTHROPIC=$(ask_opt "Anthropic API key")
OPENAI=$(ask_opt    "OpenAI API key")
GEMINI=$(ask_opt    "Gemini API key")
TAVILY=$(ask_opt    "Tavily search key")
echo
echo -e "${CYAN}Auth0 / OIDC SSO (optional — Enter to skip):${NC}"
AUTH0_ISSUER=$(ask_opt   "Auth0 issuer URI (e.g. https://your-tenant.us.auth0.com/)")
AUTH0_CLIENT_ID=$(ask_opt "Auth0 client ID")
AUTH0_CLIENT_SECRET=$(ask_opt "Auth0 client secret")
AUTH0_AUDIENCE=$(ask_opt  "Auth0 audience (usually same as client ID)")
HTTP_PORT=$(ask "HTTP port" "80")

echo
echo -e "${CYAN}CORS / Frontend origins (comma-separated)${NC}"
# Example: http://localhost:5173,https://your-domain.com
CORS_ALLOWED_ORIGINS_RAW=$(ask "APP_CORS_ALLOWED_ORIGINS" "http://localhost:5173,https://your-domain.com")

# ── Create container ──────────────────────────────────────────────────────────
info "Creating LXC ${CT_ID} (${CT_NAME})…"
pct create "$CT_ID" "$TEMPLATE" \
  --hostname     "$CT_NAME" \
  --storage      "$STORAGE" \
  --rootfs       "${STORAGE}:${DISK_SIZE}" \
  --password     "$CT_PASSWORD" \
  --net0         "name=eth0,bridge=${BRIDGE},ip=dhcp,type=veth" \
  --memory       "$MEMORY" \
  --cores        "$CORES" \
  --onboot       1 \
  --unprivileged 1

pct start "$CT_ID"
info "Waiting for network…"; sleep 8

# Ensure DNS works inside the container (DHCP may not populate resolv.conf)
pct exec "$CT_ID" -- bash -c "
  if ! grep -q nameserver /etc/resolv.conf 2>/dev/null; then
    echo 'nameserver 1.1.1.1' >  /etc/resolv.conf
    echo 'nameserver 8.8.8.8' >> /etc/resolv.conf
  fi
"
# Resolve container IP now so we can use it in config.env
CT_IP=$(pct exec "$CT_ID" -- hostname -I | awk '{print $1}')
info "Container IP: ${CT_IP}"

# Wait until we can actually reach the internet
info "Verifying connectivity…"
for i in $(seq 1 12); do
  pct exec "$CT_ID" -- ping -c1 -W2 deb.debian.org &>/dev/null && break
  [ "$i" -eq 12 ] && { echo "ERROR: container has no internet after 60 s. Check bridge/NAT."; exit 1; }
  sleep 5
done

# ── Install packages ──────────────────────────────────────────────────────────
info "Installing packages…"
pct exec "$CT_ID" -- bash -c "
  set -e
  apt-get update -qq
  apt-get install -y -qq locales
  echo 'en_US.UTF-8 UTF-8' > /etc/locale.gen && locale-gen
  apt-get install -y -qq wget apt-transport-https gnupg
  # Adoptium (Eclipse Temurin) repo for Java 21
  wget -qO /etc/apt/keyrings/adoptium.asc https://packages.adoptium.net/artifactory/api/gpg/key/public
  echo 'deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb bookworm main' \
    > /etc/apt/sources.list.d/adoptium.list
  apt-get update -qq
  apt-get install -y -qq temurin-21-jre
  apt-get install -y -qq \
    postgresql \
    rabbitmq-server \
    redis-server \
    nginx \
    openssh-server \
    curl
"

# ── PostgreSQL ────────────────────────────────────────────────────────────────
info "Configuring PostgreSQL…"
pct exec "$CT_ID" -- bash -c "
  set -e
  systemctl enable --now postgresql
  su - postgres -c \"psql -c \\\"CREATE USER crm WITH PASSWORD '${DB_PASS}';\\\"\"
  su - postgres -c \"psql -c \\\"CREATE DATABASE crm OWNER crm;\\\"\"
"

# ── RabbitMQ ──────────────────────────────────────────────────────────────────
info "Configuring RabbitMQ…"
pct exec "$CT_ID" -- bash -c "
  set -e
  systemctl enable --now rabbitmq-server
  rabbitmqctl add_user crm '${RMQ_PASS}'
  rabbitmqctl set_user_tags crm administrator
  rabbitmqctl set_permissions -p / crm '.*' '.*' '.*'
  rabbitmqctl delete_user guest 2>/dev/null || true
"

# ── Redis ─────────────────────────────────────────────────────────────────────
info "Configuring Redis…"
pct exec "$CT_ID" -- bash -c "
  # Write password — append ensures it's set even if sed patterns don't match
  sed -i '/^#* *requirepass/d' /etc/redis/redis.conf
  echo 'requirepass ${REDIS_PASS}' >> /etc/redis/redis.conf
  # LXC (unprivileged) cannot use mount namespacing — replace the unit entirely
  # rather than using a drop-in (base unit directives still trigger namespace setup)
  cat > /etc/systemd/system/redis-server.service <<'UNIT'
[Unit]
Description=Advanced key-value store
After=network.target
Documentation=http://redis.io/documentation

[Service]
Type=notify
User=redis
Group=redis
ExecStart=/usr/bin/redis-server /etc/redis/redis.conf --supervised systemd --daemonize no
ExecStop=/bin/kill -s TERM \$MAINPID
Restart=on-failure
RestartSec=5
LimitNOFILE=65535
RuntimeDirectory=redis
RuntimeDirectoryMode=0755

[Install]
WantedBy=multi-user.target
UNIT
  systemctl daemon-reload
  systemctl enable redis-server
  systemctl start redis-server || {
    echo '--- redis-server status ---'
    systemctl status redis-server --no-pager || true
    echo '--- journal ---'
    journalctl -xeu redis-server --no-pager -n 40 || true
    exit 1
  }
"

# ── App directories and user ──────────────────────────────────────────────────
info "Setting up app user and directories…"
pct exec "$CT_ID" -- bash -c "
  useradd -r -s /usr/sbin/nologin -d /opt/bocrm bocrm
  mkdir -p /opt/bocrm/scripts
  mkdir -p /opt/bocrm/frontend
  chown -R bocrm:bocrm /opt/bocrm
"

# ── Copy management scripts ───────────────────────────────────────────────────
info "Copying management scripts…"
pct push "$CT_ID" scripts/proxmox-reset-db.sh /opt/bocrm/scripts/proxmox-reset-db.sh
pct push "$CT_ID" scripts/proxmox-registry.sh /opt/bocrm/scripts/proxmox-registry.sh
pct exec "$CT_ID" -- bash -c "
  chmod +x /opt/bocrm/scripts/*.sh
  chown bocrm:bocrm /opt/bocrm/scripts/*.sh
"

# ── Write config.env ──────────────────────────────────────────────────────────
info "Writing config…"
# Build APP_CORS_ALLOWED_ORIGINS: include user-provided origins plus container origin
if [ -n "${CORS_ALLOWED_ORIGINS_RAW:-}" ]; then
  # remove any whitespace the user may have accidentally added
  CORS_ALLOWED_ORIGINS_CLEAN=$(echo "${CORS_ALLOWED_ORIGINS_RAW}" | tr -d '[:space:]')
  APP_CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS_CLEAN},http://${CT_IP}:${HTTP_PORT}"
else
  APP_CORS_ALLOWED_ORIGINS="http://${CT_IP}:${HTTP_PORT}"
fi

pct exec "$CT_ID" -- bash -c "cat > /opt/bocrm/config.env && chmod 600 /opt/bocrm/config.env && chown bocrm:bocrm /opt/bocrm/config.env" << EOF
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/crm
SPRING_DATASOURCE_USERNAME=crm
SPRING_DATASOURCE_PASSWORD=${DB_PASS}
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_USERNAME=crm
SPRING_RABBITMQ_PASSWORD=${RMQ_PASS}
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PASSWORD=${REDIS_PASS}
SECURITY_JWT_SECRET=${JWT}
APP_CORS_ALLOWED_ORIGINS=${APP_CORS_ALLOWED_ORIGINS}
EOF

# Write AI provider keys and disable autoconfiguration for missing ones
# Write AI provider keys; exclude Spring AI autoconfiguration for providers with no key
# (Spring AI 2.0-M2 ignores the .enabled property and validates the key unconditionally)
# EXCLUDE_CLASSES=""
# add_exclude() { EXCLUDE_CLASSES="${EXCLUDE_CLASSES:+${EXCLUDE_CLASSES},}$1"; }

[ -n "$ANTHROPIC" ] || add_exclude "org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration"
[ -n "$OPENAI" ]    || add_exclude "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration"
[ -n "$GEMINI" ]    || add_exclude "org.springframework.ai.model.google.genai.autoconfigure.GoogleGenAiChatAutoConfiguration"

{
  [ -n "$ANTHROPIC" ] && echo "ANTHROPIC_API_KEY=${ANTHROPIC}"
  echo "OPENAI_API_KEY=${OPENAI:-placeholder-not-configured}"
  [ -n "$GEMINI" ]    && echo "GEMINI_API_KEY=${GEMINI}"
  [ -n "$TAVILY" ]    && echo "TAVILY_API_KEY=${TAVILY}"
  [ -n "$EXCLUDE_CLASSES" ] && echo "SPRING_AUTOCONFIGURE_EXCLUDE=${EXCLUDE_CLASSES}"
  if [ -n "$AUTH0_ISSUER" ] && [ -n "$AUTH0_CLIENT_ID" ]; then
    echo "APP_EXTERNAL_AUTH_ENABLED=true"
    echo "APP_EXTERNAL_AUTH_ISSUER_URI=${AUTH0_ISSUER}"
    echo "APP_EXTERNAL_AUTH_JWK_SET_URI=${AUTH0_ISSUER%.}.well-known/jwks.json"
    echo "APP_EXTERNAL_AUTH_CLIENT_ID=${AUTH0_CLIENT_ID}"
    echo "APP_EXTERNAL_AUTH_CLIENT_SECRET=${AUTH0_CLIENT_SECRET}"
    echo "APP_EXTERNAL_AUTH_AUDIENCE=${AUTH0_AUDIENCE:-${AUTH0_CLIENT_ID}}"
  fi
} | pct exec "$CT_ID" -- bash -c "cat >> /opt/bocrm/config.env"

# ── Systemd service ───────────────────────────────────────────────────────────
info "Creating systemd service…"
pct exec "$CT_ID" -- bash -c "cat > /etc/systemd/system/bocrm.service" << 'EOF'
[Unit]
Description=BOCRM Backend
After=network.target postgresql.service rabbitmq-server.service redis-server.service

[Service]
User=bocrm
WorkingDirectory=/opt/bocrm
EnvironmentFile=/opt/bocrm/config.env
ExecStart=/usr/bin/java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar /opt/bocrm/bocrm.jar
Environment="JAVA_HOME=/usr/lib/jvm/temurin-21-jre-amd64"
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF
pct exec "$CT_ID" -- systemctl daemon-reload
pct exec "$CT_ID" -- systemctl enable bocrm

# ── nginx ─────────────────────────────────────────────────────────────────────
info "Configuring nginx…"
pct exec "$CT_ID" -- bash -c "cat > /etc/nginx/sites-available/bocrm" << EOF
server {
    listen ${HTTP_PORT};

    root /opt/bocrm/frontend;
    index index.html;

    location /api/ws {
        proxy_pass         http://localhost:8080/api/ws;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade           \$http_upgrade;
        proxy_set_header   Connection        "Upgrade";
        proxy_set_header   Host              \$host;
        proxy_set_header   X-Real-IP         \$remote_addr;
        proxy_set_header   X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto \$scheme;
        proxy_read_timeout 3600s;
    }

    location /api/mcp/message {
        proxy_pass         http://localhost:8080/api/mcp/message;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade           \$http_upgrade;
        proxy_set_header   Connection        "Upgrade";
        proxy_set_header   Host              \$host;
        proxy_set_header   X-Real-IP         \$remote_addr;
        proxy_set_header   X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto \$scheme;
        proxy_buffering    off;
        proxy_request_buffering off;
        proxy_read_timeout 3600s;
    }

    location /api/ {
        proxy_pass         http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header   Host              \$host;
        proxy_set_header   X-Real-IP         \$remote_addr;
        proxy_set_header   X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_connect_timeout 120s;
        proxy_send_timeout 120s;
        proxy_read_timeout 120s;
        client_max_body_size 110m;
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|svg|ico|woff2?)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
EOF
pct exec "$CT_ID" -- bash -c "
  ln -sf /etc/nginx/sites-available/bocrm /etc/nginx/sites-enabled/bocrm
  rm -f /etc/nginx/sites-enabled/default
  rm -f /etc/nginx/sites-available/default
  systemctl enable --now nginx
"

echo
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  LXC provisioned!${NC}"
echo -e "  Container ID : ${CT_ID}"
echo -e "  IP address   : ${CYAN}${CT_IP}${NC}"
echo -e "  Root password: ${YELLOW}${CT_PASSWORD}${NC}"
echo
echo -e "  Next — build and deploy the app from your dev machine:"
echo -e "  ${CYAN}CT_ID=${CT_ID} CT_IP=${CT_IP} bash scripts/deploy.sh${NC}"
echo
echo -e "${CYAN}━━━ MCP Server Setup (Claude Desktop Integration) ━━━${NC}"
echo -e "  Once deployed, you can connect Claude Desktop to use BOCRM tools:"
echo -e "  1. Log in at ${CYAN}http://${CT_IP}:${HTTP_PORT}${NC} and get a JWT token"
echo -e "  2. In Claude Desktop, go to Settings → Developer → MCP Servers"
echo -e "  3. Add MCP server with URL: ${CYAN}http://${CT_IP}:${HTTP_PORT}/api/mcp/message${NC}"
echo -e "  4. Pass JWT token via Authorization header (Bearer token)"
echo -e "  5. Ask Claude to 'explain how to set up BOCRM MCP' for full instructions"
echo
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
