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
import datetime as dt
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path


HEADER = "# \u2728 What's Changed"
DETAILS_OPEN = "<details>"
DETAILS_SUMMARY = "<summary>\U0001f4dd Historical Version Description, Click to Expand</summary>"
DEFAULT_TAG_PATTERN = "dc3.release.*"
GENERATED_CHANGELOG_RE = re.compile(r"^(docs|chore)\(release\): update generated changelog$")

CATEGORY_ORDER = [
    "Breaking Changes",
    "Security",
    "Features",
    "Bug Fixes",
    "Performance",
    "Refactoring",
    "Documentation",
    "Build",
    "CI",
    "Tests",
    "Chores",
    "Other",
]

CATEGORY_TITLES = {
    "Breaking Changes": "Breaking Changes",
    "Security": "Security",
    "Features": "Features",
    "Bug Fixes": "Bug Fixes",
    "Performance": "Performance",
    "Refactoring": "Refactoring",
    "Documentation": "Documentation",
    "Build": "Build",
    "CI": "CI",
    "Tests": "Tests",
    "Chores": "Chores",
    "Other": "Other Changes",
}

TYPE_TO_CATEGORY = {
    "feat": "Features",
    "feature": "Features",
    "fix": "Bug Fixes",
    "perf": "Performance",
    "refactor": "Refactoring",
    "docs": "Documentation",
    "doc": "Documentation",
    "build": "Build",
    "ci": "CI",
    "test": "Tests",
    "tests": "Tests",
    "chore": "Chores",
    "style": "Chores",
    "revert": "Chores",
}

CONVENTIONAL_RE = re.compile(r"^(?P<type>[a-zA-Z]+)(?:\((?P<scope>[^)]+)\))?(?P<breaking>!)?: (?P<desc>.+)$")
VERSION_RE = re.compile(r"^### .*?(?P<version>\d+\.\d+\.\d+).*$", re.MULTILINE)


@dataclass(frozen=True)
class Commit:

    full_hash: str
    short_hash: str
    subject: str
    author: str
    type: str
    scope: str
    description: str
    breaking: bool
    category: str


def run_git(*args: str) -> str:
    try:
        return subprocess.check_output(["git", *args], text=True).strip()
    except subprocess.CalledProcessError as exc:
        print(exc.output, file=sys.stderr)
        raise


def resolve_repo_root() -> Path:
    return Path(run_git("rev-parse", "--show-toplevel"))


def read_project_version(repo_root: Path) -> str:
    pom = repo_root / "pom.xml"
    root = ET.parse(pom).getroot()
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    dc3_version = root.findtext("m:properties/m:dc3.version", namespaces=namespace)
    if dc3_version:
        return dc3_version
    project_version = root.findtext("m:version", namespaces=namespace)
    if project_version:
        return project_version
    raise RuntimeError("Unable to resolve project version from pom.xml")


def rev_parse(ref: str) -> str:
    return run_git("rev-parse", ref)


def previous_release_tag(to_ref: str, tag_pattern: str) -> str:
    tags_text = run_git(
        "for-each-ref",
        "--merged",
        to_ref,
        "--sort=-creatordate",
        "--format=%(refname:short)",
        f"refs/tags/{tag_pattern}",
    )
    tags = [tag for tag in tags_text.splitlines() if tag]
    if not tags:
        return ""
    to_hash = rev_parse(to_ref)
    for tag in tags:
        if rev_parse(tag) != to_hash:
            return tag
    return tags[0]


def parse_commit(record: str) -> Commit:
    full_hash, short_hash, subject, author = record.split("\x1f")
    match = CONVENTIONAL_RE.match(subject)
    if match:
        commit_type = match.group("type").lower()
        scope = match.group("scope") or ""
        description = match.group("desc")
        breaking = bool(match.group("breaking"))
    else:
        commit_type = "other"
        scope = ""
        description = subject
        breaking = False

    category = TYPE_TO_CATEGORY.get(commit_type, "Other")
    security_text = f"{commit_type} {scope} {description}".lower()
    if any(token in security_text for token in ("security", "vulnerability", "cve", "auth bypass")):
        category = "Security"
    if breaking:
        category = "Breaking Changes"

    return Commit(full_hash, short_hash, subject, author, commit_type, scope, description, breaking, category)


def read_commits(from_ref: str, to_ref: str, include_merges: bool) -> list[Commit]:
    revision = f"{from_ref}..{to_ref}" if from_ref else to_ref
    args = ["log", "--pretty=format:%H%x1f%h%x1f%s%x1f%an%x1e", revision]
    if not include_merges:
        args.insert(1, "--no-merges")
    output = run_git(*args)
    if not output:
        return []
    return [parse_commit(record) for record in output.strip("\x1e").split("\x1e") if record.strip()]


def changed_files(commit_hash: str) -> set[str]:
    output = run_git("diff-tree", "--no-commit-id", "--name-only", "-r", commit_hash)
    return {line for line in output.splitlines() if line}


def is_generated_changelog_commit(commit: Commit, change_file: Path) -> bool:
    if not GENERATED_CHANGELOG_RE.match(commit.subject):
        return False
    normalized_change_file = change_file.as_posix()
    return changed_files(commit.full_hash) == {normalized_change_file}


def filter_commits(commits: list[Commit], change_file: Path, include_changelog_commits: bool) -> list[Commit]:
    if include_changelog_commits:
        return commits
    return [commit for commit in commits if not is_generated_changelog_commit(commit, change_file)]


def format_commit(commit: Commit) -> str:
    if commit.scope:
        return f"- **{commit.scope}**: {commit.description} (`{commit.short_hash}`)"
    return f"- {commit.description} (`{commit.short_hash}`)"


def build_summary(commits: list[Commit], from_ref: str, to_ref: str) -> list[str]:
    if not commits:
        return ["- No commits found in the selected range."]

    categories = Counter(commit.category for commit in commits)
    scopes = Counter(commit.scope for commit in commits if commit.scope)
    category_summary = ", ".join(
        f"{category} {categories[category]}" for category in CATEGORY_ORDER if categories.get(category)
    )

    lines = [
        f"- Generated from `{from_ref or 'repository start'}` to `{to_ref}`.",
        f"- Included {len(commits)} commits across {len(categories)} categories: {category_summary}.",
    ]
    if scopes:
        top_scopes = ", ".join(f"{scope}({count})" for scope, count in scopes.most_common(6))
        lines.append(f"- Most active scopes: {top_scopes}.")

    highlights = [
        commit
        for category in ("Breaking Changes", "Security", "Features", "Bug Fixes", "Refactoring")
        for commit in commits
        if commit.category == category
    ][:5]
    if highlights:
        lines.append(
            "- Highlights: "
            + "; ".join(
                f"{commit.scope + ': ' if commit.scope else ''}{commit.description}" for commit in highlights
            )
            + "."
        )
    return lines


def build_release_block(version: str, commits: list[Commit], from_ref: str, to_ref: str) -> str:
    today = dt.date.today().isoformat()
    grouped: dict[str, list[Commit]] = defaultdict(list)
    for commit in commits:
        grouped[commit.category].append(commit)

    lines = [f"### \U0001f4cc {version}", "", f"_Generated on {today}._", "", "#### Summary"]
    lines.extend(build_summary(commits, from_ref, to_ref))

    for category in CATEGORY_ORDER:
        category_commits = grouped.get(category)
        if not category_commits:
            continue
        lines.extend(["", f"#### {CATEGORY_TITLES[category]}"])
        lines.extend(format_commit(commit) for commit in category_commits)

    return "\n".join(lines).strip()


def split_changelog(existing: str) -> tuple[str, str]:
    if DETAILS_OPEN in existing:
        index = existing.index(DETAILS_OPEN)
        return existing[:index].strip(), existing[index:].strip()
    return existing.strip(), ""


def strip_header(top: str) -> str:
    top = top.strip()
    if top.startswith(HEADER):
        return top[len(HEADER):].strip()
    return top


def known_versions(markdown: str) -> set[str]:
    return set(VERSION_RE.findall(markdown))


def merge_historical(top: str, details: str, version: str) -> str:
    old_top = strip_header(top)
    if not old_top or version in known_versions(old_top):
        return details or f"{DETAILS_OPEN}\n{DETAILS_SUMMARY}\n\n</details>"

    old_versions = known_versions(old_top)
    detail_versions = known_versions(details)
    if old_versions and old_versions.issubset(detail_versions):
        return details

    if not details:
        return f"{DETAILS_OPEN}\n{DETAILS_SUMMARY}\n\n{old_top}\n\n</details>"

    lines = details.splitlines()
    insert_at = 1
    for index, line in enumerate(lines[:5]):
        if line.startswith("<summary>"):
            insert_at = index + 1
            break
    lines[insert_at:insert_at] = ["", old_top]
    return "\n".join(lines).strip()


def update_changelog(path: Path, release_block: str, version: str) -> None:
    existing = path.read_text(encoding="utf-8") if path.exists() else ""
    top, details = split_changelog(existing)
    historical = merge_historical(top, details, version)
    path.write_text(f"{HEADER}\n\n{release_block}\n\n{historical}\n", encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate categorized CHANGE.md content from git commits.")
    parser.add_argument("--from", dest="from_ref", default=os.getenv("FROM", ""))
    parser.add_argument("--to", dest="to_ref", default=os.getenv("TO", "HEAD"))
    parser.add_argument("--version", default=os.getenv("VERSION", ""))
    parser.add_argument("--file", dest="change_file", default=os.getenv("CHANGE_FILE", "dc3/doc/CHANGE.md"))
    parser.add_argument("--tag-pattern", default=os.getenv("TAG_PATTERN", DEFAULT_TAG_PATTERN))
    parser.add_argument("--include-merges", action="store_true", default=os.getenv("INCLUDE_MERGES", "") == "true")
    parser.add_argument(
        "--include-changelog-commits",
        action="store_true",
        default=os.getenv("INCLUDE_CHANGELOG_COMMITS", "") == "true",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    repo_root = resolve_repo_root()
    os.chdir(repo_root)

    version = args.version or read_project_version(repo_root)
    from_ref = args.from_ref or previous_release_tag(args.to_ref, args.tag_pattern)
    change_file = Path(args.change_file)
    commits = read_commits(from_ref, args.to_ref, args.include_merges)
    commits = filter_commits(commits, change_file, args.include_changelog_commits)
    release_block = build_release_block(version, commits, from_ref, args.to_ref)
    update_changelog(repo_root / change_file, release_block, version)

    print(f"Updated {args.change_file}")
    print(f"Version: {version}")
    print(f"Range: {from_ref or 'repository start'}..{args.to_ref}")
    print(f"Commits: {len(commits)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
