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
		'make clean        - remove dist output' \
		'make ci           - run lint-check, check, build' \
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

clean:
	$(PNPM) run clean

ci: lint-check check build

docker-build:
	cd $(DC3_DIR) && $(DOCKER_COMPOSE) build

docker-up:
	cd $(DC3_DIR) && $(DOCKER_COMPOSE) up -d

docker-down:
	cd $(DC3_DIR) && $(DOCKER_COMPOSE) down
