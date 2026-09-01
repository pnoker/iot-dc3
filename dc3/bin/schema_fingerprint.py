#!/usr/bin/env python3

from __future__ import annotations

import argparse
import hashlib
import re
from pathlib import Path


HASH_PATTERN = re.compile(r"""(VALUES\s*\(2,\s*')[0-9a-fA-F]{64}(')""")


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


def main() -> int:
    parser = argparse.ArgumentParser(description="Calculate canonical initdb schema fingerprints")
    parser.add_argument("--check", action="store_true", help="fail when an embedded fingerprint is stale")
    args = parser.parse_args()

    repository = Path(__file__).resolve().parents[2]
    failed = False
    initdb = repository / "dc3" / "dependencies" / "postgres" / "initdb"
    actual = canonical_hash(initdb)
    print(f"postgres={actual}")
    if args.check and embedded_hash(initdb) != actual:
        failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
