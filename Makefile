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

.PHONY: help clean package dev build deploy tag

COMPOSE_FILE := docker-compose-dev.yml
COMPOSE := podman compose
MVN_SETTINGS := .mvn/settings.xml
MVN := mvn -s $(MVN_SETTINGS)
MVN_SUB := mvn -s ../$(MVN_SETTINGS)

help:
	echo 'You can use make to execute the following commands:' \
	&& echo 'Usage: make [help | clean | package | dev | build | deploy | tag]' \
	&& echo ' - make clean: clean Maven build artifacts' \
	&& echo ' - make package: package all modules with Maven' \
	&& echo ' - make tag: git tag' \
	&& echo ' - make dev: run local development environment with podman compose' \
	&& echo ' - make build: build images with podman compose' \
	&& echo ' - make deploy: deploy with mvn -s .mvn/settings.xml'

clean:
	$(MVN) clean

package:
	$(MVN) clean package

tag:
	dc3/bin/tag.sh

dev:
	$(COMPOSE) -f dc3/$(COMPOSE_FILE) up -d

build: package
	$(COMPOSE) -f dc3/$(COMPOSE_FILE) build

deploy: package
	cd dc3-api \
	&& $(MVN_SUB) clean deploy -p deploy \
	&& cd ../dc3-common \
	&& $(MVN_SUB) clean deploy -p deploy
