#!/usr/bin/env python3
"""Fail-fast static gate for Docker Compose interpolation hygiene.

RULES, applied to ``dc3/docker-compose*.yml`` against ``.env.example``:

1. every ``${VAR}`` / ``${VAR:-default}`` / ``${VAR:?msg}`` interpolated
   variable must exist as a KEY in ``.env.example`` (compose-internal
   variables in the allowlist below are exempt);
2. no literal asterisk-run default (``*********``) may appear in any compose
   file — placeholder "passwords" were removed and must stay out;
3. a variable must not carry more than one distinct ``${VAR:-default}``
   across the compose files (intentional per-stack differences are
   allowlisted below).

``$${VAR}`` compose escapes are ignored; defaults may contain spaces.
"""

# Copyright 2016-present the IoT DC3 original author or authors.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program. If not, see <https://www.gnu.org/licenses/>.

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
COMPOSE_DIR = ROOT / "dc3"
ENV_EXAMPLE = ROOT / ".env.example"

INTERPOLATION_RE = re.compile(r"\$\{(\w+)(?::[-?][^}]*)?\}")
DEFAULT_RE = re.compile(r"\$\{(\w+):-([^}]*)\}")
ASTERISK_RUN_RE = re.compile(r"\*{3,}")

# Rule 1 exemptions: (variable, reason). Every entry is echoed in the passing
# summary so the list cannot grow silently.
MISSING_KEY_ALLOWLIST: dict[str, str] = {
    "DC3_SECURITY_KEY": (
        "secret, intentionally absent from .env.example (.env.example header documents it); "
        "production stacks use the :? required form and reject the public repository default"
    ),
    "AUTH_HMAC_SECRET": (
        "secret, intentionally absent from .env.example (.env.example header documents it); "
        "production stacks use the :? required form and reject the public repository default"
    ),
    "DC3_R2DBC_URL": "compose-internal container-network wiring default, identical in every stack",
    "DC3_SCHEMA_FINGERPRINT": "schema-guard constant shipped with the compose stacks, not a local-run knob",
    "DC3_SCHEMA_CONTRACT": "schema-guard constant shipped with the compose stacks, not a local-run knob",
}

# Rule 3 exemptions: (variable, reason).
DRIFT_ALLOWLIST: dict[str, str] = {
    "DC3_GATEWAY_OAUTH_JWKS_URL": (
        "intentional per-stack split: split stacks point at dc3-center-auth, "
        "the single stack at dc3-center-single"
    ),
}


def env_example_keys() -> set[str]:
    keys: set[str] = set()
    for line in ENV_EXAMPLE.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if stripped and not stripped.startswith("#") and "=" in stripped:
            keys.add(stripped.split("=", 1)[0].strip())
    return keys


def line_of(text: str, index: int) -> int:
    return text.count("\n", 0, index) + 1


def main() -> int:
    compose_files = sorted(COMPOSE_DIR.glob("docker-compose*.yml"))
    if not compose_files:
        print("Compose variable check failed: no docker-compose*.yml files found", file=sys.stderr)
        return 1
    env_keys = env_example_keys()

    errors: list[str] = []
    interpolated: set[str] = set()
    defaults: dict[str, set[str]] = {}
    asterisk_hits = 0
    for path in compose_files:
        relative = path.relative_to(ROOT)
        # Compose escapes "$$" as a literal dollar; neutralize the pairs before
        # scanning so "$${NOT_A_VAR}" is not treated as interpolation.
        text = path.read_text(encoding="utf-8").replace("$$", "\x00")
        for match in INTERPOLATION_RE.finditer(text):
            interpolated.add(match.group(1))
        for match in DEFAULT_RE.finditer(text):
            defaults.setdefault(match.group(1), set()).add(match.group(2))
        for match in ASTERISK_RUN_RE.finditer(text):
            asterisk_hits += 1
            errors.append(f"{relative}:{line_of(text, match.start())}: asterisk-run placeholder default")

    missing = sorted(variable for variable in interpolated if variable not in env_keys)
    unlisted_missing = [variable for variable in missing if variable not in MISSING_KEY_ALLOWLIST]
    for variable in missing:
        listed = variable in MISSING_KEY_ALLOWLIST
        if not listed:
            errors.append(f"{variable}: interpolated but absent from .env.example and not allowlisted")

    drifted = {
        variable: sorted(values)
        for variable, values in defaults.items()
        if len(values) > 1 and variable not in DRIFT_ALLOWLIST
    }
    for variable, values in drifted.items():
        errors.append(f"{variable}: conflicting defaults across compose files: {values}")

    if errors:
        print("Compose variable check failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        "Compose variable check passed: "
        f"files={len(compose_files)}, variables={len(interpolated)}, "
        f"allowlisted_keys={sum(1 for v in missing)}, drift_allowlisted="
        f"{sum(1 for v in defaults if len(defaults[v]) > 1)}, asterisk_runs=0"
    )
    for variable in missing:
        print(f"  missing-key allowlist: {variable}: {MISSING_KEY_ALLOWLIST[variable]}")
    for variable, values in sorted(defaults.items()):
        if len(values) > 1:
            print(f"  drift allowlist: {variable}: {values} — {DRIFT_ALLOWLIST[variable]}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
