#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://127.0.0.1:8082}"
RATE="${RATE:-5}"
DURATION_SECONDS="${DURATION_SECONDS:-300}"
CONCURRENT="${CONCURRENT:-false}"

end_at=$((SECONDS + DURATION_SECONDS))
request_id=0

send_group() {
  request_id=$((request_id + 1))
  local amount=$((RANDOM % 250 + 10))
  local account=$((RANDOM % 9000 + 1000))
  local ssn_part=$((RANDOM % 900 + 100))

  curl -sS -o /dev/null "$BACKEND_URL/api/account/balance"
  curl -sS -o /dev/null -X POST "$BACKEND_URL/api/account/deposit" -H 'content-type: application/json' -d "{\"amount\":$amount,\"metadata\":\"driver deposit $request_id\"}"
  curl -sS -o /dev/null -X POST "$BACKEND_URL/api/account/transfer" -H 'content-type: application/json' -d "{\"toAccount\":\"$account\",\"amount\":1,\"metadata\":\"driver transfer $request_id\"}"
  curl -sS -o /dev/null -X POST "$BACKEND_URL/api/credit/check" -H 'content-type: application/json' -d "{\"ssn\":\"$ssn_part-45-6789\",\"metadata\":\"driver credit $request_id\"}"
}

printf 'Driving traffic to %s at about %s request groups/sec for %ss (concurrent=%s)\n' "$BACKEND_URL" "$RATE" "$DURATION_SECONDS" "$CONCURRENT"

while [ "$SECONDS" -lt "$end_at" ]; do
  for _ in $(seq 1 "$RATE"); do
    if [ "$CONCURRENT" = "true" ]; then
      send_group &
    else
      send_group || true
    fi
  done
  if [ "$CONCURRENT" = "true" ]; then
    wait || true
  fi
  sleep 1
  printf '.'
done

printf '\nDone. Sent about %s request groups.\n' "$request_id"
