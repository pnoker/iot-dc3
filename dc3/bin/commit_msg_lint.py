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
from pathlib import Path


ALLOWED_TYPES = {
    "feat",
    "fix",
    "perf",
    "refactor",
    "docs",
    "build",
    "ci",
    "test",
    "chore",
    "style",
    "security",
    "revert",
}

WEAK_DESCRIPTIONS = {
    ".",
    "add",
    "add comment",
    "change",
    "change code",
    "changes",
    "fix",
    "fix bug",
    "misc",
    "optimize",
    "update",
    "update code",
    "wip",
}

SUBJECT_RE = re.compile(
    r"^(?P<type>[a-z]+)"
    r"(?:\((?P<scope>[a-z0-9][a-z0-9._/-]*(?:,[a-z0-9][a-z0-9._/-]*)*)\))?"
    r"(?P<breaking>!)?: (?P<description>.+)$"
)
CJK_RE = re.compile(r"[\u4e00-\u9fff]")
FIXUP_RE = re.compile(r"^(fixup|squash)! +", re.IGNORECASE)
GENERATED_CHANGELOG_SUBJECT = "docs(release): update generated changelog"
MAX_SUBJECT_LENGTH = 100
MIN_DESCRIPTION_LENGTH = 10


def read_subject(message_file: Path) -> tuple[str, str]:
    text = message_file.read_text(encoding="utf-8")
    lines = [line.rstrip() for line in text.splitlines()]
    content_lines = [line for line in lines if line.strip() and not line.lstrip().startswith("#")]
    subject = content_lines[0] if content_lines else ""
    body = "\n".join(content_lines[1:])
    while FIXUP_RE.match(subject):
        subject = FIXUP_RE.sub("", subject, count=1)
    return subject, body


def is_git_generated_subject(subject: str) -> bool:
    return subject.startswith("Merge ") or subject.startswith("Revert ")


def normalized_description(description: str) -> str:
    return re.sub(r"[\s.。!！?？_-]+", " ", description.strip().lower()).strip()


def validate_subject(subject: str, body: str) -> list[str]:
    errors: list[str] = []

    if not subject:
        return ["Commit subject is required."]
    if is_git_generated_subject(subject):
        return []
    if subject == GENERATED_CHANGELOG_SUBJECT:
        return []
    if len(subject) > MAX_SUBJECT_LENGTH:
        errors.append(f"Commit subject must be at most {MAX_SUBJECT_LENGTH} characters.")
    if CJK_RE.search(subject):
        errors.append("Commit subject must be written in English.")

    match = SUBJECT_RE.match(subject)
    if not match:
        errors.append("Commit subject must match '<type>(optional-scope): <english summary>'.")
        return errors

    commit_type = match.group("type")
    description = match.group("description").strip()
    breaking = bool(match.group("breaking"))

    if commit_type not in ALLOWED_TYPES:
        allowed = ", ".join(sorted(ALLOWED_TYPES))
        errors.append(f"Unsupported commit type '{commit_type}'. Allowed types: {allowed}.")
    if len(description) < MIN_DESCRIPTION_LENGTH:
        errors.append(f"Commit description must be at least {MIN_DESCRIPTION_LENGTH} characters.")
    if normalized_description(description) in WEAK_DESCRIPTIONS:
        errors.append("Commit description is too vague for generated release notes.")
    if description[0].islower() is False and not description[0].isdigit():
        errors.append("Commit description should start with lowercase imperative text.")
    if breaking and "BREAKING CHANGE:" not in body:
        errors.append("Breaking-change commits using '!' must include a 'BREAKING CHANGE:' body entry.")

    return errors


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate IoT DC3 commit messages.")
    parser.add_argument("message_file", type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    subject, body = read_subject(args.message_file)
    errors = validate_subject(subject, body)
    if not errors:
        return 0

    print("Invalid commit message:", file=sys.stderr)
    print(f"  {subject or '<empty>'}", file=sys.stderr)
    for error in errors:
        print(f"- {error}", file=sys.stderr)
    print("", file=sys.stderr)
    print("Examples:", file=sys.stderr)
    print("  feat(agentic): add session cleanup policy", file=sys.stderr)
    print("  fix(manager): validate tenant scope for device queries", file=sys.stderr)
    print("  docs(release): update generated changelog", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())

