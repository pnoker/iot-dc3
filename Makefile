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

# tip:
# make -f ./Makefile help

.PHONY: help clean package app app-all dev dev-all dev-db dev-optional build deploy tag changelog install-hooks \
	check-compose compose-file compose-up compose-down compose-ps compose-config compose-build \
	compose-logs compose-pull compose-refresh compose-reset compose-restart

COMPOSE ?= podman compose
COMPOSE_DIR ?= dc3
REGISTRY ?= global
STACK ?= dev
MVN_SETTINGS := .mvn/settings.xml
MVN := mvn -s $(MVN_SETTINGS)
MVN_SUB := mvn -s ../$(MVN_SETTINGS)

ifeq ($(REGISTRY),global)
DEFAULT_IMAGE_REGISTRY := pnoker
else ifeq ($(REGISTRY),overseas)
DEFAULT_IMAGE_REGISTRY := pnoker
else ifeq ($(REGISTRY),international)
DEFAULT_IMAGE_REGISTRY := pnoker
else ifeq ($(REGISTRY),domestic)
DEFAULT_IMAGE_REGISTRY := registry.cn-beijing.aliyuncs.com/dc3
else ifeq ($(REGISTRY),aliyun)
DEFAULT_IMAGE_REGISTRY := registry.cn-beijing.aliyuncs.com/dc3
else ifeq ($(REGISTRY),cn)
DEFAULT_IMAGE_REGISTRY := registry.cn-beijing.aliyuncs.com/dc3
else
$(error Unsupported REGISTRY '$(REGISTRY)'. Use REGISTRY=global|overseas or REGISTRY=domestic|aliyun|cn)
endif

DC3_IMAGE_REGISTRY ?= $(DEFAULT_IMAGE_REGISTRY)
COMPOSE_ENV := DC3_IMAGE_REGISTRY="$(DC3_IMAGE_REGISTRY)"
STACK_SUFFIX := $(if $(filter app,$(STACK)),,-$(STACK))
CHANGE_FILE ?= dc3/doc/CHANGE.md
FROM ?=
TO ?= HEAD
VERSION ?=

ifeq ($(origin COMPOSE_FILE), undefined)
RESOLVED_COMPOSE_FILE := $(COMPOSE_DIR)/docker-compose$(STACK_SUFFIX).yml
MAKE_COMPOSE_OVERRIDE :=
else
RESOLVED_COMPOSE_FILE := $(if $(findstring /,$(COMPOSE_FILE)),$(COMPOSE_FILE),$(COMPOSE_DIR)/$(COMPOSE_FILE))
MAKE_COMPOSE_OVERRIDE := COMPOSE_FILE="$(COMPOSE_FILE)"
endif

help:
	echo 'You can use make to execute the following commands:' \
	&& echo 'Usage: make [help | clean | package | changelog | app | app-all | dev-db | dev-optional | dev | dev-all | build | deploy | tag]' \
	&& echo ' - make clean: clean Maven build artifacts' \
	&& echo ' - make package: package all modules with Maven' \
	&& echo ' - make changelog [FROM=<ref>] [TO=HEAD] [VERSION=<version>]: update dc3/doc/CHANGE.md from git commits' \
	&& echo ' - make install-hooks: install repository Git hooks' \
	&& echo ' - make tag: git tag' \
	&& echo ' - make app: run the packaged application stack (docker-compose.yml)' \
	&& echo ' - make app-all: run db + optional + packaged application stacks' \
	&& echo ' - make dev-db: run the base dependency stack (Postgres + RabbitMQ)' \
	&& echo ' - make dev-optional: run optional local dependencies (EMQX)' \
	&& echo ' - make dev: run the local development application stack (docker-compose-dev.yml)' \
	&& echo ' - make dev-all: run db + optional + local development application stacks' \
	&& echo ' - make build: build images with the selected compose file (default: docker-compose-dev.yml)' \
	&& echo ' - make compose-up STACK=<app|dev|db|optional|grafana|elasticsearch> [REGISTRY=domestic]' \
	&& echo ' - make compose-down STACK=<...>: stop the selected compose stack' \
	&& echo ' - make compose-ps STACK=<...>: list containers in the selected compose stack' \
	&& echo ' - make compose-config STACK=<...>: print the rendered compose configuration' \
	&& echo ' - make compose-pull STACK=<...>: pull images for the selected compose stack' \
	&& echo ' - make compose-refresh STACK=<...>: pull, stop, and start the selected compose stack' \
	&& echo ' - make compose-logs STACK=<...>: tail logs for the selected compose stack' \
	&& echo ' - make compose-restart STACK=<...>: restart the selected compose stack' \
	&& echo ' - make compose-reset STACK=<...> CONFIRM_RESET_VOLUMES=true: stop stack and delete volumes' \
	&& echo ' - make compose-file STACK=<...>: print the resolved compose file path' \
	&& echo ' - make deploy: deploy with mvn -s .mvn/settings.xml' \
	&& echo 'Registry aliases:' \
	&& echo '   global / overseas / international -> DC3_IMAGE_REGISTRY=pnoker' \
	&& echo '   domestic / aliyun / cn           -> DC3_IMAGE_REGISTRY=registry.cn-beijing.aliyuncs.com/dc3' \
	&& echo 'Examples:' \
	&& echo '   make dev' \
	&& echo '   make dev REGISTRY=domestic' \
	&& echo '   make dev-all REGISTRY=domestic' \
	&& echo '   make app-all REGISTRY=aliyun' \
	&& echo '   make compose-up STACK=grafana REGISTRY=cn' \
	&& echo '   make compose-logs STACK=dev REGISTRY=global'

clean:
	$(MVN) clean

package:
	$(MVN) clean package

tag:
	dc3/bin/tag.sh

changelog:
	@FROM="$(FROM)" TO="$(TO)" VERSION="$(VERSION)" CHANGE_FILE="$(CHANGE_FILE)" dc3/bin/changelog.py

install-hooks:
	git config core.hooksPath .githooks
	chmod +x .githooks/commit-msg dc3/bin/commit_msg_lint.py dc3/bin/changelog.py

check-compose:
	@test -f "$(RESOLVED_COMPOSE_FILE)" || (echo "Compose file not found: $(RESOLVED_COMPOSE_FILE)" && exit 1)

compose-file: check-compose
	@echo "$(RESOLVED_COMPOSE_FILE)"

compose-up: check-compose
	$(COMPOSE_ENV) $(COMPOSE) -f "$(RESOLVED_COMPOSE_FILE)" up -d

compose-down: check-compose
	$(COMPOSE_ENV) $(COMPOSE) -f "$(RESOLVED_COMPOSE_FILE)" down

compose-ps: check-compose
	$(COMPOSE_ENV) $(COMPOSE) -f "$(RESOLVED_COMPOSE_FILE)" ps

compose-config: check-compose
	$(COMPOSE_ENV) $(COMPOSE) -f "$(RESOLVED_COMPOSE_FILE)" config

compose-build: check-compose
	$(COMPOSE_ENV) $(COMPOSE) -f "$(RESOLVED_COMPOSE_FILE)" build

compose-pull: check-compose
	$(COMPOSE_ENV) $(COMPOSE) -f "$(RESOLVED_COMPOSE_FILE)" pull

compose-refresh: check-compose
	$(COMPOSE_ENV) $(COMPOSE) -f "$(RESOLVED_COMPOSE_FILE)" pull
	$(COMPOSE_ENV) $(COMPOSE) -f "$(RESOLVED_COMPOSE_FILE)" down
	$(COMPOSE_ENV) $(COMPOSE) -f "$(RESOLVED_COMPOSE_FILE)" up -d

compose-reset: check-compose
	@test "$(CONFIRM_RESET_VOLUMES)" = "true" || (echo "Refusing to delete volumes. Re-run with CONFIRM_RESET_VOLUMES=true" && exit 1)
	$(COMPOSE_ENV) $(COMPOSE) -f "$(RESOLVED_COMPOSE_FILE)" down -v

compose-logs: check-compose
	$(COMPOSE_ENV) $(COMPOSE) -f "$(RESOLVED_COMPOSE_FILE)" logs -f --tail=200

compose-restart: check-compose
	$(COMPOSE_ENV) $(COMPOSE) -f "$(RESOLVED_COMPOSE_FILE)" restart

app:
	@$(MAKE) compose-up STACK=app REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)

app-all:
	@$(MAKE) dev-db REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)
	@$(MAKE) dev-optional REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)
	@$(MAKE) app REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)

dev-db:
	@$(MAKE) compose-up STACK=db REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)

dev-optional:
	@$(MAKE) compose-up STACK=optional REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)

dev:
	@$(MAKE) compose-up STACK=dev REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)

dev-all:
	@$(MAKE) dev-db REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)
	@$(MAKE) dev-optional REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)
	@$(MAKE) dev REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)

build: package
	@$(MAKE) compose-build STACK=dev REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)

deploy: package
	cd dc3-api \
	&& $(MVN_SUB) clean deploy -p deploy \
	&& cd ../dc3-common \
	&& $(MVN_SUB) clean deploy -p deploy
