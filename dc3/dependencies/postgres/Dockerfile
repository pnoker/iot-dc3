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

# Layers the DC3 seed SQL on top of the dc3-postgres-base image (PostgreSQL +
# AGE/TimescaleDB/pgvector, built in dc3-docker). The base registry/tag are
# passed in by docker-compose-db.yml so this image follows the same release line.
ARG DC3_BASE_REGISTRY=pnoker
ARG DC3_BASE_TAG=2026.5

FROM ${DC3_BASE_REGISTRY}/dc3-postgres-base:${DC3_BASE_TAG}

LABEL dc3.author=pnokers
LABEL dc3.author.email=pnokers.icloud.com

# Schema/seed scripts the official entrypoint runs in filename order on first
# start. Demo data (../demo) is applied manually, so it is intentionally excluded.
COPY initdb/ /docker-entrypoint-initdb.d/
