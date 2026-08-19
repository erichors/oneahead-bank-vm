#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

CREDIT_HOST="${CREDIT_HOST:-localhost}"
CREDIT_PORT="${CREDIT_PORT:-8084}"

export DEBUG="${DEBUG:-false}"
export PORT="${PORT:-8082}"
export DT_TAGS="${DT_TAGS:-app_name=ABNK tier=backend}"
export CREDIT_SERVICE_URL="${CREDIT_SERVICE_URL:-http://${CREDIT_HOST}:${CREDIT_PORT}/api/credit/check}"
export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5432/oneahead}"
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-oneahead}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-oneahead}"
export SPRING_DATASOURCE_DRIVER_CLASS_NAME="${SPRING_DATASOURCE_DRIVER_CLASS_NAME:-org.postgresql.Driver}"

echo "Starting backend on port ${PORT}"
echo "Using credit service: ${CREDIT_SERVICE_URL}"
echo "Using database: ${SPRING_DATASOURCE_URL}"
echo "Using Dynatrace tags: ${DT_TAGS}"

java -jar backend/target/banking-backend-1.0.0.jar
