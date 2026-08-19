#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

export DEBUG="${DEBUG:-false}"
export PORT="${PORT:-8084}"

echo "Starting credit service on port ${PORT}"

java -jar credit-service/target/banking-credit-service-1.0.0.jar
