#!/bin/bash

#set -e

DC3_ROOT=$(dirname "${BASH_SOURCE}")/

function delete_namespace {

  kubectl delete -f "${DC3_ROOT}/namespace/dc3-namespace.yaml"

}

function delete_network {

  kubectl delete -f "${DC3_ROOT}/network/dc3-networkpolicy.yaml"

}

function delete_services {

  kubectl delete -f "${DC3_ROOT}/services/dc3-register-service.yaml"
  kubectl delete -f "${DC3_ROOT}/services/dc3-monitor-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-auth-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-gateway-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-manager-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-mongo-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-mysql-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-nginx-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-opcua-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-portainer-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-rabbitmq-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-redis-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-resource-service.yaml"
  #kubectl delete -f "${DC3_ROOT}/services/dc3-rtmp-service.yaml"

}

function delete_deployments {

  kubectl delete -f "${DC3_ROOT}/deployments/dc3-register-deployment.yaml"
  sleep 10
  kubectl delete -f "${DC3_ROOT}/deployments/dc3-monitor-deployment.yaml"
  sleep 10
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-auth-deployment.yaml"
  #sleep 10
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-gateway-deployment.yaml"
  #sleep 10
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-manager-deployment.yaml"
  #sleep 10
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-mongo-deployment.yaml"
  #sleep 10
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-mysql-deployment.yaml"
  #sleep 10
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-nginx-deployment.yaml"
  #sleep 10
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-opcua-deployment.yaml"
  #sleep 10
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-portainer-client-deployment.yaml"
  #sleep 10
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-rabbitmq-distro-deployment.yaml"
  #sleep 10
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-redis-deployment.yaml"
  #sleep 10
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-resource-deployment.yaml"
  #sleep 10
  #kubectl delete -f "${DC3_ROOT}/deployments/dc3-rtmp-deployment.yaml"

}

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