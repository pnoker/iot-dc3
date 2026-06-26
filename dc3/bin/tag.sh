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

# Create a semver release tag on main: vYYYY.M.P
# Usage: make tag [patch|minor|major]            (or: dc3/bin/tag.sh <bump> [--dry-run])
set -euo pipefail

bump=""
dryrun=0
for arg in "$@"; do
    case "$arg" in
        patch|minor|major) bump="$arg" ;;
        --dry-run) dryrun=1 ;;
        *) echo "unknown argument: $arg (expected: patch|minor|major|--dry-run)" >&2; exit 1 ;;
    esac
done
[ -n "$bump" ] || bump="patch"

branch=$(git rev-parse --abbrev-ref HEAD)
if [ "$branch" != "main" ]; then
    echo "Tagging is only allowed on 'main' (current: '$branch'). Switch to main first." >&2
    exit 1
fi

git pull --tags --quiet

last=$(git tag -l "v*" --sort=-v:refname | head -1)
if [ -z "$last" ]; then
    echo "No 'v*' tag found. Create an initial baseline tag first, e.g.:" >&2
    echo "  git tag v2025.9.3 && git push origin v2025.9.3" >&2
    exit 1
fi

re='^v([0-9]+)\.([0-9]+)\.([0-9]+)$'
if ! [[ $last =~ $re ]]; then
    echo "Unparseable last tag: $last (expected vYYYY.M.P)" >&2
    exit 1
fi
major=${BASH_REMATCH[1]}; minor=${BASH_REMATCH[2]}; patch=${BASH_REMATCH[3]}
case "$bump" in
    patch) patch=$((patch + 1)) ;;
    minor) minor=$((minor + 1)); patch=0 ;;
    major) major=$((major + 1)); minor=0; patch=0 ;;
esac
newtag="v${major}.${minor}.${patch}"

echo "last: $last -> new: $newtag"
[ "$dryrun" = "1" ] && { echo "(dry-run, not tagging)"; exit 0; }

git tag "$newtag"
git push origin "$newtag"
gh release create "$newtag" --generate-notes --title "$newtag"
echo "Tagged and released $newtag"
