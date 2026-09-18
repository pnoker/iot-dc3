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
#

# Layers on top of the dc3-mariadb-base image (upstream MariaDB + the DC3
# timezone convention, built in dc3-docker). Nothing is layered yet: DC3 runs
# on the PostgreSQL dialect, so there are no MariaDB seed scripts. Credentials,
# seeds, or entrypoint overrides land here when a MariaDB profile is needed.
# The base registry/tag are passed in by docker-compose so this image follows
# the same release line.
ARG DC3_BASE_REGISTRY=pnoker
ARG DC3_BASE_TAG=2026.6

FROM ${DC3_BASE_REGISTRY}/dc3-mariadb-base:${DC3_BASE_TAG}

LABEL dc3.author=pnokers
LABEL dc3.author.email=pnokers.icloud.com
