#!/usr/bin/env python3
"""Fail-fast static gate for the R2DBC flag-day migration.

The gate intentionally fails while legacy relation persistence remains.  Driver
and SQL modules are the only JDBC allow-list; every other platform module must
be free of MyBatis, JDBC relation APIs, legacy pagination/envelopes and blocking
bridges before the flag-day build can be published.
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
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE_ROOTS = [
    ROOT / "dc3-common",
    ROOT / "dc3-center",
    ROOT / "dc3-db",
    ROOT / "dc3-gateway",
    ROOT / "dc3-api",
    ROOT / "dc3-tsdb",
    ROOT / "dc3-cli" / "src",
    ROOT / "dc3-web" / "src",
]
# Scan source trees once; Maven descriptors are handled separately below so a
# parent POM is never counted twice (which would make the gate trend noisy).
POM_ROOTS = [ROOT]
ALLOWLIST = ("dc3-common-sql", "dc3-driver")
EXTERNAL_JDBC_BOUNDARIES = {
    ROOT / "dc3-common" / "dc3-common-driver" / "src" / "main" / "java"
    / "io" / "github" / "pnoker" / "common" / "driver" / "buffer" / "PointValueBuffer.java"
}

PATTERNS: dict[str, tuple[str, ...]] = {
    "mybatis imports": (r"com\.baomidou", r"org\.mybatis", r"@MapperScan"),
    "jdbc relation APIs": (
        r"spring-boot-starter-jdbc",
        r"spring-jdbc",
        r"JdbcTemplate",
        r"DataSourceTransactionManager",
        r"javax\.sql\.DataSource",
        r"dynamic-datasource",
        r"jdbc:",
    ),
    "legacy pagination": (
        r"mybatisplus\.extension\.plugins\.pagination\.Page",
        r"common\.entity\.common\.Pages",
        r"\bFacadePage\b",
        r"\bGrpcPage\b",
    ),
    "legacy response envelopes": (
        r"common\.entity\.R",
        r"\bGrpcR[A-Za-z]",
        r"\bR<",
        r"response\.data\.ok",
        r"\.data\.records",
    ),
    "blocking bridges": (r"\.block\s*\(", r"Schedulers\.boundedElastic\s*\("),
}


IGNORED_PARTS = {"target", "node_modules", "dist", ".git"}


def files(root: Path, suffix: str) -> list[Path]:
    if root.is_file():
        return [root] if root.name.endswith(suffix) else []
    if not root.exists():
        return []
    return sorted(
        path for path in root.rglob(f"*{suffix}")
        if not any(part in IGNORED_PARTS for part in path.parts)
    )


def is_allowlisted(path: Path) -> bool:
    return any(part == entry or part.startswith(entry + "-") for part in path.parts for entry in ALLOWLIST)


def is_external_jdbc_boundary(path: Path, category: str) -> bool:
    return category == "jdbc relation APIs" and path in EXTERNAL_JDBC_BOUNDARIES


def is_expected_startup_block(path: Path, category: str, match: re.Match[str]) -> bool:
    return (
        category == "blocking bridges"
        and path.name == "SchemaFingerprintStartupValidator.java"
        and match.group(0).startswith(".block")
    )


def offset_page_method_bodies(text: str) -> list[str]:
    bodies: list[str] = []
    for match in re.finditer(r"Mono<OffsetPage", text):
        opening = text.find("{", match.end())
        if opening < 0:
            continue
        depth = 0
        for index in range(opening, len(text)):
            if text[index] == "{":
                depth += 1
            elif text[index] == "}":
                depth -= 1
                if depth == 0:
                    bodies.append(text[opening:index + 1])
                    break
    return bodies


def scan_offset_pagination() -> list[str]:
    errors: list[str] = []
    roots = [
        ROOT / "dc3-common" / "dc3-common-auth" / "src" / "main" / "java",
        ROOT / "dc3-common" / "dc3-common-manager" / "src" / "main" / "java",
        ROOT / "dc3-common" / "dc3-common-data" / "src" / "main" / "java",
        ROOT / "dc3-common" / "dc3-common-agentic" / "src" / "main" / "java",
    ]
    for root in roots:
        for path in files(root, ".java"):
            if not path.name.startswith("R2dbc") or not path.name.endswith("Store.java"):
                continue
            text = path.read_text(encoding="utf-8", errors="replace")
            methods = offset_page_method_bodies(text)
            if not methods:
                continue
            relative = path.relative_to(ROOT)
            if "PageTransaction" not in text or ".as(pageTransaction::transactional)" not in text:
                errors.append(f"{relative}: unsafe offset pagination: missing PageTransaction snapshot")
            for method in methods:
                if "zipWith(" in method or "Mono.zip(" in method:
                    line = text.count("\n", 0, text.find(method)) + 1
                    errors.append(f"{relative}:{line}: unsafe offset pagination: concurrent count/items")
    return errors


def scan_non_string_jsonb_reads() -> list[str]:
    schema_root = ROOT / "dc3" / "dependencies" / "postgres" / "initdb"
    schema_files = files(schema_root, ".sql")
    columns: set[str] = set()
    for path in schema_files:
        text = path.read_text(encoding="utf-8", errors="replace")
        columns.update(re.findall(r"(?m)^\s*([a-z][a-z0-9_]*)\s+JSONB\b", text))
    if not columns:
        return ["PostgreSQL schema declares no JSONB columns"]

    column_pattern = "|".join(re.escape(column) for column in sorted(columns))
    non_string_read = re.compile(
        rf'\b[A-Za-z_$][A-Za-z0-9_$]*\.get\("(?:{column_pattern})"'
        rf'(?!\s*,\s*String\.class\s*\))'
    )
    errors: list[str] = []
    for root in (ROOT / "dc3-common", ROOT / "dc3-db"):
        for path in files(root, ".java"):
            if not path.name.startswith("R2dbc"):
                continue
            text = path.read_text(encoding="utf-8", errors="replace")
            for match in non_string_read.finditer(text):
                line = text.count("\n", 0, match.start()) + 1
                errors.append(
                    f"{path.relative_to(ROOT)}:{line}: non-string JSONB read: {match.group(0)}"
                )
    return errors


def scan() -> tuple[list[str], dict[str, int]]:
    errors: list[str] = []
    counts = {name: 0 for name in PATTERNS}
    for root in SOURCE_ROOTS:
        candidates: list[Path] = []
        for suffix in (".java", ".xml", ".yml", ".yaml", ".properties", ".ts", ".vue", ".proto"):
            candidates.extend(files(root, suffix))
        for path in sorted(set(candidates)):
            if is_allowlisted(path):
                continue
            text = path.read_text(encoding="utf-8", errors="replace")
            if path.name == "pom.xml":
                text = re.sub(r"<!--.*?-->", "", text, flags=re.DOTALL)
            for category, patterns in PATTERNS.items():
                if is_external_jdbc_boundary(path, category):
                    continue
                if category == "blocking bridges" and "src" in path.parts and "test" in path.parts:
                    continue
                for pattern in patterns:
                    matches = list(re.finditer(pattern, text))
                    matches = [match for match in matches if not is_expected_startup_block(path, category, match)]
                    counts[category] += len(matches)
                    for match in matches[:3]:
                        line = text.count("\n", 0, match.start()) + 1
                        errors.append(f"{path.relative_to(ROOT)}:{line}: {category}: {match.group(0)}")
    for root in POM_ROOTS:
        for path in files(root, "pom.xml"):
            if is_allowlisted(path):
                continue
            text = path.read_text(encoding="utf-8", errors="replace")
            text = re.sub(r"<!--.*?-->", "", text, flags=re.DOTALL)
            for category in ("jdbc relation APIs",):
                for pattern in PATTERNS[category]:
                    matches = list(re.finditer(pattern, text))
                    counts[category] += len(matches)
                    for match in matches[:3]:
                        line = text.count("\n", 0, match.start()) + 1
                        errors.append(f"{path.relative_to(ROOT)}:{line}: {category}: {match.group(0)}")
    pagination_errors = scan_offset_pagination()
    counts["unsafe offset pagination"] = len(pagination_errors)
    errors.extend(pagination_errors)
    jsonb_errors = scan_non_string_jsonb_reads()
    counts["non-string JSONB reads"] = len(jsonb_errors)
    errors.extend(jsonb_errors)
    return errors, counts


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report-only", action="store_true")
    args = parser.parse_args()
    errors, counts = scan()
    print("R2DBC migration gate:")
    for category, count in counts.items():
        print(f"  {category}: {count}")
    if errors:
        print("Blocking findings:")
        for error in errors[:120]:
            print(f"- {error}")
        if len(errors) > 120:
            print(f"- ... {len(errors) - 120} more findings")
    return 0 if args.report_only or not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
