#!/usr/bin/env python3
"""Probe every OpenAPI endpoint of a running DC3 stack through its gateway.

The OpenAPI specs are pulled directly from each center (auth/manager/
data/agentic) and every operation is then called through the gateway's
public route (/api/v3/{svc}{path}) with a real login cookie, so the whole
chain (gateway routing + JWT + center + store + database) is exercised.

Safe by construction: read operations are called directly; write
operations carry an empty body or a nonexistent id (999999) so nothing
is created or deleted. 2xx and deliberate 4xx problem-details answers
count as reachable; bare 404s, 401/403, 5xx and transport failures are
flagged for review.
"""

import json
import os
import re
import sys
import urllib.error
import urllib.request

GATEWAY = os.environ.get("DC3_PROBE_GATEWAY", "http://127.0.0.1:8000")
CENTERS = [
    ("auth", "http://127.0.0.1:8300/auth"),
    ("manager", "http://127.0.0.1:8400/manager"),
    ("data", "http://127.0.0.1:8500/data"),
    ("agentic", "http://127.0.0.1:8600/agentic"),
]
LOGIN = {"tenant": "default", "name": "dc3", "password": "dc3dc3dc3"}


def http(url, data=None, headers=None, method=None, timeout=20):
    body = json.dumps(data).encode() if data is not None else None
    hdr = {"Content-Type": "application/json"}
    hdr.update(headers or {})
    req = urllib.request.Request(url, data=body, headers=hdr, method=method)
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.status, r.read().decode("utf-8", errors="replace")


def login_cookie():
    """Two-step login against the gateway; returns the auth cookie pair."""
    _, salt = http(f"{GATEWAY}/api/v3/auth/token/salt", {"tenant": LOGIN["tenant"], "name": LOGIN["name"]})
    payload = dict(LOGIN, salt=salt.strip().strip('"'))
    req = urllib.request.Request(
        f"{GATEWAY}/api/v3/auth/token/generate",
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=20) as r:
        return r.headers.get("Set-Cookie", "")


def collect_endpoints():
    ops = []
    unresolved = []
    for svc, base in CENTERS:
        try:
            code, body = http(f"{base}/v3/api-docs", timeout=90)
            spec = json.loads(body)
        except Exception:  # noqa: BLE001 - a center may hang on native
            unresolved.append(svc)
            continue
        for path, item in spec.get("paths", {}).items():
            for m in ("get", "post", "put", "delete", "patch"):
                if m in item:
                    ops.append((svc, m.upper(), path))
    if unresolved:
        # Fallback: the dc3-center-single aggregated spec carries every
        # domain's paths without a prefix; match by path hints.
        hints = {"agentic": ("session", "message", "model", "provider", "chat", "action", "attachment"),
                 "manager": ("driver", "device", "point", "profile", "command", "event", "group", "label"),
                 "data": ("point_value", "point_command", "alarm", "rule", "notify", "dashboard", "analytics", "event_history", "command_history"),
                 "auth": ("token", "user", "tenant", "role", "menu", "resource", "api", "oauth", "mcp", "audit")}
        try:
            agg = json.load(open("/tmp/openapi.json", encoding="utf-8"))
        except OSError:
            print(f"no fallback spec; unprobed domains: {unresolved}")
            return ops
        seen = {(s, m, p) for s, m, p in ops}
        for svc in unresolved:
            for path, item in agg.get("paths", {}).items():
                seg = path.strip("/").split("/")[0]
                if seg not in hints[svc]:
                    continue
                for m in ("get", "post", "put", "delete", "patch"):
                    if m in item and (svc, m.upper(), path) not in seen:
                        ops.append((svc, m.upper(), path))
        print(f"fallback spec used for: {unresolved}")
    return ops


def probe(method, url, cookie):
    headers = {"Cookie": cookie, "X-Auth-Tenant": "default", "X-Auth-Login": "dc3"}
    data = b"{}" if method in ("POST", "PUT", "PATCH") else None
    if data is not None:
        # @RequestBody endpoints 415 without a media type on the payload.
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, ""
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", errors="replace")[:200]
    except Exception as e:  # noqa: BLE001 - report transport failures
        return -1, str(e)


def main():
    cookie = login_cookie()
    if not cookie:
        print("login failed: no Set-Cookie")
        return 2
    print(f"login ok: {cookie[:40]}...")

    ops = collect_endpoints()
    print(f"endpoints collected: {len(ops)} "
          f"({json.dumps({s: sum(1 for x in ops if x[0] == s) for s, _ in CENTERS})})")

    dist = {}
    flagged = []
    for svc, method, path in ops:
        p = re.sub(r"\{[^}]+\}", "999999", path)
        code, body = probe(method, f"{GATEWAY}/api/v3/{svc}{p}", cookie)
        dist.setdefault(code, []).append(f"[{svc}] {method} {path}")
        app_level = '"title"' in body or "R404" in body
        # 415s are anomalous at scale: the probe sends JSON to every write
        # endpoint, so a native-image media-type regression shows up here.
        if code == -1 or code >= 500 or code in (401, 403, 415) or (code == 404 and not app_level):
            flagged.append((svc, method, path, code, body[:100]))

    print("\n=== status distribution ===")
    for code in sorted(dist, key=lambda x: (x < 0, x)):
        print(f"  {code}: {len(dist[code])}")

    print("\n=== flagged endpoints ===")
    for svc, method, path, code, body in flagged:
        print(f"  [{code}] [{svc}] {method} {path} | {body}")

    json.dump(
        {"total": len(ops),
         "dist": {str(k): len(v) for k, v in dist.items()},
         "flagged": [(s, m, p, c) for s, m, p, c, _ in flagged]},
        open("/tmp/probe-gateway-summary.json", "w", encoding="utf-8"), indent=1)
    print("\nsummary -> /tmp/probe-gateway-summary.json")
    return 1 if flagged else 0


if __name__ == "__main__":
    sys.exit(main())
