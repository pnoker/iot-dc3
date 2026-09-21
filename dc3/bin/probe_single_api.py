#!/usr/bin/env python3
"""Probe every OpenAPI endpoint of the running dc3-center-single service.

Read endpoints are called directly; write endpoints are probed safely
(empty body or a nonexistent id 999999) so nothing is created or deleted.
An endpoint counts as reachable when the app answers with 2xx or a
deliberate 4xx problem-details response (parameter validation, resource
not found). Bare 404s, 401/403, 5xx and connection failures are flagged.
"""

import hashlib
import hmac
import json
import re
import sys
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8100/single"
ENV_FILE = "/mnt/c/Users/pnoker/Code/github/iot-dc3/.env"
SPEC_FILE = "/tmp/openapi.json"

PRINCIPAL = '{"principalId":1,"principalType":"USER","displayName":"DC3","principalName":"dc3","tenantId":1}'


def hmac_secret():
    for line in open(ENV_FILE, encoding="utf-8"):
        if line.startswith("AUTH_HMAC_SECRET="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("AUTH_HMAC_SECRET not found")


def probe(method, path, headers):
    p = re.sub(r"\{[^}]+\}", "999999", path)
    data = b"{}" if method in ("POST", "PUT", "PATCH") else None
    req = urllib.request.Request(BASE + p, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, ""
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", errors="replace")[:200]
    except Exception as e:  # noqa: BLE001 - report transport failures
        return -1, str(e)


def main():
    secret = hmac_secret()
    sign = hmac.new(secret.encode(), PRINCIPAL.encode(), hashlib.sha256).hexdigest()
    headers = {
        "Content-Type": "application/json",
        "X-Auth-Principal": PRINCIPAL,
        "X-Auth-Sign": sign,
        "X-Auth-Tenant": "default",
        "X-Auth-Login": "dc3",
    }

    spec = json.load(open(SPEC_FILE, encoding="utf-8"))
    ops = [
        (m.upper(), p)
        for p, item in spec.get("paths", {}).items()
        for m in ("get", "post", "put", "delete", "patch")
        if m in item
    ]
    print(f"OpenAPI endpoints: {len(ops)}")

    dist = {}
    flagged = []
    for method, path in ops:
        code, body = probe(method, path, headers)
        dist.setdefault(code, []).append(f"{method} {path}")
        app_level = '"title"' in body or "R404" in body
        if code == -1 or code >= 500 or code in (401, 403) or (code == 404 and not app_level):
            flagged.append((method, path, code, body[:120]))

    print("\n=== status distribution ===")
    for code in sorted(dist, key=lambda x: (x < 0, x)):
        print(f"  {code}: {len(dist[code])}")

    print("\n=== flagged endpoints ===")
    for method, path, code, body in flagged:
        print(f"  [{code}] {method} {path} | {body}")

    summary = {
        "total": len(ops),
        "dist": {str(k): len(v) for k, v in dist.items()},
        "flagged": [(m, p, c) for m, p, c, _ in flagged],
        "bare404": dist.get(404, []),
    }
    json.dump(summary, open("/tmp/probe-summary.json", "w", encoding="utf-8"), indent=1)
    print("\nsummary -> /tmp/probe-summary.json")
    return 1 if flagged else 0


if __name__ == "__main__":
    sys.exit(main())
