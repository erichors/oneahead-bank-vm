#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../frontend"
BROWSER=none REACT_APP_API_URL="${REACT_APP_API_URL:-http://localhost:8082}" PORT="${PORT:-8081}" npm start
