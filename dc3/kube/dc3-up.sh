#!/bin/bash

#
#  Copyright 2018-2020 Pnoker. All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

set -e

DC3_ROOT=$(dirname "${BASH_SOURCE}")/

function create_namespace() {

  kubectl apply -f "${DC3_ROOT}/namespace/dc3-namespace.yaml"

}

function create_ingress() {

  kubectl apply -f "${DC3_ROOT}/ingress/dc3-ingress.yaml"

}

function create_network() {

  kubectl apply -f "${DC3_ROOT}/network/dc3-networkpolicy.yaml"

}

function create_services() {

  #kubectl apply -f "${DC3_ROOT}/services/dc3-mysql-service.yaml"
  #kubectl apply -f "${DC3_ROOT}/services/dc3-redis-service.yaml"
  #kubectl apply -f "${DC3_ROOT}/services/dc3-mongo-service.yaml"
  #kubectl apply -f "${DC3_ROOT}/services/dc3-rabbitmq-service.yaml"
  #kubectl apply -f "${DC3_ROOT}/services/dc3-nginx-service.yaml"
  kubectl apply -f "${DC3_ROOT}/services/dc3-register-service.yaml"
  #kubectl apply -f "${DC3_ROOT}/services/dc3-monitor-service.yaml"
  #kubectl apply -f "${DC3_ROOT}/services/dc3-auth-service.yaml"
  #kubectl apply -f "${DC3_ROOT}/services/dc3-gateway-service.yaml"
  #kubectl apply -f "${DC3_ROOT}/services/dc3-data-service.yaml"
  #kubectl apply -f "${DC3_ROOT}/services/dc3-manager-service.yaml"
  #kubectl apply -f "${DC3_ROOT}/services/dc3-resource-service.yaml"
  #kubectl apply -f "${DC3_ROOT}/services/dc3-rtmp-service.yaml"
  #kubectl apply -f "${DC3_ROOT}/services/dc3-opcua-service.yaml"

}

function create_deployments() {

  #kubectl apply -f "${DC3_ROOT}/deployments/dc3-mysql-deployment.yaml"
  #kubectl apply -f "${DC3_ROOT}/deployments/dc3-redis-deployment.yaml"
  #kubectl apply -f "${DC3_ROOT}/deployments/dc3-mongo-deployment.yaml"
  #kubectl apply -f "${DC3_ROOT}/deployments/dc3-rabbitmq-deployment.yaml"
  #kubectl apply -f "${DC3_ROOT}/deployments/dc3-nginx-deployment.yaml"
  kubectl apply -f "${DC3_ROOT}/deployments/dc3-register-deployment.yaml"
  #kubectl apply -f "${DC3_ROOT}/deployments/dc3-monitor-deployment.yaml"
  #kubectl apply -f "${DC3_ROOT}/deployments/dc3-auth-deployment.yaml"
  #kubectl apply -f "${DC3_ROOT}/deployments/dc3-gateway-deployment.yaml"
  #kubectl apply -f "${DC3_ROOT}/deployments/dc3-manager-deployment.yaml"
  #kubectl apply -f "${DC3_ROOT}/deployments/dc3-resource-deployment.yaml"
  #kubectl apply -f "${DC3_ROOT}/deployments/dc3-rtmp-deployment.yaml"
  #kubectl apply -f "${DC3_ROOT}/deployments/dc3-opcua-deployment.yaml"

}

echo "Creating DC3 namespace now!"
create_namespace
echo "DC3 namespace created successfully!"

echo "Creating DC3 network now!"
create_network
echo "DC3 network created successfully!"

echo "Creating DC3 services now!"
create_services
echo "DC3 services created successfully !"

echo "Creating DC3 deployments now!"
create_deployments
echo "DC3 deployments created successfully!"

echo "Creating DC3 ingress now!"
create_ingress
echo "DC3 ingress created successfully!"
