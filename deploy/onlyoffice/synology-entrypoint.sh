#!/bin/bash
set -euo pipefail
host="${EAC_OFFICE_BACKEND_HOST:-eac-system}"
ip=""
for attempt in $(seq 1 30); do
  ip="$(getent ahostsv4 "$host" | awk 'NR==1 {print $1}' || true)"
  if [[ "$ip" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then break; fi
  sleep 2
done
if [[ ! "$ip" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then echo 'Cannot resolve the EAC backend container.' >&2; exit 1; fi
# Allow the resolved backend IP only; retain private/metadata request filtering.
printf -v NODE_CONFIG '{"services":{"CoAuthoring":{"request-filtering-agent":{"allowPrivateIPAddress":false,"allowMetaIPAddress":false,"allowIPAddressList":["%s"]}}}}' "$ip"
export NODE_CONFIG
exec /app/ds/run-document-server.sh "$@"
