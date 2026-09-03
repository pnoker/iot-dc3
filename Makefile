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

.PHONY: help env init-env install-node clean package test test-it test-e2e coverage deploy \
	build up stop down ps logs config pull restart refresh reset run \
	dev dev-auth dev-gateway dev-data dev-manager dev-agentic dev-web dev-cli \
	format format-java format-web format-cli lint validate-web-lint validate-cli-lint \
	check validate-quality validate-web-quality validate-cli-quality validate-cli-format \
	changelog openapi tag validate-annotations validate-logging validate-permissions validate-scripts validate-python \
	validate-postgres-init validate-schema-fingerprint sync-schema-fingerprint validate-r2dbc-migration \
	validate-documentation validate-javadoc validate-format-java validate-checkstyle validate-java-quality \
	stack-deploy stack-rm stack-ps k8s-apply k8s-delete helm-install helm-uninstall \
	release-backfill release-backfill-apply release-backfill-refresh

ENV_FILE ?= $(firstword $(wildcard .env) .env.example)
RUNTIME_ENV_FILE ?= dc3/env/dev.env

ifneq ($(wildcard $(RUNTIME_ENV_FILE)),)
include $(RUNTIME_ENV_FILE)
endif
ifneq ($(wildcard $(ENV_FILE)),)
include $(ENV_FILE)
endif
export

COMPOSE ?= $(shell if docker compose version >/dev/null 2>&1; then printf 'docker compose'; elif podman compose version >/dev/null 2>&1; then printf 'podman compose'; else printf 'docker compose'; fi)
COMPOSE_DIR ?= dc3
REGISTRY ?= auto
STACK ?= dev
SERVICES ?=
GROUP ?=

GROUP_SERVICES_center := auth manager data agentic
GROUP_SERVICES_core := $(GROUP_SERVICES_center) gateway
GROUP_SERVICES_drivers := $(patsubst dc3-driver/dc3-driver-%,%,$(sort $(wildcard dc3-driver/dc3-driver-*)))
SELECTED_SERVICES := $(strip $(SERVICES) $(GROUP_SERVICES_$(GROUP)))

RUN_SERVICE ?= $(or $(SERVICE),gateway)
RUN_MODULE_gateway := dc3-gateway
RUN_MODULE_auth    := dc3-center/dc3-center-auth
RUN_MODULE_manager := dc3-center/dc3-center-manager
RUN_MODULE_data    := dc3-center/dc3-center-data
RUN_MODULE_agentic := dc3-center/dc3-center-agentic
$(foreach d,$(GROUP_SERVICES_drivers),$(eval RUN_MODULE_$(d) := dc3-driver/dc3-driver-$(d)))
RUN_MODULE := $(RUN_MODULE_$(RUN_SERVICE))

MVN_SETTINGS ?= .mvn/settings.xml
MVN_SETTINGS_ARG := $(if $(strip $(MVN_SETTINGS)),-s $(MVN_SETTINGS),)
MVN_SUB_SETTINGS_ARG := $(if $(strip $(MVN_SETTINGS)),-s ../$(MVN_SETTINGS),)
MVN := mvn $(MVN_SETTINGS_ARG)
MVN_SUB := mvn $(MVN_SUB_SETTINGS_ARG)
NODE ?= node
PNPM ?= corepack pnpm
PNPM_WEB := cd dc3-web && $(PNPM)
PNPM_CLI := cd dc3-cli && $(PNPM)

CHANGE_FILE ?= dc3/doc/CHANGE.md
FROM ?=
TO ?= HEAD
VERSION ?=

ifeq ($(REGISTRY),auto)
DC3_IMAGE_REGISTRY ?= pnoker
else ifeq ($(REGISTRY),global)
DEFAULT_IMAGE_REGISTRY := pnoker
override DC3_IMAGE_REGISTRY := $(DEFAULT_IMAGE_REGISTRY)
else ifeq ($(REGISTRY),cn)
DEFAULT_IMAGE_REGISTRY := registry.cn-beijing.aliyuncs.com/dc3
override DC3_IMAGE_REGISTRY := $(DEFAULT_IMAGE_REGISTRY)
else
$(error Unsupported REGISTRY '$(REGISTRY)'. Use REGISTRY=auto|global|cn)
endif

STACK_SUFFIX := $(if $(filter app,$(STACK)),,-$(STACK))

ifeq ($(origin COMPOSE_FILE), undefined)
RESOLVED_COMPOSE_FILE := $(COMPOSE_DIR)/docker-compose$(STACK_SUFFIX).yml
MAKE_COMPOSE_OVERRIDE :=
else
RESOLVED_COMPOSE_FILE := $(if $(findstring /,$(COMPOSE_FILE)),$(COMPOSE_FILE),$(COMPOSE_DIR)/$(COMPOSE_FILE))
MAKE_COMPOSE_OVERRIDE := COMPOSE_FILE="$(COMPOSE_FILE)"
endif

define dc3_compose
$(COMPOSE) -f "$(RESOLVED_COMPOSE_FILE)"
endef

help:
	@printf '%s\n' 'IoT DC3 Make targets'
	@printf '%s\n' ''
	@printf '%s\n' 'Common:'
	@printf '  %-24s %s\n' 'make install-node' 'Install Web and CLI dependencies from their independent lockfiles'
	@printf '  %-24s %s\n' 'make dev' 'Build and run auth, gateway, data, manager, and agentic'
	@printf '  %-24s %s\n' 'make dev-auth' 'Build and run one backend service; auth can be replaced by another service'
	@printf '  %-24s %s\n' 'make dev-web' 'Run the Web development server'
	@printf '  %-24s %s\n' 'make dev-cli' 'Run the CLI development watcher'
	@printf '  %-24s %s\n' 'make format' 'Format Java, Web, and CLI source'
	@printf '  %-24s %s\n' 'make lint' 'Check Java, Web, and CLI lint rules'
	@printf '  %-24s %s\n' 'make check' 'Run the complete non-mutating repository quality gate'
	@printf '  %-24s %s\n' 'make package' 'Build all Maven modules'
	@printf '  %-24s %s\n' 'make test' 'Run unit tests'
	@printf '  %-24s %s\n' 'make test-it' 'Run integration-test phase'
	@printf '  %-24s %s\n' 'make test-e2e' 'Run E2E harness'
	@printf '  %-24s %s\n' 'make coverage' 'Generate aggregated JaCoCo coverage'
	@printf '  %-24s %s\n' 'make validate-logging' 'Run backend and frontend logging policy gates'
	@printf '  %-24s %s\n' 'make validate-scripts' 'Validate Python, Node, and shell tools under dc3/bin'
	@printf '  %-24s %s\n' 'make validate-python' 'Compile dc3/bin Python tools for syntax validation'
	@printf '  %-24s %s\n' 'make validate-postgres-init' 'Validate PostgreSQL initialization schema comments and syntax policy'
	@printf '  %-24s %s\n' 'make validate-schema-fingerprint' 'Verify canonical clean-DDL fingerprints for PostgreSQL'
	@printf '  %-24s %s\n' 'make sync-schema-fingerprint' 'Rewrite embedded and distributed PostgreSQL fingerprints to the canonical value'
	@printf '  %-24s %s\n' 'make validate-r2dbc-migration' 'Fail when legacy relation persistence remains outside the JDBC allow-list'
	@printf '  %-24s %s\n' 'make validate-documentation' 'Validate documentation against executable project metadata'
	@printf '  %-24s %s\n' 'make validate-javadoc' 'Validate public Java API documentation and references'
	@printf '  %-24s %s\n' 'make format-java' 'Format Java source and add canonical copyright headers'
	@printf '  %-24s %s\n' 'make validate-format-java' 'Check Java formatting and copyright headers'
	@printf '  %-24s %s\n' 'make validate-checkstyle' 'Check Java naming and Javadoc structure'
	@printf '  %-24s %s\n' 'make validate-java-quality' 'Run Java formatting, Checkstyle, and Javadoc checks'
	@printf '  %-24s %s\n' 'make run SERVICE=auth' 'Run one Spring Boot service with env auto-loaded'
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
	@printf '%s\n' 'Shortcuts (auto-generated, no env vars needed):'
	@printf '  %-32s %s\n' 'make <op>-<stack>[-<registry>]' 'e.g. make up-db-cn, make logs-dev, make down-app'
	@printf '  %-32s %s\n' '  op' 'up down stop ps logs build pull restart refresh config reset'
	@printf '  %-32s %s\n' '  stack' 'dev app db optional'
	@printf '  %-32s %s\n' '  registry' 'cn global (only up/pull/build/refresh)'
	@printf '%s\n' ''
	@printf '%s\n' 'Variables:'
	@printf '  %-24s %s\n' 'STACK=dev|app|db|optional' 'Compose stack selector'
	@printf '  %-24s %s\n' 'REGISTRY=auto|global|cn' 'Image registry selector; auto uses .env/DC3_IMAGE_REGISTRY'
	@printf '  %-24s %s\n' 'SERVICES="..."' 'Compose service list, empty means all services'
	@printf '  %-24s %s\n' 'GROUP=center|core|drivers' 'Predefined service group'
	@printf '  %-24s %s\n' 'ENV_FILE=.env' 'Make/Compose env file, auto-falls back to .env.example'
	@printf '  %-24s %s\n' 'RUNTIME_ENV_FILE=...' 'Local source-run env file, defaults to dc3/env/dev.env'
	@printf '%s\n' ''
	@printf '%s\n' 'Examples:'
	@printf '  %s\n' 'make init-env'
	@printf '  %s\n' 'make up-db'
	@printf '  %s\n' 'make up-db-cn'
	@printf '  %s\n' 'make up SERVICES="agentic"'
	@printf '  %s\n' 'make up SERVICES="gateway agentic"'
	@printf '  %s\n' 'make up GROUP=core'
	@printf '  %s\n' 'make up STACK=optional SERVICES="prometheus grafana"'
	@printf '  %s\n' 'make logs SERVICES="gateway agentic"'
	@printf '%s\n' ''

env:
	@printf 'ENV_FILE=%s\n' "$(ENV_FILE)"
	@printf 'RUNTIME_ENV_FILE=%s\n' "$(RUNTIME_ENV_FILE)"
	@printf 'COMPOSE=%s\n' "$(COMPOSE)"
	@printf 'STACK=%s\n' "$(STACK)"
	@printf 'COMPOSE_FILE=%s\n' "$(RESOLVED_COMPOSE_FILE)"
	@printf 'REGISTRY=%s\n' "$(REGISTRY)"
	@printf 'DC3_IMAGE_REGISTRY=%s\n' "$(DC3_IMAGE_REGISTRY)"
	@printf 'DC3_IMAGE_TAG=%s\n' "$(DC3_IMAGE_TAG)"
	@printf 'POSTGRES=%s:%s/%s\n' "$(POSTGRES_HOST)" "$(POSTGRES_PORT)" "$(POSTGRES_DB)"
	@printf 'RABBITMQ=%s:%s/%s\n' "$(RABBITMQ_HOST)" "$(RABBITMQ_PORT)" "$(RABBITMQ_VIRTUAL_HOST)"

init-env:
	@test -f .env || cp .env.example .env
	@printf '%s\n' 'Using .env'

install-node:
	cd dc3-web && CI=true $(PNPM) install --frozen-lockfile
	cd dc3-cli && CI=true $(PNPM) install --frozen-lockfile

clean:
	$(MVN) clean

package:
	$(MVN) clean package

test:
	$(MVN) -B -Dmaven.test.skip=false test

validate-annotations:
	@echo "Running x-dc3-ai annotation gate (allowlisted service modules)..."
	$(MVN) -q -pl dc3-common/dc3-common-resource test -Dtest=ControllerAnnotationGateTest
	$(MVN) -q -pl dc3-common/dc3-common-manager -am test -Dtest=ManagerAnnotationGateTest -Dsurefire.failIfNoSpecifiedTests=false
	$(MVN) -q -pl dc3-common/dc3-common-data -am test -Dtest=DataAnnotationGateTest -Dsurefire.failIfNoSpecifiedTests=false
	$(MVN) -q -pl dc3-common/dc3-common-agentic -am test -Dtest=AgenticAnnotationGateTest -Dsurefire.failIfNoSpecifiedTests=false
	$(MVN) -q -pl dc3-common/dc3-common-auth -am test -Dtest=AuthAnnotationGateTest -Dsurefire.failIfNoSpecifiedTests=false
	@echo "Allowlisted: manager, data, agentic, auth (all center services)."

validate-logging:
	$(MVN) -q -pl dc3-common/dc3-common-log test -Dtest=LoggingPolicyTest,LogbackConfigurationTest
	$(PNPM_WEB) exec vitest run tests/guardrails/ai-guardrails.test.ts

validate-permissions:
	python3 dc3/bin/audit_controller_permissions.py

validate-python:
	python3 -m py_compile dc3/bin/*.py

validate-scripts: validate-python
	$(NODE) --check dc3/bin/dev.mjs
	bash -n dc3/bin/*.sh

validate-postgres-init:
	python3 dc3/bin/check_postgres_init.py

validate-schema-fingerprint:
	python3 dc3/bin/schema_fingerprint.py --check

sync-schema-fingerprint:
	python3 dc3/bin/schema_fingerprint.py --sync

validate-r2dbc-migration:
	python3 dc3/bin/check_r2dbc_migration.py

validate-documentation:
	python3 dc3/bin/check_documentation.py

format: format-java format-web format-cli

format-java:
	$(MVN) initialize spotless:apply

format-web:
	$(PNPM_WEB) lint

format-cli:
	$(PNPM_CLI) format
	$(PNPM_CLI) lint

lint: validate-checkstyle validate-web-lint validate-cli-lint

validate-web-lint:
	$(PNPM_WEB) lint:check

validate-cli-lint:
	$(PNPM_CLI) lint:check

validate-format-java:
	$(MVN) initialize spotless:check

validate-cli-format:
	$(PNPM_CLI) format:check

validate-checkstyle:
	$(MVN) -B -DskipTests checkstyle:check

validate-java-quality: validate-format-java validate-checkstyle validate-javadoc

validate-web-quality: validate-web-lint
	$(PNPM_WEB) check

validate-cli-quality: validate-cli-format validate-cli-lint
	$(PNPM_CLI) build

validate-quality: validate-scripts validate-java-quality validate-web-quality validate-cli-quality

check: validate-quality

validate-javadoc:
	$(MVN) -B -DskipTests -Dspotless.check.skip=true -Dcheckstyle.skip=true -pl dc3-common/dc3-common-public -am install
	$(MVN) -B -DskipTests -Ddoclint=all -Dmaven.javadoc.failOnWarnings=true \
		-pl dc3-common/dc3-common-constant,dc3-common/dc3-common-public javadoc:javadoc

test-it:
	$(MVN) -B -Dmaven.test.skip=false -Dskip.unit.tests=true -Dcoverage.check.skip=true verify

test-e2e:
	DC3_E2E=true $(MVN) -B -Dmaven.test.skip=false -Dskip.unit.tests=true -pl dc3-e2e -am -Pe2e verify

coverage:
	$(MVN) -B -Dmaven.test.skip=false -pl dc3-coverage -am verify

build:
	$(call dc3_compose) build $(SELECTED_SERVICES)

up:
	$(call dc3_compose) up -d $(SELECTED_SERVICES)

stop:
	$(call dc3_compose) stop $(SELECTED_SERVICES)

down:
	$(call dc3_compose) down

ps:
	$(call dc3_compose) ps $(SELECTED_SERVICES)

logs:
	$(call dc3_compose) logs -f --tail=200 $(SELECTED_SERVICES)

config:
	$(call dc3_compose) config

pull:
	$(call dc3_compose) pull $(SELECTED_SERVICES)

restart:
	$(call dc3_compose) restart $(SELECTED_SERVICES)

refresh:
	$(call dc3_compose) pull $(SELECTED_SERVICES)
	$(call dc3_compose) down
	$(call dc3_compose) up -d $(SELECTED_SERVICES)

reset:
	@test "$(CONFIRM_RESET_VOLUMES)" = "true" || (echo "Refusing to delete volumes. Re-run with CONFIRM_RESET_VOLUMES=true" && exit 1)
	$(call dc3_compose) down -v

run:
	$(MVN) -pl "$(RUN_MODULE)" -am spring-boot:run

dev:
	$(NODE) dc3/bin/dev.mjs all

dev-auth dev-gateway dev-data dev-manager dev-agentic:
	$(NODE) dc3/bin/dev.mjs $(patsubst dev-%,%,$@)

dev-web:
	$(PNPM_WEB) dev

dev-cli:
	$(PNPM_CLI) dev

# Auto-generated compose shortcuts: <op>-<stack>[-<registry>]
#   op       : up down stop ps logs build pull restart refresh config reset
#   stack    : dev app db optional
#   registry : cn global   (only up/pull/build/refresh)
# Each shortcut recurses into the base op with STACK/REGISTRY set, so all
# existing logic (compose-file resolution, reset confirmation, SERVICES/GROUP
# filtering) is reused. Examples: make up-db-cn  make logs-dev  make down-app
COMPOSE_OPS          := up down stop ps logs build pull restart refresh config reset
COMPOSE_REGISTRY_OPS := up pull build refresh
COMPOSE_STACKS       := dev app db optional scale
COMPOSE_REGISTRIES   := cn global

define dc3_stack_target
.PHONY: $(1)-$(2)
$(1)-$(2):
	@$$(MAKE) $(1) STACK=$(2) SERVICES='$$(SERVICES)' GROUP='$$(GROUP)' REGISTRY='$$(REGISTRY)' COMPOSE='$$(COMPOSE)' COMPOSE_DIR='$$(COMPOSE_DIR)' $$(MAKE_COMPOSE_OVERRIDE)
endef

define dc3_stack_registry_target
.PHONY: $(1)-$(2)-$(3)
$(1)-$(2)-$(3):
	@$$(MAKE) $(1) STACK=$(2) SERVICES='$$(SERVICES)' GROUP='$$(GROUP)' REGISTRY=$(3) COMPOSE='$$(COMPOSE)' COMPOSE_DIR='$$(COMPOSE_DIR)' $$(MAKE_COMPOSE_OVERRIDE)
endef

$(foreach op,$(COMPOSE_OPS),$(foreach st,$(COMPOSE_STACKS),$(eval $(call dc3_stack_target,$(op),$(st)))))
$(foreach op,$(COMPOSE_REGISTRY_OPS),$(foreach st,$(COMPOSE_STACKS),$(foreach rg,$(COMPOSE_REGISTRIES),$(eval $(call dc3_stack_registry_target,$(op),$(st),$(rg))))))

deploy: package
	cd dc3-api \
	&& $(MVN_SUB) clean deploy -P deploy \
	&& cd ../dc3-common \
	&& $(MVN_SUB) clean deploy -P deploy \
	&& cd ../dc3-db \
	&& $(MVN_SUB) clean deploy -P deploy \
	&& cd ../dc3-mq \
	&& $(MVN_SUB) clean deploy -P deploy \
	&& cd ../dc3-tsdb \
	&& $(MVN_SUB) clean deploy -P deploy

tag:
	@dc3/bin/tag.sh

# ----------------------------------------------------------------------------
# Orchestrator deployments (Docker Swarm / Kubernetes / Helm)
# ----------------------------------------------------------------------------
# Swarm: dc3/docker-compose-swarm.yml is a full self-contained stack. Dependency
# images (dc3-postgres/dc3-rabbitmq) must be built and pushed first, see
# dc3/doc/DEPLOYMENT.md section 2.
STACK_CMD ?= docker stack
STACK_NAME ?= dc3

.PHONY: stack-deploy stack-rm stack-ps
stack-deploy:
	$(STACK_CMD) deploy -c dc3/docker-compose-swarm.yml $(STACK_NAME)

stack-rm:
	$(STACK_CMD) rm $(STACK_NAME)

stack-ps:
	$(STACK_CMD) services $(STACK_NAME)

# Kubernetes (kustomize) - copy dc3/deploy/k8s/secret.env.example to
# dc3/deploy/k8s/secret.env and edit it first.
K8S_DIR ?= dc3/deploy/k8s

.PHONY: k8s-apply k8s-delete
k8s-apply:
	kubectl apply -k $(K8S_DIR)

k8s-delete:
	kubectl delete -k $(K8S_DIR)

# Helm - dc3/deploy/helm/dc3 (see chart README for values).
HELM_RELEASE ?= dc3
HELM_CHART ?= dc3/deploy/helm/dc3
HELM_VALUES ?= $(HELM_CHART)/values-production.yaml

.PHONY: helm-install helm-uninstall
helm-install:
	helm upgrade --install $(HELM_RELEASE) $(HELM_CHART) -f $(HELM_VALUES)

helm-uninstall:
	helm uninstall $(HELM_RELEASE)

# Reject unknown targets instead of silently succeeding.
%:
	@echo "Unknown target '$@'. Run 'make help' for available targets." && exit 1

changelog:
	@FROM="$(FROM)" TO="$(TO)" VERSION="$(VERSION)" CHANGE_FILE="$(CHANGE_FILE)" dc3/bin/changelog.py

# Backfill GitHub Releases for CHANGE.md versions that were never released.
# Dry-run lists the gap; release-backfill-apply creates them through the API
# (tags created via API do not trigger the Docker Images workflow, and the
# 'latest' pointer is never moved). Requires gh authenticated.
release-backfill:
	python3 dc3/bin/release_backfill.py

release-backfill-apply:
	python3 dc3/bin/release_backfill.py --apply

# Re-render the release bodies of backfilled releases (e.g. after TITLE.md or
# RELEASE-FOOTER.md evolves). Never touches tags, targets, or the 'latest'
# pointer; CI-published releases (dc3.release.* tags) are left alone.
release-backfill-refresh:
	python3 dc3/bin/release_backfill.py --refresh --apply

# Export each center's OpenAPI JSON from a running stack (dev/test profile).
# OPENAPI_BASE overrides the gateway URL; OPENAPI_OUT the output directory.
openapi:
	@OPENAPI_BASE="$(OPENAPI_BASE)" dc3/bin/export_openapi.sh $(OPENAPI_OUT)
