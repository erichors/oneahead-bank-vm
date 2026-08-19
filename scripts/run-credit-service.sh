#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

export DEBUG="${DEBUG:-false}"
export PORT="${PORT:-8084}"
export DT_TAGS="${DT_TAGS:-app_name=ABNK tier=credit}"

echo "Starting credit service on port ${PORT}"
echo "Using Dynatrace tags: ${DT_TAGS}"

java -jar credit-service/target/banking-credit-service-1.0.0.jar
