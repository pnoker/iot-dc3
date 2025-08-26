#!/bin/bash

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

set -e

type=""

# shellcheck disable=SC2092
# shellcheck disable=SC2006
if `git status | grep "develop" &>/dev/null`; then
    type="develop"
fi

# shellcheck disable=SC2092
# shellcheck disable=SC2006
if `git status | grep "release" &>/dev/null`; then
    type="release"
fi

# shellcheck disable=SC2092
# shellcheck disable=SC2006
if `git status | grep "main" &>/dev/null`; then
    type="release"
fi

if [[ ${type} == "" ]]; then
    echo -e "This branch doesn't support tagging, please switch to the \033[31mdevelop\033[0m or \033[31mrelease\033[0m branch."
    exit
fi

git pull --tags

# shellcheck disable=SC2046
# shellcheck disable=SC2116
tag=$(echo dc3.${type}.$(date +'%Y%m%d').$(git tag -l "dc3.${type}.$(date +'%Y%m%d').*" | wc -l | xargs printf '%02d'))
echo "${tag}" 
git tag "${tag}"

git push origin --tags