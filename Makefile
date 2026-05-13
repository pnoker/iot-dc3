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
DOCKER_COMPOSE ?= docker compose

.PHONY: \
	help \
	install \
	dev \
	dev-prod \
	preview \
	build \
	build-tauri \
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
	test-e2e-sweep \
	clean \
	ci \
	docker-build \
	docker-up \
	docker-down

help:
	@printf '%s\n' \
		'make install      - install dependencies' \
		'make dev          - run Vite dev server (dev mode)' \
		'make dev-prod     - run Vite dev server (production mode)' \
		'make preview      - preview built assets' \
		'make build        - build web assets' \
		'make build-tauri  - build Tauri app' \
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
		'make test-e2e-sweep - run browser sweep against a full environment' \
		'make clean        - remove dist output' \
		'make ci           - run lint-check, check, guardrails, test, build' \
		'make docker-build - build dc3 docker services' \
		'make docker-up    - start dc3 docker services' \
		'make docker-down  - stop dc3 docker services'

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

build-tauri:
	$(PNPM) run build:tauri

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

test-e2e-sweep:
	$(PNPM) run test:e2e:sweep

clean:
	$(PNPM) run clean

ci: lint-check check test-guard test-ci build

docker-build:
	cd $(DC3_DIR) && $(DOCKER_COMPOSE) build

docker-up:
	cd $(DC3_DIR) && $(DOCKER_COMPOSE) up -d

docker-down:
	cd $(DC3_DIR) && $(DOCKER_COMPOSE) down
