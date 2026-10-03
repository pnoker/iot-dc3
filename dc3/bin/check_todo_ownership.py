#!/usr/bin/env python3
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
"""TODO ownership gate.

Unowned TODO/FIXME/XXX/HACK markers silently rot: the 2026-09-29 driver audit
found 13 of them, several marking dead protocol paths. This gate keeps markers
out of main sources unless they carry a tracker reference. Design decisions
belong in comments or READMEs as plain prose, never as TODO markers.

Rules:
- src/main sources (Java) plus dc3-web/src and dc3-cli/src must not contain
  bare TODO/FIXME/XXX/HACK markers.
- A marker is owned when written as ``TODO(<ref>):`` where <ref> is an issue
  number, issue slug, or commit hash of at least three characters. Replace the
  slug with the real issue number once the issue is filed.
- Vendored third-party trees (org/openscada under dc3-driver-opc-da) are
  exempt, as are the explicit inline allowlist entries below.
"""

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

SCAN_ROOTS = [
    "dc3-common",
    "dc3-center",
    "dc3-db",
    "dc3-gateway",
    "dc3-driver",
    "dc3-mq",
    "dc3-tsdb",
    "dc3-e2e",
    "dc3-api",
    "dc3-web/src",
    "dc3-cli/src",
]

SCAN_SUFFIXES = {".java", ".ts", ".vue"}

MARKER_PATTERN = re.compile(r"\b(TODO|FIXME|XXX|HACK)\b")
OWNED_PATTERN = re.compile(r"^\(\s*([A-Za-z0-9][A-Za-z0-9._/#-]{2,})\s*\)")

# (path-suffix, marker, reason) pairs that are consciously allowed.
ALLOWLIST = []

# Vendored third-party source trees; their markers belong to the upstream project.
EXCLUDED_PATH_FRAGMENTS = (
    "/org/openscada/",
    "\\org\\openscada\\",
)


def find_violations():
    violations = []
    scanned = 0
    for root in SCAN_ROOTS:
        base = REPO_ROOT / root
        if not base.exists():
            continue
        for path in sorted(base.rglob("*")):
            if path.suffix not in SCAN_SUFFIXES:
                continue
            if "target" in path.parts or "node_modules" in path.parts:
                continue
            relative = path.relative_to(REPO_ROOT).as_posix()
            if any(fragment in relative for fragment in EXCLUDED_PATH_FRAGMENTS):
                continue
            scanned += 1
            try:
                lines = path.read_text(encoding="utf-8").splitlines()
            except (OSError, UnicodeDecodeError):
                continue
            for number, line in enumerate(lines, start=1):
                for match in MARKER_PATTERN.finditer(line):
                    marker = match.group(1)
                    if _is_allowlisted(relative, marker):
                        continue
                    remainder = line[match.end():].lstrip()
                    owned = OWNED_PATTERN.match(remainder)
                    if not owned:
                        violations.append((relative, number, marker, line.strip()))
                    break
    return scanned, violations


def _is_allowlisted(relative, marker):
    for suffix, allowed_marker, _ in ALLOWLIST:
        if relative.endswith(suffix) and marker == allowed_marker:
            return True
    return False


def main() -> int:
    scanned, violations = find_violations()
    if violations:
        print(f"TODO ownership check failed: {len(violations)} unowned marker(s) in {scanned} files", file=sys.stderr)
        for relative, number, marker, line in violations:
            print(f"  {relative}:{number}: {marker} — {line}", file=sys.stderr)
        print(
            "Own each marker as TODO(<issue>): … (issue number, slug, or commit hash) or restate "
            "it as prose; design decisions belong in comments/READMEs, not TODO markers.",
            file=sys.stderr,
        )
        return 1
    allowlisted = ", ".join(f"{suffix}#{marker}: {reason}" for suffix, marker, reason in ALLOWLIST)
    print(
        "TODO ownership check passed: "
        f"rule=TODO/FIXME/XXX/HACK markers in main sources require a tracker reference, "
        f"files={scanned}, unowned=0"
        + (f", allowlisted: {allowlisted}" if allowlisted else "")
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
