#!/bin/bash

#
# Copyright 2016-present the IoT DC3 original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
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