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

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path


MAPPING_RE = re.compile(r"@(GetMapping|PostMapping|PutMapping|DeleteMapping|RequestMapping)\b")
PRE_AUTHORIZE_RE = re.compile(r"@PreAuthorize\s*\((?P<expr>.*)\)")
PERM_CAN_RE = re.compile(r"@perm\.can\(\s*['\"]([a-z0-9_]+)['\"]\s*,\s*['\"]([a-z]+)['\"]\s*\)")
CLASS_RE = re.compile(r"\bclass\s+([A-Za-z_][A-Za-z0-9_]*)\b")
METHOD_RE = re.compile(
    r"\b(public|protected|private)\s+"
    r"(?:static\s+)?(?:final\s+)?(?:<[^>]+>\s+)?"
    r"[\w<>\[\],.? extends super]+\s+"
    r"(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*\("
)

VALID_SCOPES = {"add", "delete", "update", "get", "list"}

# Kept public by product decision. Token pre-login methods are covered by
# @PublicEndpoint and do not need to be listed here.
EXPLICIT_PUBLIC_METHODS = {
    ("ServiceMcpToolsController", "getMcpTools"),
}


@dataclass(frozen=True)
class Annotation:
    line: int
    text: str


@dataclass(frozen=True)
class Endpoint:
    path: Path
    line: int
    class_name: str
    method_name: str
    annotations: list[Annotation]
    class_annotations: list[Annotation]


def parse_args() -> argparse.Namespace:
    repo_root = Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(
        description="Audit controller authorization annotations and resource-registration closure."
    )
    parser.add_argument("--root", type=Path, default=repo_root)
    return parser.parse_args()


def java_files(root: Path) -> list[Path]:
    return sorted(
        path for path in root.glob("**/src/main/java/**/*Controller.java")
        if "target" not in path.parts
    )


def join_annotation(lines: list[str]) -> str:
    return " ".join(line.strip() for line in lines)


def collect_endpoints(path: Path) -> list[Endpoint]:
    lines = path.read_text(encoding="utf-8").splitlines()
    endpoints: list[Endpoint] = []
    pending: list[Annotation] = []
    current_annotation: list[str] | None = None
    current_annotation_line = 0
    paren_balance = 0
    class_name = path.stem
    class_annotations: list[Annotation] = []

    for line_no, line in enumerate(lines, start=1):
        stripped = line.strip()

        if current_annotation is not None:
            current_annotation.append(stripped)
            paren_balance += stripped.count("(") - stripped.count(")")
            if paren_balance <= 0:
                pending.append(Annotation(current_annotation_line, join_annotation(current_annotation)))
                current_annotation = None
            continue

        if stripped.startswith("@"):
            current_annotation = [stripped]
            current_annotation_line = line_no
            paren_balance = stripped.count("(") - stripped.count(")")
            if paren_balance <= 0:
                pending.append(Annotation(current_annotation_line, join_annotation(current_annotation)))
                current_annotation = None
            continue

        if not stripped or stripped.startswith("//") or stripped.startswith("*") or stripped.startswith("/*"):
            continue

        class_match = CLASS_RE.search(stripped)
        if class_match:
            class_name = class_match.group(1)
            class_annotations = pending
            pending = []
            continue

        method_match = METHOD_RE.search(stripped)
        if method_match:
            if any(MAPPING_RE.search(annotation.text) for annotation in pending):
                endpoints.append(Endpoint(
                    path=path,
                    line=line_no,
                    class_name=class_name,
                    method_name=method_match.group("name"),
                    annotations=pending,
                    class_annotations=class_annotations,
                ))
            pending = []
            continue

        pending = []

    return endpoints


def annotation_texts(endpoint: Endpoint) -> list[str]:
    return [annotation.text for annotation in endpoint.annotations]


def class_annotation_texts(endpoint: Endpoint) -> list[str]:
    return [annotation.text for annotation in endpoint.class_annotations]


def is_public(endpoint: Endpoint) -> bool:
    if (endpoint.class_name, endpoint.method_name) in EXPLICIT_PUBLIC_METHODS:
        return True
    texts = annotation_texts(endpoint) + class_annotation_texts(endpoint)
    return any("@PublicEndpoint" in text for text in texts)


def pre_authorize_expr(endpoint: Endpoint) -> str | None:
    for annotation in endpoint.annotations + endpoint.class_annotations:
        match = PRE_AUTHORIZE_RE.search(annotation.text)
        if match:
            return match.group("expr")
    return None


def validate_endpoint(endpoint: Endpoint, root: Path) -> list[str]:
    if is_public(endpoint):
        return []

    label = f"{endpoint.path.relative_to(root)}:{endpoint.line} {endpoint.class_name}#{endpoint.method_name}"
    expr = pre_authorize_expr(endpoint)
    if expr is None:
        return [f"{label}: missing @PreAuthorize or @PublicEndpoint"]

    match = PERM_CAN_RE.search(expr)
    if not match:
        return [f"{label}: @PreAuthorize must use @perm.can('domain', 'scope') so resource registration can mirror it"]

    domain, scope = match.groups()
    errors: list[str] = []
    if scope not in VALID_SCOPES:
        errors.append(f"{label}: unsupported permission scope '{scope}'")
    if not domain:
        errors.append(f"{label}: permission domain is blank")
    return errors


def main() -> int:
    args = parse_args()
    root = args.root.resolve()
    endpoints = [endpoint for path in java_files(root) for endpoint in collect_endpoints(path)]
    errors = [error for endpoint in endpoints for error in validate_endpoint(endpoint, root)]

    if errors:
        print("Controller permission audit failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        print("", file=sys.stderr)
        print(
            "Each controller mapping must be guarded by @PreAuthorize(\"@perm.can('domain', 'scope')\") "
            "or explicitly marked @PublicEndpoint.",
            file=sys.stderr,
        )
        return 1

    guarded = len([endpoint for endpoint in endpoints if not is_public(endpoint)])
    public = len(endpoints) - guarded
    print(f"Controller permission audit passed: endpoints={len(endpoints)}, guarded={guarded}, public={public}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
