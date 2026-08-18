#!/usr/bin/env bash
#
# Builds and pushes the dependency images that the release CI does NOT publish
# (dc3-postgres, dc3-rabbitmq). Kubernetes and Swarm need them in a registry
# reachable by every node.
#
# Usage:
#   DC3_IMAGE_REGISTRY=my.registry/dc3 DC3_IMAGE_TAG=2026.6 ./scripts/push-images.sh
#   # single-node clusters (kind/k3s/docker-desktop): load into the node instead:
#   kind load docker-image pnoker/dc3-postgres:2026.6 pnoker/dc3-rabbitmq:2026.6
#
set -euo pipefail
REGISTRY="${DC3_IMAGE_REGISTRY:-pnoker}"
TAG="${DC3_IMAGE_TAG:-2026.6}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"

for image in dc3-postgres dc3-rabbitmq; do
  docker build -t "${REGISTRY}/${image}:${TAG}" "${ROOT}/dc3/dependencies/${image}"
  echo "built ${REGISTRY}/${image}:${TAG}"
done

if [[ "$REGISTRY" == "pnoker" ]]; then
  echo "WARNING: pnoker is the public Docker Hub namespace - pushing there requires"
  echo "permissions. Push to your own registry instead:"
  echo "  DC3_IMAGE_REGISTRY=my.registry/dc3 ./scripts/push-images.sh"
else
  docker push "${REGISTRY}/dc3-postgres:${TAG}"
  docker push "${REGISTRY}/dc3-rabbitmq:${TAG}"
  echo "pushed ${REGISTRY}/dc3-postgres:${TAG} and dc3-rabbitmq:${TAG}"
fi
