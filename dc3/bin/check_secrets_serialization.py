#!/usr/bin/env python3
"""Fail-fast static gate for the "secrets never appear in serialization" rule.

RULE: every secret-named field in a ``*VO.java`` under ``dc3-common/**/src/main/java``
must be excluded from Jackson serialization (``@JsonProperty(access = WRITE_ONLY)``
or ``@JsonIgnore``, or a class-level ``@JsonIgnoreProperties`` naming it) and from
Lombok ``@ToString`` output (``@ToString.Exclude`` whenever the file declares
``@ToString`` anywhere).  ``@Schema(accessMode = WRITE_ONLY)`` is documentation
only and satisfies NEITHER requirement — that exact gap is the regression this
gate exists to catch (ModelProviderVO.apiKey shipped apiKey in responses until
2026-09-29 with @Schema WRITE_ONLY but no Jackson/ToString guard).
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

# A field is a SECRET FIELD when its name is exactly one of the tokens below
# (case-insensitive, snake/camel tolerant) or ends with one of the suffix
# tokens. Credential *metadata* fields (credentialType, credentialExt,
# credentialRef, credentialPolicyExt) are deliberately outside the rule: they
# describe a credential, they do not carry secret material.
SECRET_FIELD_EXACT = re.compile(r"(?i)^(api_?key|password|secret|token|credentials?|client_?secret)$")
SECRET_FIELD_SUFFIX = re.compile(r"(?i)(password|secret|apikey|token)$")

# Inline allowlist: (path suffix, field name, reason). Every entry is reported
# in the passing summary so the list cannot grow silently.
ALLOWLIST: tuple[tuple[str, str, str], ...] = (
    (
        "auth/entity/vo/OAuthClientRegistrationResponseVO.java",
        "clientSecret",
        "RFC 7591 dynamic client registration mandates the secret in the JSON response; @ToString.Exclude keeps it out of logs",
    ),
)

FIELD_RE = re.compile(
    r"(?m)^[ \t]*(?:private|protected|public)\s+(?:static\s+|final\s+|transient\s+)*"
    r"[A-Za-z_$][\w$.]*(?:\s*<[^>;=]*>)?\s*([A-Za-z_$][\w$]*)\s*(?:=[^;]*)?;"
)
JSON_WRITE_ONLY_RE = re.compile(r"@(?:\w+\.)*JsonProperty\s*\([^()]*\bWRITE_ONLY\b")
JSON_IGNORE_RE = re.compile(r"@(?:\w+\.)*JsonIgnore(?![\w])")
JSON_IGNORE_PROPERTIES_RE = re.compile(r"@(?:\w+\.)*JsonIgnoreProperties\s*\(([^()]*)\)")
TO_STRING_EXCLUDE_RE = re.compile(r"@ToString\s*\.\s*Exclude")

IGNORED_PARTS = {"target", "node_modules", "dist", ".git"}


def is_secret_field(name: str) -> bool:
    return bool(SECRET_FIELD_EXACT.match(name) or SECRET_FIELD_SUFFIX.search(name))


def annotation_regions(text: str) -> dict[int, str]:
    """Map each field-declaration start to its preceding annotation region.

    The region starts at the end of the previous field declaration (or the
    enclosing ``{`` for the first field) so semicolons inside annotation
    strings — e.g. ``example = "...secret; write-only..."`` — cannot cut an
    annotation block in half.
    """
    regions: dict[int, str] = {}
    previous_end = -1
    for match in FIELD_RE.finditer(text):
        boundary = max(
            previous_end,
            text.rfind("{", 0, match.start()) if previous_end < 0 else previous_end,
        )
        regions[match.start()] = text[boundary:match.start()]
        previous_end = match.end()
    return regions


def validate_file(path: Path) -> tuple[list[str], int, int]:
    text = path.read_text(encoding="utf-8", errors="replace")
    relative = path.relative_to(ROOT).as_posix()
    class_ignored_properties = "|".join(
        match.group(1) for match in JSON_IGNORE_PROPERTIES_RE.finditer(text)
    )
    has_to_string = "@ToString" in text
    regions = annotation_regions(text)
    errors: list[str] = []
    secret_fields = 0
    allowlisted = 0

    for match in FIELD_RE.finditer(text):
        field = match.group(1)
        if not is_secret_field(field):
            continue
        secret_fields += 1
        line = text.count("\n", 0, match.start(1)) + 1
        label = f"{relative}:{line} {field}"
        if any(relative.endswith(suffix) and field == name for suffix, name, _ in ALLOWLIST):
            allowlisted += 1
            continue
        region = regions[match.start()]
        json_guarded = bool(
            JSON_WRITE_ONLY_RE.search(region)
            or JSON_IGNORE_RE.search(region)
            or re.search(rf'[\'"]{re.escape(field)}[\'"]', class_ignored_properties)
        )
        if not json_guarded:
            errors.append(
                f"{label}: secret field is serialized — add "
                "@JsonProperty(access = WRITE_ONLY) or @JsonIgnore "
                "(@Schema WRITE_ONLY is documentation only)"
            )
        if has_to_string and not TO_STRING_EXCLUDE_RE.search(region):
            errors.append(f"{label}: @ToString is present but field lacks @ToString.Exclude")

    return errors, secret_fields, allowlisted


def main() -> int:
    root = ROOT
    common_root = root / "dc3-common"
    vo_files = sorted(
        path
        for path in common_root.rglob("*VO.java")
        if "src" in path.parts and "main" in path.parts
        and not any(part in IGNORED_PARTS for part in path.parts)
    )

    errors: list[str] = []
    secret_fields = 0
    allowlisted = 0
    for path in vo_files:
        file_errors, file_secrets, file_allowlisted = validate_file(path)
        errors.extend(file_errors)
        secret_fields += file_secrets
        allowlisted += file_allowlisted

    rule = "secret fields require Jackson WRITE_ONLY/@JsonIgnore + @ToString.Exclude"
    if errors:
        print(f"Secrets serialization check failed (rule: {rule}):", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        if allowlisted:
            print("Allowlisted (still enforced for future fields):", file=sys.stderr)
            for suffix, field, reason in ALLOWLIST:
                print(f"- */{suffix}#{field}: {reason}", file=sys.stderr)
        return 1

    print(
        "Secrets serialization check passed: "
        f"rule={rule}, files={len(vo_files)}, secret_fields={secret_fields}, allowlisted={allowlisted}"
    )
    for suffix, field, reason in ALLOWLIST:
        print(f"  allowlist: */{suffix}#{field}: {reason}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
