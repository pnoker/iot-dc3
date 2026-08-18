# IoT DC3 on Kubernetes

Production-oriented Kubernetes manifests for the IoT DC3 platform: gateway, the four
centers (auth / manager / data / agentic), the web console, 25 protocol drivers, and
the PostgreSQL + RabbitMQ stateful dependencies.

| What            | Where                                                              |
|-----------------|--------------------------------------------------------------------|
| Compose stacks  | `dc3/docker-compose*.yml` (single host, scale, swarm)            |
| Kubernetes      | this directory (`kustomize`)                                       |
| Helm            | `../helm/dc3` (equivalent chart)                                   |
| Deployment doc  | `dc3/doc/DEPLOYMENT.md`                                            |

## Layout

```text
dc3/deploy/k8s/
├── kustomization.yaml          # single source of truth: resources + secret + image tags
├── namespace.yaml              # namespace "dc3"
├── configmap.yaml              # non-secret runtime environment (dc3-config)
├── secret.env(.example)        # kustomize secretGenerator input -> Secret dc3-secrets
├── postgres/ rabbitmq/         # StatefulSets + headless Services (singleton, PVC-backed)
├── auth/ manager/ data/ agentic/ gateway/ web/   # Deployment + ClusterIP Service each
│   └── agentic/pvc.yaml        # persistent attachment storage
├── drivers/                    # one Deployment per protocol driver (generated)
│   ├── template.yaml + list.txt + generate.sh
│   └── service-listening-virtual.yaml   # NodePort for inbound device TCP/UDP
├── ingress.yaml                # nginx-ingress: /api/ -> gateway, / -> web, TLS
├── hpa.yaml                    # gateway + web CPU autoscaling
├── pdb.yaml                    # PodDisruptionBudget for the stateless tier
└── scripts/push-images.sh      # build+push the dependency images CI does not publish
```

## Prerequisites

- Kubernetes >= 1.25, an ingress controller (the bundled `ingress.yaml` targets
  [ingress-nginx](https://kubernetes.github.io/ingress-nginx/)), and a StorageClass
  that provisions volumes (the default class is used by the PVCs).
- The release CI publishes all `pnoker/dc3-*` **app** images to Docker Hub and Aliyun.
  The **dependency** images `pnoker/dc3-postgres` and `pnoker/dc3-rabbitmq` are *not*
  published - build and push them once (see next step).

## Install

```bash
cd iot-dc3

# 1. Build and push the dependency images (or kind load them on single-node clusters)
DC3_IMAGE_REGISTRY=my.registry/dc3 ./dc3/deploy/k8s/scripts/push-images.sh
# then point the manifests at your registry:
kustomize edit set image pnoker/dc3-postgres=my.registry/dc3/dc3-postgres
kustomize edit set image pnoker/dc3-rabbitmq=my.registry/dc3/dc3-rabbitmq

# 2. Secrets - start from the weak public defaults and replace them before production
cp dc3/deploy/k8s/secret.env.example dc3/deploy/k8s/secret.env
$EDITOR dc3/deploy/k8s/secret.env        # DC3_SECURITY_KEY / AUTH_HMAC_SECRET / passwords

# 3. Apply everything
kubectl apply -k dc3/deploy/k8s

# 4. Watch it come up
kubectl -n dc3 get pods -w
kubectl -n dc3 get svc,ingress
```

> Tip: change the image tag or registry in one place with
> `kustomize edit set image pnoker/dc3-gateway=my.registry/dc3/dc3-gateway:2026.7`
> (repeat per image, or edit the `images:` block in `kustomization.yaml`).

## Access

- **Web console**: `https://<ingress-host>/` - set `spec.rules[0].host` in
  `ingress.yaml` (default placeholder `dc3.example.com`) and provision the
  `dc3-tls` secret (cert-manager + ClusterIssuer, or your CA).
- **API**: `https://<ingress-host>/api/` terminates at `dc3-gateway:8000`.
- **Device ingress** (`listening-virtual`): NodePort `30670` (TCP) / `30671` (UDP)
  on any worker node, or change `service-listening-virtual.yaml` to `type: LoadBalancer`.

## Scaling

```bash
kubectl -n dc3 scale deployment dc3-gateway --replicas=3
kubectl -n dc3 scale deployment dc3-driver-modbus-tcp --replicas=2   # stateless drivers
kubectl -n dc3 get hpa -n dc3                                        # gateway/web autoscale
```

Scaling semantics (same as the compose-scale stack, see `dc3/doc/DEPLOYMENT.md`):

- **gateway / web**: stateless, load-balanced by the Service - safe to scale and
  autoscale (HPA is enabled).
- **centers**: safe for HA/rollouts. Traffic from the gateway is balanced by Spring
  Cloud Gateway over HTTP; center-to-center gRPC uses static DNS targets, so a scaled
  center serves as failover rather than request-level balancing.
- **drivers**: stateless workers sharing the same RabbitMQ queues - safe to scale.
  Exception: `listening-virtual` pins inbound device sockets, keep `replicas: 1`.
- **postgres / rabbitmq**: singletons by design. Do not scale past 1 replica; for HA
  run managed PostgreSQL/RabbitMQ and point the ConfigMap at them.

## Rolling upgrade

```bash
# bump the tag centrally, then re-apply (Deployments roll with maxUnavailable: 0)
kustomize edit set image pnoker/dc3-gateway=pnoker/dc3-gateway:2026.7   # per image
kubectl apply -k dc3/deploy/k8s
kubectl -n dc3 rollout status deployment/dc3-gateway
```

Stateful services upgrade in place (RollingUpdate on the StatefulSet). Back up the
database before upgrading `dc3-postgres`:

```bash
kubectl -n dc3 exec dc3-postgres-0 -- pg_dumpall -U dc3 > backup.sql
```

## Storage

- `dc3-postgres` -> 20Gi PVC (initdb seed SQL runs on first start of an empty volume).
- `dc3-rabbitmq` -> 8Gi PVC.
- `dc3-center-agentic` -> 5Gi PVC for attachments (Agentic memory/attachments).
- Drivers mount `emptyDir` for protocol-local cache; promote to a PVC per driver if a
  driver needs durable state.
- On multi-node clusters make sure the default StorageClass is replicated (or use a
  CSI driver with snapshots). Back up PostgreSQL regularly (pg_dump/pgBackRest).

## Security hardening (before production)

1. Replace every value in `secret.env` - especially `DC3_SECURITY_KEY` and
   `AUTH_HMAC_SECRET` (the `pro` profile refuses to start with weak keys) - and do
   not commit `secret.env`.
2. Terminate TLS at the ingress (bundled annotations reference cert-manager) and remove
   the placeholder `dc3.example.com`.
3. Restrict egress with NetworkPolicies (only the centers need to reach the LLM
   endpoint), and enable Pod Security Admission
   (`kubectl label ns dc3 pod-security.kubernetes.io/enforce=baseline`).
4. Use a dedicated ServiceAccount per workload and pull images with
   `imagePullSecrets` from a private registry.
5. Expose Swagger/OpenAPI only on dev-like profiles (already disabled for `pro`).

## Operations

```bash
kubectl -n dc3 logs -f deploy/dc3-gateway
kubectl -n dc3 exec -it dc3-postgres-0 -- psql -U dc3 -d dc3
kubectl -n dc3 delete -k dc3/deploy/k8s   # teardown (PVCs survive unless --cascade=foreground)
```
