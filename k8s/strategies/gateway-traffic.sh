#!/usr/bin/env bash
set -euo pipefail

NS="${NS:-url-shortener-namespace}"
EG_NS="${EG_NS:-envoy-gateway-system}"
LOCAL_PORT="${LOCAL_PORT:-8888}"
RATE="${RATE:-0.5}"
PATH_TO_POLL="${PATH_TO_POLL:-/api/version}"
HEADER="${HEADER:-}"
COUNT="${1:-0}"

SVC="$(kubectl get svc -n "$EG_NS" \
  -l gateway.envoyproxy.io/owning-gateway-name=eg \
  -o jsonpath='{.items[0].metadata.name}')"

if [[ -z "$SVC" ]]; then
  echo "Не найден Envoy Service для Gateway eg" >&2
  exit 1
fi

echo "Port-forward $EG_NS/svc/$SVC -> localhost:$LOCAL_PORT"
kubectl port-forward -n "$EG_NS" "svc/$SVC" "$LOCAL_PORT:80" \
  >/tmp/gateway-traffic-pf.log 2>&1 &
PF_PID=$!
trap 'kill "$PF_PID" 2>/dev/null || true' EXIT INT TERM

for _ in $(seq 1 30); do
  curl -sf "http://localhost:${LOCAL_PORT}${PATH_TO_POLL}" >/dev/null 2>&1 && break
  sleep 0.5
done

echo "Gateway: http://localhost:${LOCAL_PORT}${PATH_TO_POLL}  rate=${RATE}s"
echo "-------------------------------------------------------------------"

i=0
while :; do
  if [[ -n "$HEADER" ]]; then
    body="$(curl -s -H "$HEADER" "http://localhost:${LOCAL_PORT}${PATH_TO_POLL}" || echo '{"error":"no response"}')"
  else
    body="$(curl -s "http://localhost:${LOCAL_PORT}${PATH_TO_POLL}" || echo '{"error":"no response"}')"
  fi

  printf '%s | %s\n' "$(date +%H:%M:%S)" "$body"

  i=$((i + 1))
  if [[ "$COUNT" -gt 0 && "$i" -ge "$COUNT" ]]; then break; fi
  sleep "$RATE"
done