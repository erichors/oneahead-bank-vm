#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
DEBUG=false PORT="${PORT:-8082}" CREDIT_SERVICE_URL="${CREDIT_SERVICE_URL:-http://localhost:8084/api/credit/check}" java -jar backend/target/banking-backend-1.0.0.jar
