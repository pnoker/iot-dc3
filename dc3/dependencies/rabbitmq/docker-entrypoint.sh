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

set -euo pipefail

: "${RABBITMQ_DEFAULT_VHOST:=${RABBITMQ_VIRTUAL_HOST:-dc3}}"
: "${RABBITMQ_DEFAULT_USER:=${RABBITMQ_USERNAME:-dc3}}"
: "${RABBITMQ_MQTT_EXCHANGE:=dc3.e.mqtt}"

if [ -z "${RABBITMQ_DEFAULT_PASS:-}" ]; then
	if [ -n "${RABBITMQ_PASSWORD:-}" ]; then
		RABBITMQ_DEFAULT_PASS="$RABBITMQ_PASSWORD"
	else
		echo "RABBITMQ_DEFAULT_PASS or RABBITMQ_PASSWORD must be set" >&2
		exit 1
	fi
fi

export RABBITMQ_DEFAULT_VHOST
export RABBITMQ_DEFAULT_USER
export RABBITMQ_DEFAULT_PASS
export RABBITMQ_MQTT_EXCHANGE

escape_sed_replacement() {
	printf '%s' "$1" | sed -e 's/[\/&]/\\&/g'
}

render_template() {
	local input="$1"
	local output="$2"
	local vhost user pass pass_hash exchange

	vhost="$(escape_sed_replacement "$RABBITMQ_DEFAULT_VHOST")"
	user="$(escape_sed_replacement "$RABBITMQ_DEFAULT_USER")"
	pass="$(escape_sed_replacement "$RABBITMQ_DEFAULT_PASS")"
	pass_hash="$(rabbitmqctl hash_password "$RABBITMQ_DEFAULT_PASS" | tail -n 1)"
	pass_hash="$(escape_sed_replacement "$pass_hash")"
	exchange="$(escape_sed_replacement "$RABBITMQ_MQTT_EXCHANGE")"

	sed \
		-e "s/@RABBITMQ_DEFAULT_VHOST@/${vhost}/g" \
		-e "s/@RABBITMQ_DEFAULT_USER@/${user}/g" \
		-e "s/@RABBITMQ_DEFAULT_PASS@/${pass}/g" \
		-e "s/@RABBITMQ_DEFAULT_PASS_HASH@/${pass_hash}/g" \
		-e "s/@RABBITMQ_MQTT_EXCHANGE@/${exchange}/g" \
		"$input" > "$output"
}

render_template /etc/rabbitmq/rabbitmq.conf.template /etc/rabbitmq/rabbitmq.conf
render_template /etc/rabbitmq/definitions.json.template /etc/rabbitmq/definitions.json

exec docker-entrypoint.sh "$@"
