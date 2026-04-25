#!/usr/bin/env bash
# =============================================================================
# BOCRM — build a release package for native LXC deployment
# Run on your dev machine from the repo root.
# Usage:  bash scripts/build.sh [version]
# Output: dist/bocrm-<version>.tar.gz
#           bocrm.jar
#           frontend/  (static files)
# =============================================================================
set -euo pipefail

VERSION="${1:-$(date +%Y%m%d-%H%M%S)}"
PACKAGE="bocrm-${VERSION}.tar.gz"

CYAN='\033[0;36m'; GREEN='\033[0;32m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STAGING="$(mktemp -d)"
trap 'rm -rf "$STAGING"' EXIT

mkdir -p "$REPO_ROOT/dist" "$STAGING/frontend"

# ── Backend ───────────────────────────────────────────────────────────────────
info "Building backend JAR…"
cd "$REPO_ROOT/backend"
./gradlew bootJar -q
JAR=$(ls build/libs/*.jar | grep -v plain | head -1)
cp "$JAR" "$STAGING/bocrm.jar"
success "Backend done"

# ── Frontend ──────────────────────────────────────────────────────────────────
info "Building frontend…"
cd "$REPO_ROOT/frontend"
npm ci --silent
# Mirror CI: export OIDC vars from .env.local, then hide the file so no other
# local overrides (e.g. VITE_API_URL pointing at a dev host) leak into the build.
ENV_LOCAL="$REPO_ROOT/frontend/.env.local"
if [[ -f "$ENV_LOCAL" ]]; then
  while IFS= read -r line; do
    [[ "$line" =~ ^[[:space:]]*# || -z "${line// }" ]] && continue
    [[ "$line" =~ ^VITE_OIDC_ ]] && export "$line"
  done < "$ENV_LOCAL"
  mv "$ENV_LOCAL" "${ENV_LOCAL}.bak"
  trap 'mv "${ENV_LOCAL}.bak" "$ENV_LOCAL" 2>/dev/null; rm -rf "$STAGING"' EXIT
fi
VITE_API_URL=/api npm run build
[[ -f "${ENV_LOCAL}.bak" ]] && mv "${ENV_LOCAL}.bak" "$ENV_LOCAL"
cp -r dist/. "$STAGING/frontend/"
success "Frontend done"

# ── Package ───────────────────────────────────────────────────────────────────
info "Creating package…"
cd "$REPO_ROOT"
tar -czf "dist/$PACKAGE" -C "$STAGING" .
success "Package: dist/$PACKAGE"

echo
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  Build complete!${NC}"
echo -e "  Package  : dist/${PACKAGE}"
echo -e "  Size     : $(du -sh "dist/$PACKAGE" | cut -f1)"
echo
echo -e "  Deploy to LXC:"
echo -e "  ${CYAN}CT_ID=201 PACKAGE=${PACKAGE} bash scripts/deploy.sh${NC}"
echo
echo -e "  Or copy directly to a remote server via scp:"
echo -e "  ${CYAN}scp dist/${PACKAGE} user@your-server:/opt/bocrm/${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
