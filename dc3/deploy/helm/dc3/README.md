# IoT DC3 Helm Chart

Helm chart for deploying the IoT DC3 platform: HTTP gateway, the four centers
(auth / manager / data / agentic), the Vue web console, 25 protocol drivers, and
the PostgreSQL + RabbitMQ stateful dependencies.

> Kubernetes manifests (kustomize) live in `../../k8s`; the compose stacks live in
> `dc3/docker-compose*.yml`. See `dc3/doc/DEPLOYMENT.md` for the full deployment guide.

## Quick start

```bash
cd dc3/deploy/helm/dc3

# dependency images (dc3-postgres / dc3-rabbitmq) are not published by the
# release CI - build and push them first, then point the chart at your registry
helm upgrade --install dc3 .   --set image.registry=my.registry/dc3   --set image.tag=2026.6   --set-string secrets.DC3_SECURITY_KEY=<random>   --set-string secrets.AUTH_HMAC_SECRET=<random>   --set secrets.POSTGRES_PASSWORD=<strong>   --set secrets.RABBITMQ_PASSWORD=<strong>   --set ingress.host=dc3.example.com
```

Production presets (replicas, resources, autoscaling, TLS):

```bash
helm upgrade --install dc3 . -f values-production.yaml   --set image.registry=my.registry/dc3   --set ingress.host=dc3.example.com
```

## What gets installed

| Component          | Kind          | Notes                                            |
|--------------------|---------------|--------------------------------------------------|
| `dc3-postgres`     | StatefulSet   | 20Gi PVC, initdb seed on first start             |
| `dc3-rabbitmq`     | StatefulSet   | 8Gi PVC, MQTT plugin + TLS                       |
| `dc3-gateway`      | Deployment    | HTTP entry, gRPC facade                          |
| `dc3-center-{auth,manager,data,agentic}` | Deployments | per-service ConfigMap/Secret env |
| `dc3-web`          | Deployment    | nginx, proxies /api/ to the gateway              |
| `dc3-driver-*`      | Deployments   | one per protocol driver                          |
| `dc3-driver-listening-virtual` | NodePort Service | inbound device TCP 30670 / UDP 30671  |
| HPA / PDB / Ingress | -             | gateway + web autoscaling, disruption budgets    |

## Key values

| Value | Default | Meaning |
|-------|---------|---------|
| `image.registry` / `image.tag` | `pnoker` / `2026.6` | image source for every component (`services.web.tag` overrides: web has no series tag) |
| `secrets.*` | weak public defaults | **replace before production** |
| `existingSecret` | `""` | use your own Secret instead of the chart-owned one |
| `services.<name>.replicas` | per service | replica count per stateless service |
| `drivers.<name>.enabled` | `true` | disable drivers you do not need |
| `ingress.host` | `dc3.example.com` | route host (placeholder) |
| `ingress.tls` | disabled | enable + cert-manager ClusterIssuer |
| `autoscaling.*` | gateway/web | CPU-based HPA |

Full reference: `helm show values dc3` (or `values.yaml`).

## Scaling / lifecycle

```bash
helm upgrade dc3 . --reuse-values --set services.gateway.replicas=4
helm upgrade dc3 . --reuse-values --set drivers.modbus-tcp.enabled=true
helm rollback dc3 1
helm uninstall dc3     # PVCs survive by default
```

Scaling semantics match the kustomize manifests and the compose-scale stack:
gateway/web/centers/drivers are safe to scale (except `listening-virtual` which pins
inbound device sockets and must stay at 1 replica); postgres/rabbitmq are singletons.
