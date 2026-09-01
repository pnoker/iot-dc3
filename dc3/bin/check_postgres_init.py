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

from __future__ import annotations

import re
import sys
from dataclasses import dataclass
from pathlib import Path


CREATE_TABLE_RE = re.compile(
    r"^CREATE\s+TABLE(?:\s+IF\s+NOT\s+EXISTS)?\s+(?P<table>[a-z][a-z0-9_]*)\s*$",
    re.IGNORECASE,
)
COMMENT_TABLE_RE = re.compile(
    r"COMMENT\s+ON\s+TABLE\s+(?P<table>[a-z][a-z0-9_]*)\s+IS\s+",
    re.IGNORECASE,
)
COMMENT_COLUMN_RE = re.compile(
    r"COMMENT\s+ON\s+COLUMN\s+(?P<table>[a-z][a-z0-9_]*)\.(?P<column>[a-z][a-z0-9_]*)\s+IS\s+",
    re.IGNORECASE,
)
COLUMN_RE = re.compile(r"^(?P<column>[a-z][a-z0-9_]*)\s+", re.IGNORECASE)
TABLE_NAME_RE = re.compile(
    r'@TableName\s*\(\s*(?:value\s*=\s*)?"(?P<table>[a-z][a-z0-9_]*)"',
    re.IGNORECASE,
)
MAPPED_COLUMN_RE = re.compile(
    r'@(?:TableField|TableId)\s*\(\s*(?:value\s*=\s*)?"(?P<column>[a-z][a-z0-9_]*)"',
    re.IGNORECASE,
)
NON_COLUMN_PREFIXES = {"check", "constraint", "exclude", "foreign", "primary", "references", "unique"}
LICENSE_MARKER = "Copyright 2016-present the IoT DC3 original author or authors."
MODULE_SQL_PREFIXES = {
    "dc3-common-agentic": ("06-",),
    "dc3-common-auth": ("02-",),
    "dc3-common-data": ("03-", "05-"),
    "dc3-common-manager": ("04-",),
}


@dataclass(frozen=True)
class Table:
    name: str
    line: int
    columns: tuple[str, ...]


def collect_tables(text: str) -> list[Table]:
    lines = text.splitlines()
    tables: list[Table] = []

    for index, line in enumerate(lines):
        match = CREATE_TABLE_RE.match(line.strip())
        if match is None or index + 1 >= len(lines) or lines[index + 1].strip() != "(":
            continue

        columns: list[str] = []
        for body_line in lines[index + 2:]:
            stripped = body_line.split("--", maxsplit=1)[0].strip().rstrip(",")
            if stripped == ");":
                break
            column_match = COLUMN_RE.match(stripped)
            if column_match is None:
                continue
            column = column_match.group("column").lower()
            if column not in NON_COLUMN_PREFIXES:
                columns.append(column)
        else:
            raise ValueError(f"unterminated CREATE TABLE at line {index + 1}")

        tables.append(Table(match.group("table").lower(), index + 1, tuple(columns)))

    return tables


def validate_file(path: Path) -> tuple[list[str], int, int]:
    text = path.read_text(encoding="utf-8")
    normalized = " ".join(text.split())
    tables = collect_tables(text)
    table_comments = {match.group("table").lower() for match in COMMENT_TABLE_RE.finditer(normalized)}
    column_comments = {
        (match.group("table").lower(), match.group("column").lower())
        for match in COMMENT_COLUMN_RE.finditer(normalized)
    }
    errors: list[str] = []

    if text.count(LICENSE_MARKER) != 1:
        errors.append(f"{path.name}: expected exactly one license header")
    if re.search(r"=\s+>", text):
        errors.append(f"{path.name}: invalid named-argument token '= >'; use '=>'")
    for table in tables:
        label = f"{path.name}:{table.line} {table.name}"
        if table.name not in table_comments:
            errors.append(f"{label}: missing COMMENT ON TABLE")
        for column in table.columns:
            if (table.name, column) not in column_comments:
                errors.append(f"{label}.{column}: missing COMMENT ON COLUMN")

    return errors, len(tables), sum(len(table.columns) for table in tables)


def validate_entities(repo_root: Path, tables_by_file: dict[str, list[Table]]) -> tuple[list[str], int]:
    errors: list[str] = []
    entity_count = 0

    for path in sorted(repo_root.glob("dc3-common/**/src/main/java/**/*.java")):
        text = path.read_text(encoding="utf-8")
        table_match = TABLE_NAME_RE.search(text)
        if table_match is None:
            continue

        module = next((part for part in path.parts if part in MODULE_SQL_PREFIXES), None)
        if module is None:
            continue
        entity_count += 1
        table_name = table_match.group("table").lower()
        prefixes = MODULE_SQL_PREFIXES[module]
        candidates = [
            table
            for file_name, tables in tables_by_file.items()
            if file_name.startswith(prefixes)
            for table in tables
            if table.name == table_name
        ]
        label = str(path.relative_to(repo_root))
        if not candidates:
            errors.append(f"{label}: @TableName references missing initialization table {table_name}")
            continue
        if len(candidates) > 1:
            errors.append(f"{label}: initialization table {table_name} is ambiguous within module schema")
            continue

        sql_columns = set(candidates[0].columns)
        mapped_columns = {match.group("column").lower() for match in MAPPED_COLUMN_RE.finditer(text)}
        missing_in_sql = sorted(mapped_columns - sql_columns)
        missing_in_entity = sorted(sql_columns - mapped_columns)
        if missing_in_sql:
            errors.append(f"{label}: mapped columns absent from {table_name}: {', '.join(missing_in_sql)}")
        if missing_in_entity:
            errors.append(f"{label}: initialization columns absent from entity: {', '.join(missing_in_entity)}")

    return errors, entity_count


def main() -> int:
    repo_root = Path(__file__).resolve().parents[2]
    init_dir = repo_root / "dc3" / "dependencies" / "postgres" / "initdb"
    sql_files = sorted(init_dir.glob("*.sql"))
    errors: list[str] = []
    table_count = 0
    column_count = 0
    tables_by_file: dict[str, list[Table]] = {}

    for path in sql_files:
        try:
            file_errors, file_tables, file_columns = validate_file(path)
            tables_by_file[path.name] = collect_tables(path.read_text(encoding="utf-8"))
        except ValueError as error:
            file_errors, file_tables, file_columns = [f"{path.name}: {error}"], 0, 0
        errors.extend(file_errors)
        table_count += file_tables
        column_count += file_columns

    entity_errors, entity_count = validate_entities(repo_root, tables_by_file)
    errors.extend(entity_errors)

    if errors:
        print("PostgreSQL initialization policy check failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        "PostgreSQL initialization policy check passed: "
        f"files={len(sql_files)}, tables={table_count}, columns={column_count}, entities={entity_count}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
