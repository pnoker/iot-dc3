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
"""Backfill GitHub Releases for versions recorded in ``dc3/doc/CHANGE.md``.

Every entry in CHANGE.md should have a matching GitHub Release. When a version
is recorded but never tagged/released (skipped release windows, failed runs),
this tool rebuilds the missing release from the recorded data alone:

* tag ``v<version>`` -> last commit on main dated on or before the version day
  (CHANGE.md versions are ``YYYY.M.D`` dates, so the mapping is deterministic);
* release body -> the same template the docker-ci release job assembles:
  TITLE.md intro + this version's changelog block + a <details> block with the
  N previous versions + the RELEASE-FOOTER quick start (``${SERVICE_VERSION}``
  substituted with the version; full usage/deployment content lives on
  docs.dc3.site) + a Full Changelog link to the previous release tag;
* tags are created through the GitHub API, so the ``Docker Images`` workflow
  (triggered by ``v*`` tag *pushes*) is NOT triggered for backfilled releases;
* the ``latest`` pointer is never touched - backfilled releases carry no images.

Usage:

  make release-backfill            # dry-run: list what would be created
  make release-backfill-apply      # create the missing releases
  make release-backfill-refresh    # re-render bodies of backfilled releases
  python3 dc3/bin/release_backfill.py --versions 2026.5.17   # explicit subset

"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
import tempfile
import time
from dataclasses import dataclass
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
CHANGE_FILE = REPO_ROOT / "dc3/doc/CHANGE.md"
TITLE_FILE = REPO_ROOT / "dc3/doc/TITLE.md"
FOOTER_FILE = REPO_ROOT / "dc3/doc/RELEASE-FOOTER.md"
USAGE_FILE = REPO_ROOT / "dc3/doc/USAGE.md"

VERSION_RE = re.compile(r"^(\d{4})\.(\d{1,2})\.(\d{1,2})$")
HEADING_RE = re.compile(r"^###\s+\D*(\d{4}\.\d{1,2}\.\d{1,2})\s*$")
DETAILS_LINE_RE = re.compile(r"^(<details>|</details>|<summary>.*</summary>)$")


@dataclass
class Plan:
    version: str
    commit: str
    subject: str
    body: str
    prev_tag: str | None


def run(cmd: list[str], *, check: bool = True) -> str:
    proc = subprocess.run(cmd, cwd=REPO_ROOT, capture_output=True, text=True)
    if check and proc.returncode != 0:
        raise SystemExit(f"command failed ({proc.returncode}): {' '.join(cmd)}\n{proc.stderr.strip()}")
    return proc.stdout.strip()


def parse_change() -> list[tuple[str, str]]:
    """Return [(version, block_text)] newest-first, exactly as CHANGE.md orders them."""
    blocks: list[tuple[str, str]] = []
    current_version: str | None = None
    current_lines: list[str] = []
    for line in CHANGE_FILE.read_text(encoding="utf-8").splitlines():
        match = HEADING_RE.match(line)
        if match:
            if current_version is not None:
                blocks.append((current_version, "\n".join(current_lines).strip()))
            current_version = match.group(1)
            current_lines = []
        elif current_version is not None:
            current_lines.append(line)
    if current_version is not None:
        blocks.append((current_version, "\n".join(current_lines).strip()))
    return blocks


def strip_embedded_details(block: str) -> str:
    """Drop <details>/<summary> markup that the changelog generator embeds.

    CHANGE.md keeps ALL pre-latest versions wrapped in one <details> block.
    A block extracted "until the next ### heading" of the latest version
    therefore ends with that opening markup; this tool always renders its own
    single <details> wrapper and must strip the embedded one to avoid nesting.
    """
    return "\n".join(line for line in block.splitlines() if not DETAILS_LINE_RE.match(line)).strip()


def released_versions() -> dict[str, str]:
    """Return {release_name: tag_name} for all existing GitHub releases."""
    out = run(["gh", "release", "list", "--limit", "300", "--json", "name,tagName"])
    import json
    return {item["name"]: item["tagName"] for item in json.loads(out)}


def version_commit(version: str) -> tuple[str, str]:
    """Map a YYYY.M.D version to the last commit dated on or before that day."""
    match = VERSION_RE.match(version)
    if not match:
        raise SystemExit(f"non-date version {version!r} cannot be mapped to a commit")
    year, month, day = (int(part) for part in match.groups())
    before = f"{year:04d}-{month:02d}-{day:02d}T23:59:59"
    sha = run(["git", "rev-list", "-1", f"--before={before}", "origin/main"], check=False)
    if not sha:
        raise SystemExit(f"no commit on or before {before} on origin/main for {version}")
    subject = run(["git", "log", "-1", "--format=%s", sha])
    return sha, subject


def previous_tag(version: str, ordered: list[str], releases: dict[str, str]) -> str | None:
    """Resolve the actual tag of the chronologically previous released version.

    ``ordered`` is newest-first, so the versions older than ``version`` are the
    entries AFTER its index; walk them nearest-first and return the first that
    exists as a release or a git tag.
    """
    idx = ordered.index(version)
    for candidate in ordered[idx + 1:]:
        if candidate in releases:
            return releases[candidate]
        for fallback in (f"v{candidate}", candidate):
            if run(["git", "rev-parse", "-q", "--verify", f"{fallback}^{{commit}}"], check=False):
                return fallback
    return None


def build_body(version: str, block: str, history: list[tuple[str, str]], prev_tag: str | None) -> str:
    title = TITLE_FILE.read_text(encoding="utf-8").replace("${SERVICE_VERSION}", version).strip()
    footer = FOOTER_FILE.read_text(encoding="utf-8").replace("${SERVICE_VERSION}", version).strip()

    parts = [title, "", "## ✨ What's Changed", "", f"### 📌 {version}", "", block, ""]
    if history:
        parts.append("<details>")
        parts.append("<summary>📝 Historical Version Description, Click to Expand</summary>")
        parts.append("")
        for prev_version, prev_block in history:
            parts.extend([f"### {prev_version}", "", prev_block, ""])
        parts.append("</details>")
        parts.append("")
    parts.extend([footer, ""])
    if prev_tag:
        parts.append(f"**Full Changelog**: https://github.com/pnoker/iot-dc3/compare/{prev_tag}...v{version}")
    return "\n".join(parts).strip() + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--apply", action="store_true", help="create releases (default: dry-run)")
    parser.add_argument("--refresh", action="store_true", help="re-render bodies of existing releases instead of creating")
    parser.add_argument("--history", type=int, default=3, help="previous versions folded into <details> (default 3)")
    parser.add_argument("--versions", nargs="*", help="explicit versions instead of all missing ones")
    args = parser.parse_args()

    blocks = parse_change()
    ordered = [version for version, _ in blocks]  # newest-first
    chronological = list(reversed(ordered))
    releases = released_versions()

    missing = [v for v in chronological if v not in releases]
    if args.refresh:
        # only releases this tool owns: name matches version AND tag is v<version>
        targets = args.versions or [v for v in chronological if releases.get(v) == f"v{v}"]
    else:
        targets = args.versions or missing
    unknown = [v for v in targets if v not in ordered]
    if unknown:
        raise SystemExit(f"versions not recorded in CHANGE.md: {', '.join(unknown)}")

    if not targets:
        action = "refresh" if args.refresh else "backfill"
        print(f"CHANGE.md records {len(ordered)} versions; nothing to {action}.")
        return 0

    mode = ("REFRESH" if args.refresh else "CREATE") if args.apply else ("DRY-RUN-REFRESH" if args.refresh else "DRY-RUN")
    print(f"{mode}: {len(targets)} release(s) to backfill")
    plans: list[Plan] = []
    for version in targets:
        block = strip_embedded_details(dict(blocks)[version])
        idx = chronological.index(version)
        history = [(v, strip_embedded_details(dict(blocks)[v])) for v in chronological[max(0, idx - args.history):idx]]
        history = [(v, b) for v, b in history if b]
        sha, subject = version_commit(version)
        prev = previous_tag(version, ordered, releases)
        body = build_body(version, block, history, prev)
        plans.append(Plan(version, sha, subject, body, prev))
        print(f"  {version:<12} -> {sha[:9]}  {subject[:60]}")

    if not args.apply:
        print("dry-run only; pass --apply (or make release-backfill-apply) to create them")
        return 0

    for plan in plans:
        with tempfile.NamedTemporaryFile("w", suffix=".md", delete=False) as handle:
            handle.write(plan.body)
            notes = handle.name
        if args.refresh:
            run(["gh", "release", "edit", f"v{plan.version}", "--notes-file", notes])
            print(f"refreshed v{plan.version}")
        else:
            run(["gh", "release", "create", f"v{plan.version}", "--target", plan.commit,
                 "--title", plan.version, "--notes-file", notes])
            print(f"created v{plan.version}")
        time.sleep(1)
    verb = "refreshed" if args.refresh else "created"
    print(f"done: {len(plans)} release(s) {verb}. The 'latest' pointer was not touched.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
