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