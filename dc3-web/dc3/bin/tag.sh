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

set -euo pipefail

branch=$(git rev-parse --abbrev-ref HEAD)

case "${branch}" in
    develop)
        type="develop"
        ;;
    main | master | release | release/*)
        type="release"
        ;;
    *)
        echo -e "This branch doesn't support tagging, please switch to the \033[31mdevelop\033[0m or \033[31mrelease\033[0m branch."
        exit 1
        ;;
esac

git pull --tags

date=$(date +'%Y%m%d')
count=$(git tag -l "dc3.${type}.${date}.*" | wc -l | xargs printf '%02d')
tag="dc3.${type}.${date}.${count}"

echo "${tag}"
git tag "${tag}"
git push origin "${tag}"
