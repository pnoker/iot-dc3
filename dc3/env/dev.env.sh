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

# Local source-run environment for IDE, EnvFile plugins, or Maven runs.
# This file mirrors the "Local source-run defaults" section in ../../.env.example.
# It intentionally excludes Compose-only variables such as DC3_IMAGE_* and DC3_*_PORT.

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
export RABBITMQ_SSL_ENABLED=false
export RABBITMQ_SSL_ALGORITHM=TLS
export RABBITMQ_SSL_VALIDATE_SERVER_CERTIFICATE=false
export RABBITMQ_SSL_VERIFY_HOSTNAME=false

# Mqtt
export MQTT_BROKER_HOST=localhost
export MQTT_BROKER_PORT=31883
export MQTT_USERNAME=dc3
export MQTT_PASSWORD=dc3dc3dc3

# gRPC
export CENTER_AUTH_HOST=localhost
export CENTER_MANAGER_HOST=localhost
export CENTER_DATA_HOST=localhost
export CENTER_AGENTIC_HOST=localhost

# Runtime
export NODE_ENV=dev
export DC3_FACADE_MODE=grpc
export DC3_FACADE_GRPC_DEADLINE_MS=3000
export DC3_SECURITY_KEY=dc3.security.key.2026.io.github.pnoker
export AUTH_HMAC_SECRET=io.github.pnoker.dc3
export POINT_BATCH_SPEED=100
export POINT_BATCH_INTERVAL=5
export MQTT_BATCH_SPEED=100
export MQTT_BATCH_INTERVAL=5

# Agentic fallback OpenAI-compatible provider. Normal providers are stored in dc3_model_provider.
export AGENTIC_FALLBACK_OPENAI_BASE_URL=https://api.openai.com
export AGENTIC_FALLBACK_OPENAI_API_KEY=
export AGENTIC_FALLBACK_OPENAI_MODEL=gpt-4o
export AGENTIC_FALLBACK_OPENAI_TEMPERATURE=0.7
export AGENTIC_FALLBACK_OPENAI_MAX_TOKENS=2048
export AGENTIC_MEMORY_SCHEMA_INIT=never
export AGENTIC_MEMORY_ENABLED=false
export AGENTIC_TOOL_CALLING_ENABLED=true
export AGENTIC_MEMORY_MAX_MESSAGES=50
export AGENTIC_ATTACHMENT_STORAGE_PATH=dc3/data/agentic/attachments

# Per-process overrides. Uncomment only for a single service process.
# export TCP_PORT=6270
# export UDP_PORT=6271
# export POSTGRES_SCHEMA=dc3_manager

# Gateway route overrides. Usually CENTER_*_HOST is enough for local runs.
# export GATEWAY_ROUTE_AUTH_TOKEN_URI=http://localhost:8300/
# export GATEWAY_ROUTE_AUTH_URI=http://localhost:8300
# export GATEWAY_ROUTE_MANAGER_URI=http://localhost:8400
# export GATEWAY_ROUTE_DATA_URI=http://localhost:8500
# export GATEWAY_ROUTE_AGENTIC_URI=http://localhost:8600
