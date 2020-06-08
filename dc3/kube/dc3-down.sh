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

function delete_namespace() {

  kubectl delete -f "${DC3_ROOT}/namespace/dc3-namespace.yaml"

}

function delete_ingress() {

  kubectl delete -f "${DC3_ROOT}/ingress/dc3-ingress.yaml"

}

function delete_network() {

  kubectl delete -f "${DC3_ROOT}/network/dc3-networkpolicy.yaml"

}

function delete_services() {

  #kubectl delete -f "${DC3_ROOT}/services/dc3-opcua-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-rtmp-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-resource-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-nginx-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-manager-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-data-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-auth-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-gateway-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-monitor-service.yaml"
  kubectl delete -f "${DC3_ROOT}/services/dc3-register-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-rabbitmq-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-redis-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-mongo-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-mysql-service.yaml"

}

function delete_deployments() {

  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-opcua-deployment.yaml"
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-rtmp-deployment.yaml"
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-resource-deployment.yaml"
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-nginx-deployment.yaml"
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-manager-deployment.yaml"
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-auth-deployment.yaml"
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-gateway-deployment.yaml"
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-monitor-deployment.yaml"
  kubectl delete -f "${DC3_ROOT}/deployments/dc3-register-deployment.yaml"
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-rabbitmq-deployment.yaml"
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-redis-deployment.yaml"
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-mongo-deployment.yaml"
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-mysql-deployment.yaml"

}

echo "Deleting DC3 ingress now!"
delete_ingress
echo "DC3 ingress deleted successfully !"

echo "Deleting DC3 deployments now!"
delete_deployments
echo "DC3 deployments deleted successfully!"

echo "Deleting DC3 services now!"
delete_services
echo "DC3 services deleted successfully !"

echo "Deleting DC3 network now!"
delete_network
echo "DC3 network deleted successfully !"

echo "Deleting DC3 namespace now!"
delete_namespace
echo "DC3 namespace deleted successfully !"
