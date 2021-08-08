#!/bin/bash

#
#  Copyright 2016-2021 Pnoker. All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

set -e

cd ../../

git pull --tags
# shellcheck disable=SC2046
# shellcheck disable=SC2116
develop_tag=$(echo dc3.develop.$(date +'%Y%m%d').$(git tag -l "dc3.develop.$(date +'%Y%m%d').*" | wc -l | xargs printf '%02d'))
echo "${develop_tag}"
git tag "${develop_tag}"

git push origin --tags