# IoT DC3 Deployment Guide

This document covers every deployment mode shipped with IoT DC3, from a single-host Compose stack to Kubernetes and
Helm, and the production hardening checklist. It is the companion to the Quickstart and the environment variable
reference on docs.dc3.site.

## 1. Deployment modes at a glance

| Mode          | Files                                                        | Runtime               | Use for                                         | Scaling                             |
|---------------|--------------------------------------------------------------|-----------------------|-------------------------------------------------|-------------------------------------|
| Single host   | `dc3/docker-compose-db.yml` + `dc3/docker-compose.yml`       | Docker/Podman Compose | evaluation, demo, small production              | none (singleton)                    |
| Compose scale | `dc3/docker-compose-db.yml` + `dc3/docker-compose-scale.yml` | Docker Compose v2     | single-node production with replicated services | `docker compose up --scale <svc>=N` |
| Docker Swarm  | `dc3/docker-compose-swarm.yml`                               | Docker Swarm mode     | multi-node swarm cluster                        | `docker service scale dc3_<svc>=N`  |
| Kubernetes    | `dc3/deploy/k8s/` (kustomize)                                | any k8s cluster       | production Kubernetes                           | `kubectl scale` / HPA               |
| Helm          | `dc3/deploy/helm/dc3/`                                       | Kubernetes            | GitOps / repeatable installs                    | values + HPA                        |

All modes run the same images with the same environment variables, so a service topology tuned on Compose behaves
identically elsewhere. The runtime differences are only about *where* replicas live and *how* traffic reaches them.

### Service topology and ports

| Service        | Image                  | HTTP                  | gRPC | Notes                                         |
|----------------|------------------------|-----------------------|------|-----------------------------------------------|
| web            | `dc3-web`              | 80/443 (in container) | -    | nginx; proxies `/api/` to the gateway         |
| gateway        | `dc3-gateway`          | 8000                  | -    | single HTTP entry point for all centers       |
| auth           | `dc3-center-auth`      | 8300                  | 9300 | tenants, users, RBAC                          |
| manager        | `dc3-center-manager`   | 8400                  | 9400 | drivers, devices, points, profiles            |
| data           | `dc3-center-data`      | 8500                  | 9500 | point values, commands, dashboards            |
| agentic        | `dc3-center-agentic`   | 8600                  | -    | AI agentic center (LLM calls)                 |
| `dc3-driver-*` | one image per protocol | -                     | -    | protocol adapters                             |
| postgres       | `dc3-postgres`         | -                     | -    | PostgreSQL + AGE/TimescaleDB/pgvector, seeded |
| rabbitmq       | `dc3-rabbitmq`         | -                     | -    | AMQP + embedded MQTT broker, TLS-capable      |

> Ingress discipline: only `web` (80/443) and `listening-virtual` (device TCP 6270) are ever published to the
> outside. Every backend port stays on the internal network. Do not map center or database ports to the host in
> production. Note on 6271: it is intended as the device UDP channel, but every publish site today (Compose, scale,
> swarm, the k8s Service) omits `/udp` / `protocol: UDP`, so it currently publishes as TCP - add the protocol
> declaration before relying on UDP.

## 2. Image availability

The release CI (`docker-ci.yml`) builds and publishes **app images** (web, gateway, centers, drivers) to Docker Hub
(`pnoker/*`) and Aliyun (`registry.cn-beijing.aliyuncs.com/dc3/*`) for every `v*` tag. The **dependency images**
`dc3-postgres` and `dc3-rabbitmq` are built locally by
`docker-compose-db.yml` and are **not** published:

```bash
# build + push them once for swarm/k8s/helm (Compose builds them automatically)
DC3_IMAGE_REGISTRY=my.registry/dc3 ./dc3/deploy/k8s/scripts/push-images.sh
```

Tag layout differs per image family: the backend images (gateway, centers, drivers) are published as `latest`, the
series tag (`2026.6`), and the full version (`2026.6.0`), while
`dc3-web` is published only as `latest` and full release versions (no series tag). The deployment configs already
account for this - web pins `latest` with a documented override (`services.web.tag` in Helm, the `images:` block in
kustomize); pin a full release version for `dc3-web` when you need reproducible rollouts.

## 3. Mode 0 - single host (Compose)

```bash
make up-db          # PostgreSQL + RabbitMQ (docker-compose-db.yml)
make up STACK=app   # app stack: web, gateway, centers, drivers (docker-compose.yml)
make logs           # follow container logs
make down           # stop, keep volumes
make reset CONFIRM_RESET_VOLUMES=true   # down + delete volumes (destructive)
```

Use `REGISTRY=cn` for the Aliyun mirror. All values come from `.env` (see the environment variable reference on
docs.dc3.site).

## 4. Mode 1 - Compose with horizontal scaling

`dc3/docker-compose-scale.yml` is the app topology rebuilt for replicas: no
`container_name`/`hostname` pins, no per-replica host ports, and `deploy.resources`
limits enforced by Compose v2. It intentionally keeps PostgreSQL/RabbitMQ in the separate `db` stack (both files share
the `dc3net` project network).

```bash
docker compose -f dc3/docker-compose-db.yml up -d
docker compose -f dc3/docker-compose-scale.yml up -d \
    --scale gateway=2 --scale data=2
```

### What can scale, and how traffic is balanced

| Tier                | Scalable?     | Load balancing                                                                                                                                                                                       |
|---------------------|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| web                 | 1 replica     | publishes 8080/8443 - put your own LB in front for more                                                                                                                                              |
| gateway             | yes           | the nginx in `dc3-web` resolves `dc3-gateway` to every replica and round-robins (restart `web` after scaling gateway to refresh addresses)                                                           |
| centers             | yes (HA)      | HTTP routes from the gateway are balanced by Spring Cloud Gateway; center-to-center gRPC uses static DNS targets - a replica restart fails over the channel, but the channel is not request-balanced |
| drivers             | **1 replica** | every replica mounts the shared `driver_data` volume and would open the same SQLite outbox file; scale drivers on Kubernetes (per-pod `emptyDir`) instead                                            |
| listening-virtual   | **1 replica** | inbound device sockets are pinned to one container                                                                                                                                                   |
| postgres / rabbitmq | **1 replica** | stateful singletons by design                                                                                                                                                                        |

### Gotchas

- Center-to-center gRPC (`static://` scheme) keeps one channel per client. For request-level balancing of center gRPC,
  run Kubernetes (Service round-robin) or add a client-side LB. HTTP traffic through the gateway is balanced in every
  mode.
- `depends_on` with `condition: service_healthy` is honored by Compose; after scaling, new replicas start against the
  already-healthy stack.

## 5. Mode 2 - Docker Swarm

`dc3/docker-compose-swarm.yml` is a self-contained Swarm stack (dependency images included, overlay network, `deploy:`
blocks for replicas/updates/restarts/resources).

```bash
# 1. one-time: a single-node swarm is enough to start
docker swarm init

# 2. build + push dependency images (not published by CI; a local build is fine on a single node)
DC3_IMAGE_REGISTRY=my.registry/dc3 ./dc3/deploy/k8s/scripts/push-images.sh

# 3. deploy (run from the repo root so .env is interpolated)
docker stack deploy -c dc3/docker-compose-swarm.yml dc3

# 4. operate
docker service ls
docker service scale dc3_gateway=3
docker service logs -f dc3_gateway
docker stack rm dc3
```

### Swarm-specific notes

- `web` publishes 80/443 with `mode: ingress` - the swarm ingress network balances replicas automatically
  (`docker service scale dc3_web=2` is safe).
- `listening-virtual` publishes 6270/6271 with `mode: host` and must stay at 1 replica (connection affinity).
- Protocol drivers stay at 1 replica: every replica would open the same SQLite outbox file on the shared `driver_data`
  volume. Scale drivers on Kubernetes instead (each pod gets its own `emptyDir`).
- Stateful services are constrained to `node.role == manager` so their local volumes stay on one node. On a multi-node
  swarm change the constraint to a dedicated label (e.g. `node.labels.dc3-stateful==true`) and place the volumes on
  shared storage (NFS/Ceph) - the `local` driver is single-node only.
- Swarm ignores `depends_on`/`build`; startup is handled by healthchecks + restart policy, and the centers retry their
  gRPC channels until dependencies are ready.

## 6. Mode 3 - Kubernetes (kustomize)

Manifests live in `dc3/deploy/k8s/` (see its `README.md` for the full runbook).

```bash
cd iot-dc3
cp dc3/deploy/k8s/secret.env.example dc3/deploy/k8s/secret.env   # then edit values
DC3_IMAGE_REGISTRY=my.registry/dc3 ./dc3/deploy/k8s/scripts/push-images.sh
kubectl apply -k dc3/deploy/k8s
kubectl -n dc3 get pods -w
```

- The kustomize root is the single control point: resource list, `secretGenerator`
  (from `secret.env`), and the `images:` block (one place to bump the tag or switch registries).
- Stateful services are `StatefulSet`s with PVCs (postgres 20Gi, rabbitmq 8Gi, agentic 5Gi); the seed SQL runs on the
  first start of an empty volume, same as Compose.
- `gateway`/`web` ship CPU-based HPA (autoscaling/v2) and every stateless service has a
  `PodDisruptionBudget` (minAvailable 1) and rolling updates with `maxUnavailable: 0`.
- Device ingress: `dc3-driver-listening-virtual` is exposed as NodePort 30670/30671; switch the Service to
  `LoadBalancer` if you prefer.
- Ingress routes `/api/` to `dc3-gateway:8000` and `/` to `dc3-web:80`; set the host and TLS (cert-manager example
  annotation provided) before production.
- Scaling semantics: gateway/web/centers/drivers scale - driver pods each get a per-pod `emptyDir` outbox, unlike Mode
  1/2 where drivers stay at 1 replica;
  `listening-virtual` stays at 1; postgres/rabbitmq are singletons. The k8s Service (kube-proxy) additionally
  load-balances per TCP connection, so scaled centers also get connection-level distribution for HTTP. Note that a
  scaled driver's replicas are independent nodes with separate command queues (see the FAQ), not one shared worker
  pool.

## 7. Mode 4 - Helm

Chart: `dc3/deploy/helm/dc3` (README inside). It renders the same topology as the kustomize manifests, parameterized by
`values.yaml` / `values-production.yaml`.

```bash
helm upgrade --install dc3 dc3/deploy/helm/dc3 -f dc3/deploy/helm/dc3/values-production.yaml \
    --set image.registry=my.registry/dc3 \
    --set-string secrets.DC3_SECURITY_KEY=<random> \
    --set-string secrets.AUTH_HMAC_SECRET=<random>
helm ls            # or: helm status dc3
helm upgrade dc3 dc3/deploy/helm/dc3 --reuse-values --set services.gateway.replicas=4
helm rollback dc3 1
helm uninstall dc3
```

- Services are generated from the `services:`/`drivers:` maps - enable/disable and tune replicas, resources and probes
  without touching templates.
- Secrets come from the chart-owned `dc3-secrets` Secret; set `existingSecret` to use your own (GitOps-friendly).
- `listening-virtual` gets a NodePort Service from `drivers.listening-virtual.nodePort`.

## 8. Production hardening checklist

Apply every item before connecting real devices:

1. **Secrets** - replace `DC3_SECURITY_KEY`, `AUTH_HMAC_SECRET`, all database/broker passwords and the LLM API key with
   strong random values. The fail-fast gate covers `AUTH_HMAC_SECRET` only: on the `pre`/`pro` profile an empty or
   still-default value refuses to start; `DC3_SECURITY_KEY` is checked for non-empty only, so a known weak default
   still boots. Never
   commit
   `secret.env` / `values-production.yaml` with real values.
2. **TLS** - terminate TLS at the edge (web nginx already ships a hardened TLS config; k8s: ingress + cert-manager;
   swarm: put a proxy in front of `web`). Enable RabbitMQ TLS (`RABBITMQ_SSL_ENABLED=true`, port 5671) and PostgreSQL
   TLS for cross-node traffic.
3. **Storage & backups** - schedule `pg_dump`/pgBackRest with off-site copies; test restore. TimescaleDB chunks grow
   continuously - plan capacity (FAQ sizing: 8 cores / 16 GB / 100 GB SSD minimum for the full stack).
4. **High availability** - postgres: primary/standby (or managed service) + RabbitMQ cluster; stateful volumes on
   replicated storage for swarm/k8s multi-node.
5. **Observability** - add the optional stack (`docker-compose-optional.yml` in Compose; deploy Prometheus/Grafana + ELK
   on k8s/swarm) and alert on readiness/liveness.
6. **Network** - restrict egress with firewall/NetworkPolicies (centers need the LLM endpoint; everything else is
   internal), keep backend ports off the host, enable Pod Security Admission `baseline` on k8s.
7. **API surface** - Swagger/OpenAPI is disabled on the `pro` profile. Release images are built with `PROFILE=pro`, and
   at runtime every deployment config selects the profile via
   `NODE_ENV` (`test` by default; set `NODE_ENV=pro` for the hardened profile - OpenAPI/Swagger UI off, weak secrets
   rejected). Verify no debug endpoints are reachable.

## 9. Common questions

- **Why does scaling a center not balance every gRPC request?** Center-to-center gRPC uses static DNS targets
  (`static://host:port`) with one channel per client. Replicas provide failover and rollout safety; true gRPC request
  balancing needs client-side LB or k8s (per-connection round-robin via ClusterIP). HTTP traffic is balanced at every
  tier (nginx -> Spring Cloud Gateway -> centers).
- **Can I run drivers at 2 replicas?** On Compose/Swarm, no - keep protocol drivers at 1 replica (they share the
  `driver_data` volume and would open the same SQLite outbox file). On Kubernetes you can scale them (per-pod
  `emptyDir`), but understand the mechanics: every replica registers as its own node with its own RabbitMQ command
  queue (`group="${dc3.driver.client}"`, routing key `service.node`, node = per-instance UUID) - replicas are not
  competing consumers of one shared queue, so scaling adds independent driver instances rather than splitting work
  per message. `listening-virtual` always stays at 1 replica (it owns inbound device sockets).
- **PostgreSQL/RabbitMQ replicas?** Not supported by these configs - they are stateful singletons. For HA run managed
  services (or your own cluster) and override the host variables: `POSTGRES_HOST`, `RABBITMQ_HOST`, `MQTT_BROKER_HOST`
  (plus
  `CENTER_*_HOST`/`APP_API_HOST` if you also move the services). Self-managed replacements must provide what the bundled
  images provide: PostgreSQL with the AGE, TimescaleDB and pgvector extensions (plus the seed SQL on first start),
  RabbitMQ with the MQTT plugin and the `dc3.e.mqtt` exchange.
- **Do the k8s/helm configs need the dependency images?** Yes; build and push them with
  `scripts/push-images.sh` (or `kind load` on single-node clusters).
