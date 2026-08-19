#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

CREDIT_HOST="${CREDIT_HOST:-localhost}"
CREDIT_PORT="${CREDIT_PORT:-8084}"

export DEBUG="${DEBUG:-false}"
export PORT="${PORT:-8082}"
export CREDIT_SERVICE_URL="${CREDIT_SERVICE_URL:-http://${CREDIT_HOST}:${CREDIT_PORT}/api/credit/check}"

echo "Starting backend on port ${PORT}"
echo "Using credit service: ${CREDIT_SERVICE_URL}"

java -jar backend/target/banking-backend-1.0.0.jar
