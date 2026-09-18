#!/usr/bin/env bash

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

# Create the release tag that exactly matches the root pom.xml version.
# Usage: make tag                                (or: dc3/bin/tag.sh [--dry-run])
set -euo pipefail

dryrun=0
for arg in "$@"; do
    case "$arg" in
        --dry-run) dryrun=1 ;;
        *) echo "unknown argument: $arg (expected: --dry-run)" >&2; exit 1 ;;
    esac
done

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

branch=$(git rev-parse --abbrev-ref HEAD)
if [ "$branch" != "main" ]; then
    echo "Tagging is only allowed on 'main' (current: '$branch'). Switch to main first." >&2
    exit 1
fi

if [ -n "$(git status --porcelain)" ]; then
    echo "Release tagging requires a clean working tree." >&2
    exit 1
fi

git fetch origin main --tags --quiet
local_head=$(git rev-parse HEAD)
remote_head=$(git rev-parse origin/main)
if [ "$local_head" != "$remote_head" ]; then
    echo "Local main must exactly match origin/main before tagging." >&2
    exit 1
fi

version=$(awk -F'[<>]' '/<parent>/,/<\/parent>/{if($2=="version"){print $3; exit}}' pom.xml)
if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Invalid project version in pom.xml: '$version' (expected YYYY.M.P)." >&2
    exit 1
fi
newtag="v${version}"

if git rev-parse -q --verify "refs/tags/$newtag" >/dev/null; then
    echo "Release tag already exists locally: $newtag" >&2
    exit 1
fi

if git ls-remote --exit-code --tags origin "refs/tags/$newtag" >/dev/null 2>&1; then
    echo "Release tag already exists on origin: $newtag" >&2
    exit 1
fi

echo "release tag: $newtag (commit $local_head)"
[ "$dryrun" = "1" ] && { echo "(dry-run, not tagging)"; exit 0; }

git tag -a "$newtag" -m "Release $newtag"
git push origin "$newtag"
echo "Pushed $newtag. The Docker Images workflow will verify, publish, and create the GitHub Release."
