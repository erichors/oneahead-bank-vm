#!/usr/bin/env bash
set -euo pipefail

DB_NAME="${DB_NAME:-oneahead}"
DB_USER="${DB_USER:-oneahead}"
DB_PASSWORD="${DB_PASSWORD:-oneahead}"

install_postgres() {
  if command -v psql >/dev/null 2>&1; then
    return
  fi

  if command -v dnf >/dev/null 2>&1; then
    sudo dnf install -y postgresql15 postgresql15-server
    return
  fi

  if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update
    sudo apt-get install -y postgresql postgresql-contrib
    return
  fi

  echo "Could not find dnf or apt-get. Install PostgreSQL manually, then rerun this script." >&2
  exit 1
}

init_postgres() {
  if command -v postgresql-setup >/dev/null 2>&1; then
    sudo postgresql-setup --initdb || true
  fi
}

start_postgres() {
  if systemctl list-unit-files postgresql.service --no-legend 2>/dev/null | grep -q '^postgresql.service'; then
    sudo systemctl enable --now postgresql
    return
  fi

  local unit
  unit="$(systemctl list-unit-files 'postgresql*.service' --no-legend 2>/dev/null | awk 'NR == 1 {print $1}')"
  if [ -n "${unit}" ]; then
    sudo systemctl enable --now "${unit}"
    return
  fi

  echo "Could not find a PostgreSQL systemd unit." >&2
  exit 1
}

configure_database() {
  sudo -u postgres psql \
    -v db_name="${DB_NAME}" \
    -v db_user="${DB_USER}" \
    -v db_password="${DB_PASSWORD}" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'db_user', :'db_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'db_user')\gexec

SELECT format('ALTER ROLE %I WITH PASSWORD %L', :'db_user', :'db_password')\gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'db_name', :'db_user')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'db_name')\gexec

SELECT format('GRANT ALL PRIVILEGES ON DATABASE %I TO %I', :'db_name', :'db_user')\gexec
SQL
}

install_postgres
init_postgres
start_postgres
configure_database

cat <<EOF
Local PostgreSQL is ready.

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/${DB_NAME}
SPRING_DATASOURCE_USERNAME=${DB_USER}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
EOF
