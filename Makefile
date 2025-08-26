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

.PHONY: help

help:
	echo 'You can use make to execute the following commands:' \
	&& echo 'Usage: make [clean | package | deploy | dev | tag]' \
	&& echo ' - make tag: git tag' \
	&& echo ' - make dev: run local development environment' \
	&& echo ' - make build: build images' \
	&& echo ' - make deploy: mvn deploy'

tag:
	dc3/bin/tag.sh

dev:
	cd dc3 \
	&& docker-compose -f docker-compose-dev.yml up -d \

build:
	mvn clean package \
    && cd dc3 \
	&& docker-compose build \

deploy:
	mvn clean package \
    && cd dc3-api \
	&& mvn clean deploy -p deploy \
	&& cd ../dc3-common \
    && mvn clean deploy -p deploy