#!/usr/bin/env python3
"""Canonical schema fingerprint authority.

The canonical fingerprint is computed from the clean initdb SQL with the
embedded hash masked out. The embedded ``ddl_hash`` in
``08-iot-dc3-runtime.sql`` and every distributed default (env files and
compose files) must carry exactly that value. ``--check`` fails on any drift;
``--sync`` rewrites the embedded hash and all distribution points so the
initdb directory stays the single source of truth.
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

import argparse
import hashlib
import re
from pathlib import Path


HASH_PATTERN = re.compile(r"""(VALUES\s*\(2,\s*')[0-9a-fA-F]{64}(')""")

# Distributed copies of the fingerprint. Every entry must track the canonical
# value; add new distribution points here instead of inventing ad hoc copies.
# Every pattern captures (prefix)(hash)(suffix) so sync and check share one shape.
CONFIG_TARGETS: list[tuple[str, re.Pattern[str]]] = [
    ("dc3/env/dev.env", re.compile(r"(DC3_SCHEMA_FINGERPRINT=)([0-9a-fA-F]{64})()")),
    ("dc3/env/dev.env.sh", re.compile(r"(DC3_SCHEMA_FINGERPRINT=)([0-9a-fA-F]{64})()")),
    (
        "dc3/docker-compose.yml",
        re.compile(r'(DC3_SCHEMA_FINGERPRINT:\s*"\$\{DC3_SCHEMA_FINGERPRINT:-)([0-9a-fA-F]{64})(\}")'),
    ),
    (
        "dc3/docker-compose-dev.yml",
        re.compile(r'(DC3_SCHEMA_FINGERPRINT:\s*"\$\{DC3_SCHEMA_FINGERPRINT:-)([0-9a-fA-F]{64})(\}")'),
    ),
]


def canonical_hash(initdb: Path) -> str:
    digest = hashlib.sha256()
    for sql_file in sorted(initdb.glob("*.sql")):
        content = HASH_PATTERN.sub(r"\1<DDL_HASH>\2", sql_file.read_text(encoding="utf-8"))
        digest.update(sql_file.name.encode())
        digest.update(b"\0")
        digest.update(content.encode())
        digest.update(b"\0")
    return digest.hexdigest()


def embedded_hash(initdb: Path) -> str | None:
    runtime_file = initdb / "08-iot-dc3-runtime.sql"
    match = HASH_PATTERN.search(runtime_file.read_text(encoding="utf-8"))
    return match.group(0).split("'")[1] if match else None


def distributed_hash(repository: Path, rel_path: str, pattern: re.Pattern[str]) -> str | None:
    text = (repository / rel_path).read_text(encoding="utf-8")
    match = pattern.search(text)
    return match.group(2) if match else None


def sync_file(path: Path, pattern: re.Pattern[str], replacement: str) -> None:
    text = path.read_text(encoding="utf-8")
    updated, count = pattern.subn(replacement, text)
    if count == 0:
        raise SystemExit(f"no fingerprint assignment found in {path}; refusing to sync")
    path.write_text(updated, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Calculate canonical initdb schema fingerprints")
    parser.add_argument("--check", action="store_true", help="fail when any embedded or distributed fingerprint is stale")
    parser.add_argument("--sync", action="store_true", help="rewrite embedded and distributed fingerprints to the canonical value")
    args = parser.parse_args()
    if args.check and args.sync:
        parser.error("--check and --sync are mutually exclusive")

    repository = Path(__file__).resolve().parents[2]
    initdb = repository / "dc3" / "dependencies" / "postgres" / "initdb"
    actual = canonical_hash(initdb)
    print(f"postgres={actual}")

    if args.sync:
        sync_file(initdb / "08-iot-dc3-runtime.sql", HASH_PATTERN, rf"\g<1>{actual}\g<2>")
        for rel_path, pattern in CONFIG_TARGETS:
            sync_file(repository / rel_path, pattern, rf"\g<1>{actual}\g<3>")
        print(f"synchronized embedded ddl_hash and {len(CONFIG_TARGETS)} distribution points")
        return 0

    if not args.check:
        return 0

    failed = False
    embedded = embedded_hash(initdb)
    if embedded != actual:
        print(f"stale embedded ddl_hash in 08-iot-dc3-runtime.sql: {embedded}")
        failed = True
    for rel_path, pattern in CONFIG_TARGETS:
        distributed = distributed_hash(repository, rel_path, pattern)
        if distributed != actual:
            print(f"stale fingerprint default in {rel_path}: {distributed}")
            failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
