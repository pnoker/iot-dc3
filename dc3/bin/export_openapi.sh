#!/usr/bin/env bash
#
# Copyright 2016-present the IoT DC3 original author or authors.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

# Export the OpenAPI specification of each running center service to JSON.
#
# This snapshots the REST contract for review, diffing, or client generation.
# It fetches docs from already-running services (started in a dev/test profile);
# it does not start anything. Point it at the gateway aggregation endpoints by
# default, or at each center directly via env overrides.
#
# Usage:
#   dc3/bin/export_openapi.sh [OUTPUT_DIR]
#
# Env:
#   OPENAPI_BASE   Base URL of the gateway (default http://127.0.0.1:8000)
#   OUTPUT_DIR     Output directory (default dc3/doc/openapi); arg overrides env

set -euo pipefail

BASE="${OPENAPI_BASE:-http://127.0.0.1:8000}"
OUT="${1:-${OUTPUT_DIR:-dc3/doc/openapi}}"

# service -> gateway aggregation path
SERVICES=(auth manager data agentic)

mkdir -p "$OUT"

fail=0
for svc in "${SERVICES[@]}"; do
  url="${BASE}/v3/api-docs/${svc}"
  dest="${OUT}/openapi-${svc}.json"
  printf 'Exporting %-8s <- %s\n' "$svc" "$url"
  code=$(curl -s -o "$dest" -w '%{http_code}' "$url" || echo 000)
  if [[ "$code" != "200" ]]; then
    echo "  ERROR: HTTP $code for $url (is the stack up in a dev/test profile?)" >&2
    rm -f "$dest"
    fail=1
    continue
  fi
  # Pretty-print and validate JSON when python3 is available.
  if command -v python3 >/dev/null 2>&1; then
    if ! python3 -m json.tool "$dest" > "${dest}.tmp" 2>/dev/null; then
      echo "  ERROR: invalid JSON returned for $svc" >&2
      rm -f "$dest" "${dest}.tmp"
      fail=1
      continue
    fi
    mv "${dest}.tmp" "$dest"
  fi
  echo "  wrote $dest"
done

exit "$fail"
