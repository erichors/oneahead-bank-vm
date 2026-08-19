#!/usr/bin/env bash
set -euo pipefail

DB_NAME="${DB_NAME:-oneahead}"
DB_USER="${DB_USER:-oneahead}"
DB_PASSWORD="${DB_PASSWORD:-oneahead}"
POSTGRES_UNIT=""

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
    POSTGRES_UNIT="postgresql"
    sudo systemctl enable --now postgresql
    return
  fi

  local unit
  unit="$(systemctl list-unit-files 'postgresql*.service' --no-legend 2>/dev/null | awk 'NR == 1 {print $1}')"
  if [ -n "${unit}" ]; then
    POSTGRES_UNIT="${unit}"
    sudo systemctl enable --now "${unit}"
    return
  fi

  echo "Could not find a PostgreSQL systemd unit." >&2
  exit 1
}

configure_password_auth() {
  local hba_file
  hba_file="$(sudo -u postgres bash -lc "cd /tmp && psql -Atc 'show hba_file;'" 2>/dev/null || true)"
  if [ -z "${hba_file}" ] || ! sudo test -f "${hba_file}"; then
    echo "Could not find pg_hba.conf. Skipping auth file update." >&2
    return
  fi

  sudo cp "${hba_file}" "${hba_file}.bak.$(date +%Y%m%d%H%M%S)"
  local tmp_file
  tmp_file="$(mktemp)"
  {
    echo "host ${DB_NAME} ${DB_USER} 127.0.0.1/32 scram-sha-256"
    echo "host ${DB_NAME} ${DB_USER} ::1/128 scram-sha-256"
    sudo grep -Ev "^host[[:space:]]+${DB_NAME}[[:space:]]+${DB_USER}[[:space:]]+(127\\.0\\.0\\.1/32|::1/128)[[:space:]]+" "${hba_file}"
  } > "${tmp_file}"
  sudo cp "${tmp_file}" "${hba_file}"
  rm -f "${tmp_file}"

  sudo sed -i -E \
    -e 's/^(host[[:space:]]+all[[:space:]]+all[[:space:]]+127\.0\.0\.1\/32[[:space:]]+).*/\1scram-sha-256/' \
    -e 's/^(host[[:space:]]+all[[:space:]]+all[[:space:]]+::1\/128[[:space:]]+).*/\1scram-sha-256/' \
    "${hba_file}"

  if ! sudo grep -Eq '^host[[:space:]]+all[[:space:]]+all[[:space:]]+127\.0\.0\.1/32[[:space:]]+' "${hba_file}"; then
    echo "host all all 127.0.0.1/32 scram-sha-256" | sudo tee -a "${hba_file}" >/dev/null
  fi

  if ! sudo grep -Eq '^host[[:space:]]+all[[:space:]]+all[[:space:]]+::1/128[[:space:]]+' "${hba_file}"; then
    echo "host all all ::1/128 scram-sha-256" | sudo tee -a "${hba_file}" >/dev/null
  fi

  sudo systemctl reload "${POSTGRES_UNIT}" 2>/dev/null || sudo systemctl restart "${POSTGRES_UNIT}"
}

configure_database() {
  sudo -u postgres bash -lc "cd /tmp && psql" -- \
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
configure_password_auth
configure_database

cat <<EOF
Local PostgreSQL is ready.

SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/${DB_NAME}
SPRING_DATASOURCE_USERNAME=${DB_USER}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
EOF
