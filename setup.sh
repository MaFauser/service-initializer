#!/bin/bash
# Interactive project setup — run once after cloning the template.
# Replaces placeholder values across the entire codebase.

set -e

# ── Colors & formatting ─────────────────────────────────────────────────────

BOLD='\033[1m'
DIM='\033[2m'
CYAN='\033[1;36m'
GREEN='\033[1;32m'
YELLOW='\033[1;33m'
RED='\033[1;31m'
BLUE='\033[1;34m'
MAGENTA='\033[1;35m'
NC='\033[0m'

CHECKMARK="${GREEN}✓${NC}"
ARROW="${CYAN}→${NC}"
DOT="${DIM}·${NC}"

clear

# ── Banner ───────────────────────────────────────────────────────────────────

echo ""
echo -e "${CYAN}  ╭─────────────────────────────────────────╮${NC}"
echo -e "${CYAN}  │                                         │${NC}"
echo -e "${CYAN}  │${NC}   ${BOLD}Service Initializer Setup${NC}             ${CYAN}│${NC}"
echo -e "${CYAN}  │${NC}   ${DIM}Configure your new microservice${NC}       ${CYAN}│${NC}"
echo -e "${CYAN}  │                                         │${NC}"
echo -e "${CYAN}  ╰─────────────────────────────────────────╯${NC}"
echo ""

# ── Defaults (current template values) ──────────────────────────────────────

TEMPLATE_GROUP="com.mafauser"
TEMPLATE_PACKAGE="com.mafauser.service"
TEMPLATE_PACKAGE_DIR="com/mafauser/service"
TEMPLATE_SERVICE_NAME="service"
TEMPLATE_DB_NAME="servicedb"
TEMPLATE_CONTAINER_PREFIX="service"
TEMPLATE_DESCRIPTION="Default Backend Service Initializer"

# ── Prompt helper ────────────────────────────────────────────────────────────

prompt() {
  local label="$1" default="$2" var_name="$3" hint="${4:-}"
  local value
  if [ -n "$hint" ]; then
    echo -e "  ${BOLD}${label}${NC} ${DIM}${hint}${NC}"
  else
    echo -e "  ${BOLD}${label}${NC}"
  fi
  printf "  ${CYAN}❯${NC} ${DIM}(${default})${NC} "
  read -r value
  value="${value:-$default}"
  eval "$var_name='$value'"
  echo ""
}

# ── Gather input ─────────────────────────────────────────────────────────────

echo -e "  ${DIM}Answer a few questions to configure your project.${NC}"
echo -e "  ${DIM}Press Enter to accept the default in parentheses.${NC}"
echo ""

prompt "Service name" "my-service" SERVICE_NAME "e.g. order-service, user-api"

DEFAULT_DB="${SERVICE_NAME//-/_}_db"
prompt "Database name" "$DEFAULT_DB" DB_NAME "PostgreSQL database"

GROUP_ID="com.mafauser"

DEFAULT_PACKAGE="${GROUP_ID}.${SERVICE_NAME//-/.}"
prompt "Base package" "$DEFAULT_PACKAGE" BASE_PACKAGE "Java/Kotlin package"

prompt "Description" "A microservice built with Spring Boot" DESCRIPTION ""

# ── Derived values ───────────────────────────────────────────────────────────

PACKAGE_DIR="${BASE_PACKAGE//\.//}"
CONTAINER_PREFIX="${SERVICE_NAME}"
APP_CLASS_NAME="Application"

# ── Confirm ──────────────────────────────────────────────────────────────────

echo -e "  ${CYAN}──────────────────────────────────────────${NC}"
echo -e "  ${BOLD}Review your configuration:${NC}"
echo ""
echo -e "  ${DOT} Service name     ${ARROW}  ${GREEN}${SERVICE_NAME}${NC}"
echo -e "  ${DOT} Database         ${ARROW}  ${GREEN}${DB_NAME}${NC}"
echo -e "  ${DOT} Base package     ${ARROW}  ${GREEN}${BASE_PACKAGE}${NC}"
echo -e "  ${DOT} Description      ${ARROW}  ${GREEN}${DESCRIPTION}${NC}"
echo ""

printf "  ${BOLD}Proceed?${NC} ${DIM}(Y/n)${NC} "
read -r CONFIRM
CONFIRM="${CONFIRM:-Y}"
if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
  echo -e "\n  ${RED}Aborted.${NC}\n"
  exit 0
fi

echo ""

# ── Rename package directories ───────────────────────────────────────────────

step() {
  echo -e "  ${CHECKMARK} $1"
}

echo -e "  ${YELLOW}Setting up ${SERVICE_NAME}...${NC}"
echo ""

if [ "$TEMPLATE_PACKAGE_DIR" != "$PACKAGE_DIR" ]; then
  for base in src/main/kotlin src/test/kotlin; do
    if [ -d "$base/$TEMPLATE_PACKAGE_DIR" ]; then
      mkdir -p "$base/$PACKAGE_DIR"
      cp -R "$base/$TEMPLATE_PACKAGE_DIR/"* "$base/$PACKAGE_DIR/" 2>/dev/null || true
      rm -rf "$base/$TEMPLATE_PACKAGE_DIR"
      # Clean up empty parent dirs left behind (but not shared ancestors)
      _cleanup_dir="$base/$(dirname "$TEMPLATE_PACKAGE_DIR")"
      while [ "$_cleanup_dir" != "$base" ] && [ -d "$_cleanup_dir" ] && [ -z "$(ls -A "$_cleanup_dir" 2>/dev/null)" ]; do
        rmdir "$_cleanup_dir"
        _cleanup_dir="$(dirname "$_cleanup_dir")"
      done
    fi
  done
  step "Moved source packages to ${DIM}${PACKAGE_DIR}${NC}"
fi

# ── Replace in source files ──────────────────────────────────────────────────

replace_in_files() {
  local old="$1" new="$2"
  if [ "$old" = "$new" ]; then return; fi

  local sed_old
  sed_old=$(printf '%s' "$old" | sed 's/[.[\*^$()|]/\\&/g')

  if command -v rg &>/dev/null; then
    rg -l --no-messages --fixed-strings "$old" --type-add 'cfg:*.{kt,kts,yaml,yml,xml,json,properties,env,graphqls,sh,md,sql}' -t cfg . 2>/dev/null | while read -r file; do
      if [[ "$file" == *"setup.sh" ]]; then continue; fi
      if [[ "$file" == *".git/"* ]]; then continue; fi
      sed -i '' "s|${sed_old}|${new}|g" "$file"
    done
  else
    find . -type f \( -name '*.kt' -o -name '*.kts' -o -name '*.yaml' -o -name '*.yml' \
      -o -name '*.xml' -o -name '*.json' -o -name '*.properties' -o -name '*.env' \
      -o -name '*.graphqls' -o -name '*.sh' -o -name '*.md' -o -name '*.sql' \) \
      -not -path './.git/*' -not -name 'setup.sh' | while read -r file; do
      sed -i '' "s|${sed_old}|${new}|g" "$file"
    done
  fi
}

# Package references (order matters: longest match first)
replace_in_files "$TEMPLATE_PACKAGE" "$BASE_PACKAGE"
step "Updated package declarations and imports"

# Group ID in build.gradle.kts
replace_in_files "group = \"$TEMPLATE_GROUP\"" "group = \"${GROUP_ID}\""
step "Updated Gradle group ID"

# Description
replace_in_files "description = \"$TEMPLATE_DESCRIPTION\"" "description = \"${DESCRIPTION}\""
step "Updated project description"

# Application name in settings.gradle.kts and Spring config
replace_in_files "rootProject.name = \"$TEMPLATE_SERVICE_NAME\"" "rootProject.name = \"${SERVICE_NAME}\""
step "Updated settings.gradle.kts"

# spring.application.name (targeted — generic "name: service" matches container_name:, Username:, etc.)
sed -i '' "s|name: ${TEMPLATE_SERVICE_NAME}|name: ${SERVICE_NAME}|g" src/main/resources/application.yaml
step "Updated Spring application name"

# Database name
replace_in_files "$TEMPLATE_DB_NAME" "$DB_NAME"
step "Updated database name"

# Docker container names
replace_in_files "container_name: ${TEMPLATE_CONTAINER_PREFIX}-" "container_name: ${CONTAINER_PREFIX}-"
step "Updated Docker container names"

# JaCoCo exclude path
replace_in_files "**/com/mafauser/service/Application" "**/${PACKAGE_DIR}/Application"
step "Updated JaCoCo excludes"

# Also handle the .env file
if [ -f ".env" ]; then
  sed -i '' "s|${TEMPLATE_DB_NAME}|${DB_NAME}|g" ".env"
  step "Updated .env defaults"
fi

# ── Clean up bin/ (stale build output) ───────────────────────────────────────

if [ -d "bin" ]; then
  rm -rf bin
  step "Removed stale build output ${DIM}(bin/)${NC}"
fi

# ── Remove this setup script ────────────────────────────────────────────────

rm -f setup.sh
step "Removed setup script ${DIM}(one-time use)${NC}"

# ── Done ─────────────────────────────────────────────────────────────────────

echo ""
echo -e "  ${CYAN}──────────────────────────────────────────${NC}"
echo -e "  ${GREEN}${BOLD}Done!${NC} Your project ${BOLD}${SERVICE_NAME}${NC} is ready."
echo ""
echo -e "  ${DIM}Next steps:${NC}"
echo ""
echo -e "    ${ARROW}  ${BOLD}docker compose up -d${NC}       ${DIM}start dependencies${NC}"
echo -e "    ${ARROW}  ${BOLD}make run${NC}                   ${DIM}start the app${NC}"
echo -e "    ${ARROW}  ${BOLD}make test${NC}                  ${DIM}run tests${NC}"
echo ""
echo -e "    ${ARROW}  ${BOLD}git add -A && git commit${NC}   ${DIM}commit your configured project${NC}"
echo ""
