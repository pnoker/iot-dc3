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

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
compose_file="${script_dir}/../docker-compose-db.yml"

export DC3_IMAGE_REGISTRY="${DC3_IMAGE_REGISTRY:-registry.cn-beijing.aliyuncs.com/dc3}"

podman compose -f "${compose_file}" pull
if [[ "${RESET_VOLUMES:-false}" == "true" ]]; then
    podman compose -f "${compose_file}" down -v
else
    podman compose -f "${compose_file}" down
fi
podman compose -f "${compose_file}" up -d
