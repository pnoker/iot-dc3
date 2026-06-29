#
# Copyright 2016-present the IoT DC3 original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

SHELL := /usr/bin/bash
.DEFAULT_GOAL := help

PNPM ?= pnpm
DC3_DIR ?= dc3
COMPOSE_FILE ?= $(DC3_DIR)/docker-compose.yml
ENV_FILE ?= $(firstword $(wildcard .env) .env.example)

ifneq ($(wildcard $(ENV_FILE)),)
include $(ENV_FILE)
endif
export

DOCKER_COMPOSE ?= $(shell if docker compose version >/dev/null 2>&1; then printf 'docker compose'; elif podman compose version >/dev/null 2>&1; then printf 'podman compose'; else printf 'docker compose'; fi)
REGISTRY ?= auto
DC3_WEB_VERSION ?= latest
DC3_BIND_HOST ?= 127.0.0.1
DC3_WEB_HTTP_PORT ?= 8080
DC3_WEB_HTTPS_PORT ?= 8443
APP_API_HOST ?= dc3-gateway
APP_API_PORT ?= 8000
DC3_LOG_MAX_SIZE ?= 20m
DC3_LOG_MAX_FILE ?= 20

ifeq ($(REGISTRY),auto)
DC3_WEB_IMAGE ?= pnoker/dc3-web
else ifeq ($(REGISTRY),global)
override DC3_WEB_IMAGE := pnoker/dc3-web
else ifeq ($(REGISTRY),cn)
override DC3_WEB_IMAGE := registry.cn-beijing.aliyuncs.com/dc3/dc3-web
else
$(error Unsupported REGISTRY '$(REGISTRY)'. Use REGISTRY=auto|global|cn)
endif

define dc3_compose
$(DOCKER_COMPOSE) -f "$(COMPOSE_FILE)"
endef

.PHONY: \
	help \
	env \
	init-env \
	install \
	dev \
	dev-prod \
	preview \
	build \
	check \
	type-check \
	lint \
	lint-check \
	format \
	format-check \
	test \
	test-unit \
	test-api \
	test-component \
	test-guard \
	test-impact \
	test-ci \
	test-e2e \
	test-e2e-headed \
	test-e2e-sweep \
	test-e2e-sweep-headed \
	clean \
	ci \
	docker-build \
	docker-up \
	docker-down \
	docker-ps \
	docker-logs \
	docker-config \
	docker-restart \
	tag

help:
	@printf '%s\n' \
		'make install      - install dependencies' \
		'make dev          - run Vite dev server (dev mode)' \
		'make dev-prod     - run Vite dev server (production mode)' \
		'make preview      - preview built assets' \
		'make build        - build web assets' \
		'make check        - run vue-tsc type check' \
		'make type-check   - run vue-tsc type check' \
		'make lint         - run eslint --fix and prettier --write' \
		'make lint-check   - run eslint/prettier checks only' \
		'make format       - run prettier --write' \
		'make format-check - run prettier --check' \
		'make test         - run Vitest unit/api/component tests' \
		'make test-unit    - run Vitest unit tests' \
		'make test-api     - run API contract tests' \
		'make test-component - run component tests' \
		'make test-guard   - run AI coding guardrail tests' \
		'make test-impact  - print recommended checks for changed files' \
		'make test-ci      - run Vitest with coverage thresholds' \
		'make test-e2e     - run Playwright e2e tests' \
		'make test-e2e-headed - run Playwright e2e tests in a visible browser' \
		'make test-e2e-sweep - run browser sweep against a full environment' \
		'make test-e2e-sweep-headed - run browser sweep in a visible browser' \
		'make clean        - remove dist output' \
		'make ci           - run lint-check, check, guardrails, test, build' \
		'make docker-build - build dc3 docker services' \
		'make docker-up    - start dc3 docker services' \
		'make docker-down  - stop dc3 docker services' \
		'make docker-logs  - follow dc3 web logs' \
		'make env          - print effective Make/Compose defaults' \
		'make tag [patch|minor|major] - create semver release tag (delegates to root)'

env:
	@printf 'ENV_FILE=%s\n' "$(ENV_FILE)"
	@printf 'DOCKER_COMPOSE=%s\n' "$(DOCKER_COMPOSE)"
	@printf 'COMPOSE_FILE=%s\n' "$(COMPOSE_FILE)"
	@printf 'REGISTRY=%s\n' "$(REGISTRY)"
	@printf 'DC3_WEB_IMAGE=%s\n' "$(DC3_WEB_IMAGE)"
	@printf 'DC3_WEB_VERSION=%s\n' "$(DC3_WEB_VERSION)"
	@printf 'DC3_BIND_HOST=%s\n' "$(DC3_BIND_HOST)"
	@printf 'DC3_WEB_HTTP_PORT=%s\n' "$(DC3_WEB_HTTP_PORT)"
	@printf 'DC3_WEB_HTTPS_PORT=%s\n' "$(DC3_WEB_HTTPS_PORT)"
	@printf 'APP_API_HOST=%s\n' "$(APP_API_HOST)"
	@printf 'APP_API_PORT=%s\n' "$(APP_API_PORT)"

init-env:
	@test -f .env || cp .env.example .env
	@printf '%s\n' 'Using .env'

install:
	$(PNPM) install

dev:
	$(PNPM) dev

dev-prod:
	$(PNPM) run dev:prod

preview:
	$(PNPM) preview

build:
	$(PNPM) build

check:
	$(PNPM) run check

type-check:
	$(PNPM) run type-check

lint:
	$(PNPM) run lint

lint-check:
	$(PNPM) run lint:check

format:
	$(PNPM) run format

format-check:
	$(PNPM) run format:check

test:
	$(PNPM) test

test-unit:
	$(PNPM) run test:unit

test-api:
	$(PNPM) run test:api

test-component:
	$(PNPM) run test:component

test-guard:
	$(PNPM) run test:guard

test-impact:
	$(PNPM) run test:impact

test-ci:
	$(PNPM) run test:ci

test-e2e:
	$(PNPM) run test:e2e

test-e2e-headed:
	$(PNPM) run test:e2e:headed

test-e2e-sweep:
	$(PNPM) run test:e2e:sweep

test-e2e-sweep-headed:
	$(PNPM) run test:e2e:sweep:headed

clean:
	$(PNPM) run clean

ci: lint-check check test-guard test-ci build

docker-build:
	$(call dc3_compose) build

docker-up:
	$(call dc3_compose) up -d

docker-down:
	$(call dc3_compose) down

docker-ps:
	$(call dc3_compose) ps

docker-logs:
	$(call dc3_compose) logs -f --tail=200

docker-config:
	$(call dc3_compose) config

docker-restart:
	$(call dc3_compose) restart

tag:
	@$(MAKE) -C .. tag $(filter-out $@,$(MAKECMDGOALS))
%:
	@:
