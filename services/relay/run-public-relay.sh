#!/bin/sh
set -eu
umask 077
RELAY_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
RELAY_PYTHON=${RESONANCE_RELAY_PYTHON:-"$RELAY_ROOT/.venv/bin/python"}
RELAY_DATA=${RESONANCE_RELAY_DATA:-/home/container/resonance-relay-data}
test -x "$RELAY_PYTHON" || { echo 'Relay Python environment is not ready' >&2; exit 1; }
mkdir -p "$RELAY_DATA"
exec "$RELAY_PYTHON" "$RELAY_ROOT/websocket_server.py" \
  --port 45902 --budget-file "$RELAY_DATA/budget.json" \
  --daily-bytes "${RESONANCE_RELAY_DAILY_BYTES:-2147483648}" --trust-cloudflare
