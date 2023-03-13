#
# Copyright 2016-present the original author or authors.
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
# make -f ./Makefile dev
# make -f ./Makefile deploy
# make -f ./Makefile package
# make -f ./Makefile clean

.PHONY: build clean

help:
	echo 'You can use make to execute the following commands:' \
	&& echo 'Usage: make [clean | package | deploy | dev | tag]' \
	&& echo ' - make demo: build & push demo images' \
	&& echo ' - make dev: run local development environment' \
	&& echo ' - make clean: mvn clean' \
	&& echo ' - make package: mvn package' \
	&& echo ' - make build: build images' \
	&& echo ' - make deploy.N: mvn deploy N' \
	&& echo ' - make tag: git tag' \

demo:
	mvn clean package \
	&& cd dc3 \
	&& docker-compose -f docker-compose-demo.yml build \
	&& docker-compose -f docker-compose-demo.yml push \

dev:
	cd dc3 \
	&& docker-compose -f docker-compose-dev.yml up -d \

clean:
	mvn clean \

package:
	mvn package \

build:
	mvn clean package \
    && cd dc3 \
	&& docker-compose build \

deploy.api:
	cd dc3-api \
	&& mvn clean deploy \

deploy.common:
	cd dc3-common \
	&& mvn clean deploy \

deploy.sdk:
	cd dc3-driver-sdk \
	&& mvn clean deploy \