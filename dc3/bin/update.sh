#!/bin/bash

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

set -e

cd ../../
git fetch --all
git reset --hard origin/pre_main
git pull

cd dc3-api
git fetch --all
git reset --hard origin/pre_main
git pull

cd ../dc3-common
git fetch --all
git reset --hard origin/pre_main
git pull

cd ../
docker build -t registry.cn-beijing.aliyuncs.com/dc3/dc3:2024.2.0.dev .

cd dc3/docker
docker build -t registry.cn-beijing.aliyuncs.com/dc3/dc3-gateway:2024.2.0.dev -f Dockerfile.gateway .
docker build -t registry.cn-beijing.aliyuncs.com/dc3/dc3-center-auth:2024.2.0.dev -f Dockerfile.auth .
docker build -t registry.cn-beijing.aliyuncs.com/dc3/dc3-center-data:2024.2.0.dev -f Dockerfile.data .
docker build -t registry.cn-beijing.aliyuncs.com/dc3/dc3-center-manager:2024.2.0.dev -f Dockerfile.manager .
docker build -t registry.cn-beijing.aliyuncs.com/dc3/dc3-center-ekuiper:2024.2.0.dev -f Dockerfile.ekuiper .
docker build -t registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-virtual:2024.2.0.dev -f Dockerfile.virtual .
docker build -t registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-modbus-tcp:2024.2.0.dev -f Dockerfile.modbus-tcp .
docker build -t registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-mqtt:2024.2.0.dev -f Dockerfile.mqtt .

cd ../
docker-compose -f docker-compose-test.yml up -d auth manager data gateway ekuiper
echo 'Waiting for 30 second'
sleep 30s
docker-compose -f docker-compose-test.yml up -d virtual modbus-tcp mqtt