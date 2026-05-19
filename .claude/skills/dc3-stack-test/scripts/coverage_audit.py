#!/usr/bin/env python3
"""
DC3 endpoint coverage audit.

Walks the iot-dc3 backend `@*Mapping` controllers and the iot-dc3-web
frontend `httpGet` / `httpPost` call sites, then prints three buckets:

  Both              : an HTTP path called by FE that the BE serves (matched)
  Frontend-only     : FE calls a path the BE no longer exposes (stale)
  Backend-only      : BE exposes a path no FE function targets (uncovered)

Run from the iot-dc3 repo root, with iot-dc3-web checked out at
../iot-dc3-web/. No deps beyond the stdlib.

Notes:
- The frontend regex tolerates nested `<R<X>>` type parameters and
  helper-wrapped paths via `${base}/<verb>` only when ${base} resolves
  back to one of API_AUTH_BASE / API_MANAGER_BASE / API_DATA_BASE /
  API_AGENTIC_BASE. Helpers that defer the prefix to a runtime variable
  (e.g. alarm.ts's `endpoints` map) are not statically resolvable —
  count them by reading alarm.ts directly.
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

BACKEND_ROOT = Path("/home/pnoker/code/github/iot-dc3")
FRONTEND_ROOT = Path("/home/pnoker/code/github/iot-dc3-web")


def load_url_prefix_constants() -> dict[str, str]:
    """Read every `XXX_URL_PREFIX = "/yyy"` from the constants module."""
    prefix_map: dict[str, str] = {}
    out = subprocess.check_output(
        [
            "grep",
            "-rhE",
            r'URL_PREFIX\s*=\s*"[^"]+"',
            "--include=*Constant.java",
            str(BACKEND_ROOT),
        ],
        text=True,
    )
    for line in out.splitlines():
        if "target" in line:
            continue
        m = re.search(r'(\w+_URL_PREFIX)\s*=\s*"([^"]+)"', line)
        if m:
            prefix_map[m.group(1)] = m.group(2)
    return prefix_map


def collect_backend_endpoints(prefix_map: dict[str, str]) -> set[str]:
    """Combine class-level @RequestMapping with method-level @*Mapping."""
    endpoints: set[str] = set()
    out = subprocess.check_output(
        [
            "grep",
            "-rln",
            "@RequestMapping",
            "--include=*Controller.java",
            str(BACKEND_ROOT),
        ],
        text=True,
    )
    for path in out.strip().splitlines():
        if "target" in path:
            continue
        src = Path(path).read_text()
        m = re.search(
            r"@RequestMapping\((?:value\s*=\s*)?(?:[A-Za-z]+\.)?([A-Z][A-Z_0-9]+_URL_PREFIX)\)",
            src,
        )
        base = prefix_map.get(m.group(1), "") if m else ""
        if not base:
            literal = re.search(r'@RequestMapping\("([^"]+)"\)', src)
            if literal:
                base = literal.group(1)
        for mm in re.finditer(r'@(Get|Post|Put|Delete)Mapping\("(/[^"]+)"\)', src):
            endpoints.add(f"{mm.group(1)} {base}{mm.group(2)}".rstrip("/"))
    return endpoints


def collect_frontend_endpoints() -> set[str]:
    """Match `httpVerb<...>(`${API_*_BASE}/path`...)` across src/api."""
    out = subprocess.check_output(
        [
            "grep",
            "-rhE",
            "http(Get|Post|Put|Delete)",
            str(FRONTEND_ROOT / "src" / "api"),
        ],
        text=True,
    )
    pattern = re.compile(
        r"http(Get|Post|Put|Delete)(?:<[^(]*>)?\(`(\$\{(API_\w+_BASE)\})([^`]+)`"
    )
    seen: set[str] = set()
    for line in out.splitlines():
        m = pattern.search(line)
        if not m:
            continue
        verb, path = m.group(1), m.group(4).split("?")[0].rstrip("/")
        seen.add(f"{verb} {path}")
    return seen


def main() -> int:
    if not BACKEND_ROOT.exists() or not FRONTEND_ROOT.exists():
        print(
            f"Expected backend at {BACKEND_ROOT} and frontend at {FRONTEND_ROOT}.",
            file=sys.stderr,
        )
        return 1

    prefix_map = load_url_prefix_constants()
    backend = collect_backend_endpoints(prefix_map)
    frontend = collect_frontend_endpoints()

    matched = frontend & backend
    fe_only = frontend - backend
    be_only = backend - frontend

    print(f"Backend endpoints          : {len(backend)}")
    print(f"Frontend HTTP call sites   : {len(frontend)}")
    print(f"Both (matched)             : {len(matched)}")
    print(f"Frontend-only (stale)      : {len(fe_only)}")
    print(f"Backend-only (no FE caller): {len(be_only)}")

    if fe_only:
        print("\n=== Frontend-only paths (stale or typo) ===")
        for p in sorted(fe_only):
            print(f"  {p}")

    if be_only:
        print("\n=== Backend-only paths (no frontend coverage) ===")
        for p in sorted(be_only):
            print(f"  {p}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
