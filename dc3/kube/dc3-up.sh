#!/bin/bash

set -e

DC3_ROOT=$(dirname "${BASH_SOURCE}")/

function create_services {

  kubectl create -f "${DC3_ROOT}/services/dc3-register-service.yaml"
  kubectl create -f "${DC3_ROOT}/services/dc3-monitor-service.yaml"
  #kubectl create -f "${DC3_ROOT}/services/dc3-auth-service.yaml"
  #kubectl create -f "${DC3_ROOT}/services/dc3-gateway-service.yaml"
  #kubectl create -f "${DC3_ROOT}/services/dc3-manager-service.yaml"
  #kubectl create -f "${DC3_ROOT}/services/dc3-mongo-service.yaml"
  #kubectl create -f "${DC3_ROOT}/services/dc3-mysql-service.yaml"
  #kubectl create -f "${DC3_ROOT}/services/dc3-nginx-service.yaml"
  #kubectl create -f "${DC3_ROOT}/services/dc3-opcua-service.yaml"
  #kubectl create -f "${DC3_ROOT}/services/dc3-portainer-service.yaml"
  #kubectl create -f "${DC3_ROOT}/services/dc3-rabbitmq-service.yaml"
  #kubectl create -f "${DC3_ROOT}/services/dc3-redis-service.yaml"
  #kubectl create -f "${DC3_ROOT}/services/dc3-resource-service.yaml"
  #kubectl create -f "${DC3_ROOT}/services/dc3-rtmp-service.yaml"

}

function create_deployments {

  kubectl create -f "${DC3_ROOT}/deployments/dc3-register-deployment.yaml"
  sleep 10
  kubectl create -f "${DC3_ROOT}/deployments/dc3-monitor-deployment.yaml"
  sleep 10
  #kubectl create -f "${DC3_ROOT}/deployments/dc3-auth-deployment.yaml"
  #sleep 10
  #kubectl create -f "${DC3_ROOT}/deployments/dc3-gateway-deployment.yaml"
  #sleep 10
  #kubectl create -f "${DC3_ROOT}/deployments/dc3-manager-deployment.yaml"
  #sleep 10
  #kubectl create -f "${DC3_ROOT}/deployments/dc3-mongo-deployment.yaml"
  #sleep 10
  #kubectl create -f "${DC3_ROOT}/deployments/dc3-mysql-deployment.yaml"
  #sleep 10
  #kubectl create -f "${DC3_ROOT}/deployments/dc3-nginx-deployment.yaml"
  #sleep 10
  #kubectl create -f "${DC3_ROOT}/deployments/dc3-opcua-deployment.yaml"
  #sleep 10
  #kubectl create -f "${DC3_ROOT}/deployments/dc3-portainer-client-deployment.yaml"
  #sleep 10
  #kubectl create -f "${DC3_ROOT}/deployments/dc3-rabbitmq-distro-deployment.yaml"
  #sleep 10
  #kubectl create -f "${DC3_ROOT}/deployments/dc3-redis-deployment.yaml"
  #sleep 10
  #kubectl create -f "${DC3_ROOT}/deployments/dc3-resource-deployment.yaml"
  #sleep 10
  #kubectl create -f "${DC3_ROOT}/deployments/dc3-rtmp-deployment.yaml"

}

echo "Creating DC3 services now!"
create_services
echo "DC3 services created successfully !"

echo "Creating DC3 deployments now!"
create_deployments
echo "DC3 deployments created successfully!"

