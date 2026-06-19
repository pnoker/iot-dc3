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

# Layers DC3 broker config on top of the dc3-rabbitmq-base image (upstream
# RabbitMQ + plugins, built in dc3-docker). The base registry/tag are passed in
# by docker-compose-db.yml so this image follows the same release line.
ARG DC3_BASE_REGISTRY=pnoker
ARG DC3_BASE_TAG=2026.5

FROM ${DC3_BASE_REGISTRY}/dc3-rabbitmq-base:${DC3_BASE_TAG}

LABEL dc3.author=pnokers
LABEL dc3.author.email=pnokers.icloud.com

# The entrypoint renders the *.template files (vhost/user/password/exchange
# placeholders) into the real config on container start.
COPY ./rabbitmq.conf /etc/rabbitmq/rabbitmq.conf.template
COPY ./definitions.json /etc/rabbitmq/definitions.json.template
COPY ./docker-entrypoint.sh /usr/local/bin/dc3-rabbitmq-entrypoint.sh
COPY ./keys /data/rabbitmq/dc3/keys

RUN chmod +x /usr/local/bin/dc3-rabbitmq-entrypoint.sh

ENTRYPOINT ["dc3-rabbitmq-entrypoint.sh"]
CMD ["rabbitmq-server"]
