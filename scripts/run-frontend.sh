#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../frontend"

BACKEND_HOST="${BACKEND_HOST:-localhost}"
BACKEND_PORT="${BACKEND_PORT:-8082}"

export BROWSER="${BROWSER:-none}"
export PORT="${PORT:-8081}"
export DT_TAGS="${DT_TAGS:-app_name=ABNK tier=frontend}"
export BACKEND_URL="${BACKEND_URL:-http://${BACKEND_HOST}:${BACKEND_PORT}}"

echo "Starting frontend on port ${PORT}"
echo "Using backend: ${BACKEND_URL}"
echo "Using Dynatrace tags: ${DT_TAGS}"

if [ ! -d build ]; then
  echo "Missing build directory; building frontend first."
  npm run build
fi

npm run serve
