#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
DEBUG=false PORT="${PORT:-8084}" java -jar credit-service/target/banking-credit-service-1.0.0.jar
