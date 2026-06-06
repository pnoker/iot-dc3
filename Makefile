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

SHELL := /bin/bash
.DEFAULT_GOAL := help

.PHONY: help clean package test test-it test-e2e coverage \
	build up stop down ps logs config pull restart refresh reset print-compose-file \
	app app-all dev dev-all dev-db dev-optional \
	compose-build compose-up compose-down compose-ps compose-logs compose-config compose-pull \
	compose-stop compose-restart compose-refresh compose-reset compose-file \
	changelog deploy install-hooks tag check-compose check-service-group

COMPOSE ?= podman compose
COMPOSE_DIR ?= dc3
REGISTRY ?= global
STACK ?= dev
SERVICES ?=
GROUP ?=

GROUP_SERVICES_center := auth manager data agentic
GROUP_SERVICES_core := $(GROUP_SERVICES_center) gateway
GROUP_SERVICES_drivers := listening-virtual modbus-tcp mqtt opc-da opc-ua plcs7 virtual
SELECTED_SERVICES := $(strip $(SERVICES) $(GROUP_SERVICES_$(GROUP)))

MVN_SETTINGS ?= .mvn/settings.xml
MVN_SETTINGS_ARG := $(if $(strip $(MVN_SETTINGS)),-s $(MVN_SETTINGS),)
MVN_SUB_SETTINGS_ARG := $(if $(strip $(MVN_SETTINGS)),-s ../$(MVN_SETTINGS),)
MVN := mvn $(MVN_SETTINGS_ARG)
MVN_SUB := mvn $(MVN_SUB_SETTINGS_ARG)

CHANGE_FILE ?= dc3/doc/CHANGE.md
FROM ?=
TO ?= HEAD
VERSION ?=

ifeq ($(REGISTRY),global)
DEFAULT_IMAGE_REGISTRY := pnoker
else ifeq ($(REGISTRY),cn)
DEFAULT_IMAGE_REGISTRY := registry.cn-beijing.aliyuncs.com/dc3
else
$(error Unsupported REGISTRY '$(REGISTRY)'. Use REGISTRY=global|cn)
endif

DC3_IMAGE_REGISTRY ?= $(DEFAULT_IMAGE_REGISTRY)
COMPOSE_ENV := DC3_IMAGE_REGISTRY="$(DC3_IMAGE_REGISTRY)"
STACK_SUFFIX := $(if $(filter app,$(STACK)),,-$(STACK))

ifeq ($(origin COMPOSE_FILE), undefined)
RESOLVED_COMPOSE_FILE := $(COMPOSE_DIR)/docker-compose$(STACK_SUFFIX).yml
MAKE_COMPOSE_OVERRIDE :=
else
RESOLVED_COMPOSE_FILE := $(if $(findstring /,$(COMPOSE_FILE)),$(COMPOSE_FILE),$(COMPOSE_DIR)/$(COMPOSE_FILE))
MAKE_COMPOSE_OVERRIDE := COMPOSE_FILE="$(COMPOSE_FILE)"
endif

define dc3_compose
$(COMPOSE_ENV) $(COMPOSE) -f "$(RESOLVED_COMPOSE_FILE)"
endef

help:
	@printf '%s\n' 'IoT DC3 Make targets'
	@printf '%s\n' ''
	@printf '%s\n' 'Common:'
	@printf '  %-24s %s\n' 'make package' 'Build all Maven modules'
	@printf '  %-24s %s\n' 'make test' 'Run unit tests'
	@printf '  %-24s %s\n' 'make test-it' 'Run integration-test phase'
	@printf '  %-24s %s\n' 'make test-e2e' 'Run E2E harness'
	@printf '  %-24s %s\n' 'make coverage' 'Generate aggregated JaCoCo coverage'
	@printf '%s\n' ''
	@printf '%s\n' 'Compose:'
	@printf '  %-24s %s\n' 'make build' 'Build STACK images, optional SERVICES="data gateway" or GROUP=core'
	@printf '  %-24s %s\n' 'make up' 'Start STACK, optional SERVICES="data gateway" or GROUP=core'
	@printf '  %-24s %s\n' 'make stop' 'Stop selected services or the whole STACK'
	@printf '  %-24s %s\n' 'make down' 'Stop STACK'
	@printf '  %-24s %s\n' 'make ps' 'List STACK containers'
	@printf '  %-24s %s\n' 'make logs' 'Follow STACK logs'
	@printf '  %-24s %s\n' 'make config' 'Render compose configuration'
	@printf '%s\n' ''
	@printf '%s\n' 'Variables:'
	@printf '  %-24s %s\n' 'STACK=dev|app|db|optional' 'Compose stack selector'
	@printf '  %-24s %s\n' 'REGISTRY=global|cn' 'Image registry selector'
	@printf '  %-24s %s\n' 'SERVICES="..."' 'Compose service list, empty means all services'
	@printf '  %-24s %s\n' 'GROUP=center|core|drivers' 'Predefined service group'
	@printf '%s\n' ''
	@printf '%s\n' 'Examples:'
	@printf '  %s\n' 'make dev-db REGISTRY=cn'
	@printf '  %s\n' 'make up SERVICES="agentic" REGISTRY=cn'
	@printf '  %s\n' 'make up SERVICES="gateway agentic" REGISTRY=cn'
	@printf '  %s\n' 'make up GROUP=core REGISTRY=cn'
	@printf '  %s\n' 'make up STACK=optional SERVICES="prometheus grafana" REGISTRY=cn'
	@printf '  %s\n' 'make logs SERVICES="gateway agentic"'

clean:
	$(MVN) clean

package:
	$(MVN) clean package

test:
	$(MVN) -B -Dmaven.test.skip=false test

test-it:
	$(MVN) -B -Dmaven.test.skip=false -Dskip.unit.tests=true verify

test-e2e:
	DC3_E2E=true $(MVN) -B -Dmaven.test.skip=false -Dskip.unit.tests=true -pl dc3-e2e -am -Pe2e verify

coverage:
	$(MVN) -B -Dmaven.test.skip=false -pl dc3-coverage -am verify

check-compose:
	@test -f "$(RESOLVED_COMPOSE_FILE)" || (echo "Compose file not found: $(RESOLVED_COMPOSE_FILE)" && exit 1)

check-service-group:
	@if [ -n "$(GROUP)" ] && [ -z "$(GROUP_SERVICES_$(GROUP))" ]; then \
		echo "Unsupported GROUP '$(GROUP)'. Use GROUP=center|core|drivers"; \
		exit 1; \
	fi

print-compose-file: check-compose
	@echo "$(RESOLVED_COMPOSE_FILE)"

config: check-compose
	$(call dc3_compose) config

build: check-compose check-service-group
	$(call dc3_compose) build $(SELECTED_SERVICES)

up: check-compose check-service-group
	$(call dc3_compose) up -d $(SELECTED_SERVICES)

stop: check-compose check-service-group
	$(call dc3_compose) stop $(SELECTED_SERVICES)

down: check-compose
	$(call dc3_compose) down

ps: check-compose check-service-group
	$(call dc3_compose) ps $(SELECTED_SERVICES)

logs: check-compose check-service-group
	$(call dc3_compose) logs -f --tail=200 $(SELECTED_SERVICES)

pull: check-compose check-service-group
	$(call dc3_compose) pull $(SELECTED_SERVICES)

restart: check-compose check-service-group
	$(call dc3_compose) restart $(SELECTED_SERVICES)

refresh: check-compose check-service-group
	$(call dc3_compose) pull $(SELECTED_SERVICES)
	$(call dc3_compose) down
	$(call dc3_compose) up -d $(SELECTED_SERVICES)

reset: check-compose
	@test "$(CONFIRM_RESET_VOLUMES)" = "true" || (echo "Refusing to delete volumes. Re-run with CONFIRM_RESET_VOLUMES=true" && exit 1)
	$(call dc3_compose) down -v

dev-db:
	@$(MAKE) up STACK=db SERVICES= GROUP= REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)

dev-optional:
	@$(MAKE) up STACK=optional SERVICES= GROUP= REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)

dev:
	@$(MAKE) up STACK=dev SERVICES= GROUP= REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)

dev-all:
	@$(MAKE) dev-db REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)
	@$(MAKE) dev-optional REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)
	@$(MAKE) dev REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)

app:
	@$(MAKE) up STACK=app SERVICES= GROUP= REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)

app-all:
	@$(MAKE) dev-db REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)
	@$(MAKE) dev-optional REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)
	@$(MAKE) app REGISTRY=$(REGISTRY) COMPOSE='$(COMPOSE)' COMPOSE_DIR='$(COMPOSE_DIR)' $(MAKE_COMPOSE_OVERRIDE)

compose-build: build
compose-up: up
compose-stop: stop
compose-down: down
compose-ps: ps
compose-logs: logs
compose-config: config
compose-pull: pull
compose-restart: restart
compose-refresh: refresh
compose-reset: reset
compose-file: print-compose-file

tag:
	dc3/bin/tag.sh

changelog:
	@FROM="$(FROM)" TO="$(TO)" VERSION="$(VERSION)" CHANGE_FILE="$(CHANGE_FILE)" dc3/bin/changelog.py

install-hooks:
	git config core.hooksPath .githooks
	chmod +x .githooks/commit-msg dc3/bin/commit_msg_lint.py dc3/bin/changelog.py

deploy: package
	cd dc3-api \
	&& $(MVN_SUB) clean deploy -P deploy \
	&& cd ../dc3-common \
	&& $(MVN_SUB) clean deploy -P deploy
