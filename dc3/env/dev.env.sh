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

# Postgres
export POSTGRES_HOST=localhost
export POSTGRES_PORT=35432
export POSTGRES_USERNAME=dc3
export POSTGRES_PASSWORD=dc3dc3dc3
export POSTGRES_DB=dc3

# RabbitMQ
export RABBITMQ_VIRTUAL_HOST=dc3
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=35672
export RABBITMQ_USERNAME=dc3
export RABBITMQ_PASSWORD=dc3dc3dc3

# Mqtt
export MQTT_BROKER_HOST=localhost
export MQTT_BROKER_PORT=31883
export MQTT_USERNAME=dc3
export MQTT_PASSWORD=dc3dc3dc3

#Grpc
export CENTER_AUTH_HOST=localhost
export CENTER_DATA_HOST=localhost
export CENTER_MANAGER_HOST=localhost
export CENTER_AGENTIC_HOST=localhost

# Runtime
export NODE_ENV=dev
export DC3_FACADE_MODE=grpc
export POINT_BATCH_SPEED=100
export POINT_BATCH_INTERVAL=5
export MQTT_BATCH_SPEED=100
export MQTT_BATCH_INTERVAL=5

# Spring AI
export OPENAI_BASE_URL=https://api.openai.com
export OPENAI_API_KEY=sk-your-api-key
export OPENAI_MODEL=gpt-4o
export OPENAI_TEMPERATURE=0.7
export OPENAI_MAX_TOKENS=2048
export AGENTIC_MEMORY_SCHEMA_INIT=always
export AGENTIC_MEMORY_MAX_MESSAGES=50
export AGENTIC_SESSION_TTL_HOURS=72
