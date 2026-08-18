# 📦 Usage

### 🍭 Requirements

> These are the core requirements needed to build and run the application. Make sure to have these tools installed and
> properly configured in your development environment.

- **JDK 21**: Java Development Kit version 21 or higher
- **Maven**: Build automation and dependency management tool
- **Podman or Docker**: Container platform with Compose support for building and running applications

### 🍻 Quick Start

> Choose one of the following container registries:

#### 🦁 Docker Hub

> Global access with standard Docker registry service

```bash
cd iot-dc3
make up-db
make up STACK=app
```

#### 🐱 China Registry

> Optimized registry service for users in mainland China

```bash
cd iot-dc3
make up-db-cn
make up STACK=app REGISTRY=cn
```

> You can also start full workflows or other compose stacks with the same selector, for example:

```bash
make up-db && make up-optional && make up-dev
make up-db-cn && make up-optional-cn && make up-dev-cn
make up-db-cn && make up-optional-cn && make up-app-cn
make up STACK=optional
make up STACK=optional REGISTRY=cn
make logs STACK=dev REGISTRY=global
```

For frontend and API testing, use the service-level shortcuts to start only the modules under test:

```bash
make up-db-cn
make up SERVICES=agentic REGISTRY=cn
make up SERVICES="gateway agentic" REGISTRY=cn
make logs SERVICES="gateway agentic"
make up GROUP=core REGISTRY=cn
make up GROUP=drivers REGISTRY=cn
```

## 🐳 Container

### ⛳ Platform Support

All images are built for multiple platforms:

- `linux/amd64` - For Intel/AMD 64-bit systems
- `linux/arm64` - For ARM 64-bit systems (Apple Silicon, ARM servers)

### 🚥 Version Tags

- `${DC3_IMAGE_TAG}` - Specific version (recommended for production)
- `latest` - Latest stable version (may change)

### 🍉 Images

| Description              | Docker Hub                                             | China Registry                                                                       |
|--------------------------|--------------------------------------------------------|--------------------------------------------------------------------------------------|
| Gateway                  | `pnoker/dc3-gateway:${DC3_IMAGE_TAG}`                  | `registry.cn-beijing.aliyuncs.com/dc3/dc3-gateway:${DC3_IMAGE_TAG}`                  |
| Agentic Center           | `pnoker/dc3-center-agentic:${DC3_IMAGE_TAG}`           | `registry.cn-beijing.aliyuncs.com/dc3/dc3-center-agentic:${DC3_IMAGE_TAG}`           |
| Auth Center              | `pnoker/dc3-center-auth:${DC3_IMAGE_TAG}`              | `registry.cn-beijing.aliyuncs.com/dc3/dc3-center-auth:${DC3_IMAGE_TAG}`              |
| Data Center              | `pnoker/dc3-center-data:${DC3_IMAGE_TAG}`              | `registry.cn-beijing.aliyuncs.com/dc3/dc3-center-data:${DC3_IMAGE_TAG}`              |
| Manager Center           | `pnoker/dc3-center-manager:${DC3_IMAGE_TAG}`           | `registry.cn-beijing.aliyuncs.com/dc3/dc3-center-manager:${DC3_IMAGE_TAG}`           |
| Single Center            | `pnoker/dc3-center-single:${DC3_IMAGE_TAG}`            | `registry.cn-beijing.aliyuncs.com/dc3/dc3-center-single:${DC3_IMAGE_TAG}`            |
| Listening Virtual Driver | `pnoker/dc3-driver-listening-virtual:${DC3_IMAGE_TAG}` | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-listening-virtual:${DC3_IMAGE_TAG}` |
| Modbus TCP Driver        | `pnoker/dc3-driver-modbus-tcp:${DC3_IMAGE_TAG}`        | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-modbus-tcp:${DC3_IMAGE_TAG}`        |
| Modbus RTU Driver        | `pnoker/dc3-driver-modbus-rtu:${DC3_IMAGE_TAG}`        | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-modbus-rtu:${DC3_IMAGE_TAG}`        |
| MQTT Driver              | `pnoker/dc3-driver-mqtt:${DC3_IMAGE_TAG}`              | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-mqtt:${DC3_IMAGE_TAG}`              |
| OPC DA Driver            | `pnoker/dc3-driver-opc-da:${DC3_IMAGE_TAG}`            | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-opc-da:${DC3_IMAGE_TAG}`            |
| OPC UA Driver            | `pnoker/dc3-driver-opc-ua:${DC3_IMAGE_TAG}`            | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-opc-ua:${DC3_IMAGE_TAG}`            |
| Siemens S7 Driver        | `pnoker/dc3-driver-plcs7:${DC3_IMAGE_TAG}`             | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-plcs7:${DC3_IMAGE_TAG}`             |
| Virtual Driver           | `pnoker/dc3-driver-virtual:${DC3_IMAGE_TAG}`           | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-virtual:${DC3_IMAGE_TAG}`           |
| BACnet/IP Driver         | `pnoker/dc3-driver-bacnet-ip:${DC3_IMAGE_TAG}`         | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-bacnet-ip:${DC3_IMAGE_TAG}`         |
| BLE Driver               | `pnoker/dc3-driver-ble:${DC3_IMAGE_TAG}`               | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-ble:${DC3_IMAGE_TAG}`               |
| CAN Driver               | `pnoker/dc3-driver-can:${DC3_IMAGE_TAG}`               | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-can:${DC3_IMAGE_TAG}`               |
| CoAP Driver              | `pnoker/dc3-driver-coap:${DC3_IMAGE_TAG}`              | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-coap:${DC3_IMAGE_TAG}`              |
| DLMS Driver              | `pnoker/dc3-driver-dlms:${DC3_IMAGE_TAG}`              | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-dlms:${DC3_IMAGE_TAG}`              |
| EtherNet/IP Driver       | `pnoker/dc3-driver-ethernet-ip:${DC3_IMAGE_TAG}`       | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-ethernet-ip:${DC3_IMAGE_TAG}`       |
| Omron FINS Driver        | `pnoker/dc3-driver-fins:${DC3_IMAGE_TAG}`              | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-fins:${DC3_IMAGE_TAG}`              |
| HTTP Driver              | `pnoker/dc3-driver-http:${DC3_IMAGE_TAG}`              | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-http:${DC3_IMAGE_TAG}`              |
| IEC 60870-5-104 Driver   | `pnoker/dc3-driver-iec104:${DC3_IMAGE_TAG}`            | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-iec104:${DC3_IMAGE_TAG}`            |
| LwM2M Driver             | `pnoker/dc3-driver-lwm2m:${DC3_IMAGE_TAG}`             | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-lwm2m:${DC3_IMAGE_TAG}`             |
| Mitsubishi MELSEC Driver | `pnoker/dc3-driver-melsec:${DC3_IMAGE_TAG}`            | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-melsec:${DC3_IMAGE_TAG}`            |
| MySQL Driver             | `pnoker/dc3-driver-mysql:${DC3_IMAGE_TAG}`             | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-mysql:${DC3_IMAGE_TAG}`             |
| Oracle Driver            | `pnoker/dc3-driver-oracle:${DC3_IMAGE_TAG}`            | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-oracle:${DC3_IMAGE_TAG}`            |
| PostgreSQL Driver        | `pnoker/dc3-driver-postgresql:${DC3_IMAGE_TAG}`        | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-postgresql:${DC3_IMAGE_TAG}`        |
| Serial Driver            | `pnoker/dc3-driver-serial:${DC3_IMAGE_TAG}`            | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-serial:${DC3_IMAGE_TAG}`            |
| SL651 Driver             | `pnoker/dc3-driver-sl651:${DC3_IMAGE_TAG}`             | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-sl651:${DC3_IMAGE_TAG}`             |
| SNMP Driver              | `pnoker/dc3-driver-snmp:${DC3_IMAGE_TAG}`              | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-snmp:${DC3_IMAGE_TAG}`              |
| SQL Server Driver        | `pnoker/dc3-driver-sqlserver:${DC3_IMAGE_TAG}`         | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-sqlserver:${DC3_IMAGE_TAG}`         |
| TCP/UDP Driver           | `pnoker/dc3-driver-tcp-udp:${DC3_IMAGE_TAG}`           | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-tcp-udp:${DC3_IMAGE_TAG}`           |
| Zigbee Driver            | `pnoker/dc3-driver-zigbee:${DC3_IMAGE_TAG}`            | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-zigbee:${DC3_IMAGE_TAG}`            |

## 🐝 Docker Swarm

> All images are plain multi-arch OCI images, so the same stack can be scheduled with `docker stack deploy`.
> The compose files under `dc3/` are not directly deployable as a stack — Swarm ignores `depends_on` conditions,
> `container_name`, and `build:` — so deploy from a dedicated stack file like the one below.

Service names double as DNS names on the overlay network. Keep the names the images resolve by default
(`dc3-postgres`, `dc3-rabbitmq`, `dc3-center-auth`, `dc3-center-manager`, `dc3-center-data`, `dc3-center-agentic`,
`dc3-gateway`, `dc3-web`), or override `POSTGRES_HOST`, `RABBITMQ_HOST`, `MQTT_BROKER_HOST`, `CENTER_*_HOST`, and
`APP_API_HOST` to point at services deployed differently.

```bash
cd iot-dc3
make up-db                                  # build dc3-postgres and dc3-rabbitmq locally (single-node swarm)
docker swarm init                           # skip on an existing swarm manager
docker stack deploy -c dc3/docker-stack.yml dc3
DC3_IMAGE_REGISTRY=registry.cn-beijing.aliyuncs.com/dc3 docker stack deploy -c dc3/docker-stack.yml dc3
```

> On a multi-node swarm, push `dc3-postgres` and `dc3-rabbitmq` to a registry every node can pull, or run
> PostgreSQL/RabbitMQ outside the cluster and override the host variables. Swarm has no `depends_on`: services keep
> restarting until their dependencies answer, so startup order is self-healing. `NODE_ENV: test` mirrors the compose
> stack; set `NODE_ENV: pro` to run the hardened production profile (OpenAPI/Swagger UI disabled).

Save the following stack file as `dc3/docker-stack.yml`, then add the drivers you need with the same `app-env` anchor:

```yaml
x-app-env: &app-env
  NODE_ENV: test
  DC3_SECURITY_KEY: ${DC3_SECURITY_KEY:-dc3.security.key.2026.io.github.pnoker}
  AUTH_HMAC_SECRET: ${AUTH_HMAC_SECRET:-io.github.pnoker.dc3}
  POSTGRES_HOST: dc3-postgres
  POSTGRES_PORT: "5432"
  POSTGRES_USERNAME: ${POSTGRES_USERNAME:-dc3}
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-dc3dc3dc3}
  POSTGRES_DB: ${POSTGRES_DB:-dc3}
  RABBITMQ_VIRTUAL_HOST: ${RABBITMQ_VIRTUAL_HOST:-dc3}
  RABBITMQ_HOST: dc3-rabbitmq
  RABBITMQ_PORT: "5672"
  RABBITMQ_USERNAME: ${RABBITMQ_USERNAME:-dc3}
  RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD:-dc3dc3dc3}
  MQTT_BROKER_HOST: dc3-rabbitmq
  MQTT_BROKER_PORT: "1883"
  MQTT_USERNAME: ${MQTT_USERNAME:-dc3}
  MQTT_PASSWORD: ${MQTT_PASSWORD:-dc3dc3dc3}

services:
  postgres:
    image: ${DC3_IMAGE_REGISTRY:-pnoker}/dc3-postgres:${DC3_IMAGE_TAG:-2026.6}
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-dc3}
      POSTGRES_USER: ${POSTGRES_USERNAME:-dc3}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-dc3dc3dc3}
    volumes:
      - postgres:/var/lib/postgresql
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready -U ${POSTGRES_USERNAME:-dc3} -d ${POSTGRES_DB:-dc3}" ]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 15s
    networks: [ dc3net ]

  rabbitmq:
    image: ${DC3_IMAGE_REGISTRY:-pnoker}/dc3-rabbitmq:${DC3_IMAGE_TAG:-2026.6}
    environment:
      RABBITMQ_DEFAULT_VHOST: ${RABBITMQ_VIRTUAL_HOST:-dc3}
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USERNAME:-dc3}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:-dc3dc3dc3}
      RABBITMQ_MQTT_EXCHANGE: ${RABBITMQ_MQTT_EXCHANGE:-dc3.e.mqtt}
    volumes:
      - rabbitmq:/var/lib/rabbitmq
    healthcheck:
      test: [ "CMD", "rabbitmq-diagnostics", "-q", "ping" ]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 20s
    networks: [ dc3net ]

  auth:
    image: ${DC3_IMAGE_REGISTRY:-pnoker}/dc3-center-auth:${DC3_IMAGE_TAG:-2026.6}
    environment: *app-env
    healthcheck:
      test: [ "CMD-SHELL", "curl -fsS http://127.0.0.1:8300/auth/actuator/health/readiness >/dev/null || exit 1" ]
      interval: 15s
      timeout: 5s
      retries: 10
      start_period: 40s
    networks: [ dc3net ]

  manager:
    image: ${DC3_IMAGE_REGISTRY:-pnoker}/dc3-center-manager:${DC3_IMAGE_TAG:-2026.6}
    environment: *app-env
    healthcheck:
      test: [ "CMD-SHELL", "curl -fsS http://127.0.0.1:8400/manager/actuator/health/readiness >/dev/null || exit 1" ]
      interval: 15s
      timeout: 5s
      retries: 10
      start_period: 40s
    networks: [ dc3net ]

  data:
    image: ${DC3_IMAGE_REGISTRY:-pnoker}/dc3-center-data:${DC3_IMAGE_TAG:-2026.6}
    environment: *app-env
    healthcheck:
      test: [ "CMD-SHELL", "curl -fsS http://127.0.0.1:8500/data/actuator/health/readiness >/dev/null || exit 1" ]
      interval: 15s
      timeout: 5s
      retries: 10
      start_period: 40s
    networks: [ dc3net ]

  agentic:
    image: ${DC3_IMAGE_REGISTRY:-pnoker}/dc3-center-agentic:${DC3_IMAGE_TAG:-2026.6}
    environment:
      <<: *app-env
      AGENTIC_FALLBACK_OPENAI_BASE_URL: ${AGENTIC_FALLBACK_OPENAI_BASE_URL:-https://api.openai.com}
      AGENTIC_FALLBACK_OPENAI_API_KEY: ${AGENTIC_FALLBACK_OPENAI_API_KEY:-}
      AGENTIC_FALLBACK_OPENAI_MODEL: ${AGENTIC_FALLBACK_OPENAI_MODEL:-gpt-4o}
    healthcheck:
      test: [ "CMD-SHELL", "curl -fsS http://127.0.0.1:8600/agentic/actuator/health/readiness >/dev/null || exit 1" ]
      interval: 15s
      timeout: 5s
      retries: 10
      start_period: 40s
    networks: [ dc3net ]

  gateway:
    image: ${DC3_IMAGE_REGISTRY:-pnoker}/dc3-gateway:${DC3_IMAGE_TAG:-2026.6}
    environment: *app-env
    ports:
      - "8000:8000"
    healthcheck:
      test: [ "CMD-SHELL", "curl -fsS http://127.0.0.1:8000/actuator/health/readiness >/dev/null || exit 1" ]
      interval: 15s
      timeout: 5s
      retries: 10
      start_period: 30s
    networks: [ dc3net ]

  web:
    image: pnoker/dc3-web:latest
    environment:
      APP_API_HOST: dc3-gateway
      APP_API_PORT: "8000"
    ports:
      - "8080:80"
    networks: [ dc3net ]

  # Drivers follow the same pattern, for example:
  # virtual:
  #   image: ${DC3_IMAGE_REGISTRY:-pnoker}/dc3-driver-virtual:${DC3_IMAGE_TAG:-2026.6}
  #   environment: *app-env
  #   networks: [ dc3net ]
  # Listening drivers additionally publish their device ingress ports
  # (TCP 6270 / UDP 6271 for dc3-driver-listening-virtual).

networks:
  dc3net:
    driver: overlay
    attachable: true

volumes:
  postgres:
  rabbitmq:
```

## ☸️ Kubernetes

> Deploy the same images as Deployments behind Services that keep the default DNS names. The images resolve each
> other through those names, so either create Services named `dc3-center-auth`, `dc3-center-manager`,
> `dc3-center-data`, `dc3-center-agentic`, `dc3-gateway`, `dc3-postgres`, and `dc3-rabbitmq` in one namespace, or
> override `CENTER_*_HOST`, `POSTGRES_HOST`, `RABBITMQ_HOST`, and `MQTT_BROKER_HOST`.

Service ports and probe paths:

| Service               | HTTP | gRPC       | Readiness path                        |
|-----------------------|------|------------|---------------------------------------|
| `dc3-gateway`         | 8000 | — (client) | `/actuator/health/readiness`          |
| `dc3-center-auth`     | 8300 | 9300       | `/auth/actuator/health/readiness`     |
| `dc3-center-manager`  | 8400 | 9400       | `/manager/actuator/health/readiness`  |
| `dc3-center-data`     | 8500 | 9500       | `/data/actuator/health/readiness`     |
| `dc3-center-agentic`  | 8600 | — (client) | `/agentic/actuator/health/readiness`  |
| `dc3-web`             | 80   | —          | `/`                                   |

Representative manifest (namespace, shared configuration, one Deployment, and its Service):

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: dc3
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: dc3-runtime
  namespace: dc3
data:
  NODE_ENV: "test" # "pro" disables OpenAPI/Swagger UI
  POSTGRES_HOST: dc3-postgres
  POSTGRES_PORT: "5432"
  POSTGRES_DB: dc3
  RABBITMQ_VIRTUAL_HOST: dc3
  RABBITMQ_HOST: dc3-rabbitmq
  RABBITMQ_PORT: "5672"
  MQTT_BROKER_HOST: dc3-rabbitmq
  MQTT_BROKER_PORT: "1883"
---
apiVersion: v1
kind: Secret
metadata:
  name: dc3-credentials
  namespace: dc3
stringData:
  DC3_SECURITY_KEY: change-me
  AUTH_HMAC_SECRET: change-me
  POSTGRES_USERNAME: dc3
  POSTGRES_PASSWORD: dc3dc3dc3
  RABBITMQ_USERNAME: dc3
  RABBITMQ_PASSWORD: dc3dc3dc3
  MQTT_USERNAME: dc3
  MQTT_PASSWORD: dc3dc3dc3
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dc3-center-auth
  namespace: dc3
spec:
  replicas: 1
  selector:
    matchLabels:
      app: dc3-center-auth
  template:
    metadata:
      labels:
        app: dc3-center-auth
    spec:
      containers:
        - name: dc3-center-auth
          image: pnoker/dc3-center-auth:2026.6
          envFrom:
            - configMapRef:
                name: dc3-runtime
            - secretRef:
                name: dc3-credentials
          readinessProbe:
            httpGet:
              path: /auth/actuator/health/readiness
              port: 8300
            initialDelaySeconds: 40
            periodSeconds: 15
          livenessProbe:
            httpGet:
              path: /auth/actuator/health/readiness
              port: 8300
            initialDelaySeconds: 60
            periodSeconds: 30
---
apiVersion: v1
kind: Service
metadata:
  name: dc3-center-auth
  namespace: dc3
spec:
  selector:
    app: dc3-center-auth
  ports:
    - name: http
      port: 8300
      targetPort: 8300
    - name: grpc
      port: 9300
      targetPort: 9300
```

Notes:

- Repeat the Deployment/Service pair for the other centers using the port and probe-path table above (`dc3-center-agentic` has no gRPC port), then deploy `dc3-gateway` (HTTP 8000) and `dc3-web` (port 80, env `APP_API_HOST=dc3-gateway`, `APP_API_PORT=8000`) behind an Ingress or LoadBalancer.
- Drivers are plain Deployments with the same `envFrom` blocks and no Service; expose device ingress ports only for listening drivers (TCP 6270 / UDP 6271 for `dc3-driver-listening-virtual`).
- Dependencies: reuse the `dc3-postgres` and `dc3-rabbitmq` images (push them to a registry the cluster can pull; give PostgreSQL a PersistentVolumeClaim — the seed SQL under `/docker-entrypoint-initdb.d` runs only on first start), or use managed services and override `POSTGRES_HOST`/`RABBITMQ_HOST`/`MQTT_BROKER_HOST`. A self-managed PostgreSQL needs the TimescaleDB extension; a self-managed RabbitMQ needs the MQTT plugin and the `dc3.e.mqtt` exchange.
- Verify rollout with `kubectl -n dc3 get pods -w`; pods stay unready until their dependencies answer.

## ⎈ Helm

> There is no official chart yet; a minimal chart wrapping the manifests above is enough to manage releases with Helm.

Create the chart and drive the service list from values:

```bash
helm create dc3
```

`dc3/values.yaml`:

```yaml
image:
  registry: pnoker # registry.cn-beijing.aliyuncs.com/dc3 in mainland China
  tag: "2026.6" # the release series to deploy

runtime:
  nodeEnv: test # pro disables OpenAPI/Swagger UI

services:
  - name: dc3-center-auth
    port: 8300
    grpcPort: 9300
    readinessPath: /auth/actuator/health/readiness
  - name: dc3-center-manager
    port: 8400
    grpcPort: 9400
    readinessPath: /manager/actuator/health/readiness
  - name: dc3-center-data
    port: 8500
    grpcPort: 9500
    readinessPath: /data/actuator/health/readiness
  - name: dc3-center-agentic
    port: 8600
    readinessPath: /agentic/actuator/health/readiness
  - name: dc3-gateway
    port: 8000
    readinessPath: /actuator/health/readiness
  - name: dc3-driver-virtual # drivers: no Service, no probe
```

`dc3/templates/workloads.yaml` (replace the generated `deployment.yaml`/`service.yaml`, and keep the ConfigMap/Secret from the Kubernetes section as chart templates):

```yaml
{{- range .Values.services }}
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .name }}
  labels:
    app: {{ .name }}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: {{ .name }}
  template:
    metadata:
      labels:
        app: {{ .name }}
    spec:
      containers:
        - name: {{ .name }}
          image: "{{ $.Values.image.registry }}/{{ .name }}:{{ $.Values.image.tag }}"
          env:
            - name: NODE_ENV
              value: {{ $.Values.runtime.nodeEnv | quote }}
          envFrom:
            - configMapRef:
                name: dc3-runtime
            - secretRef:
                name: dc3-credentials
          {{- if .readinessPath }}
          readinessProbe:
            httpGet:
              path: {{ .readinessPath }}
              port: {{ .port }}
            initialDelaySeconds: 40
            periodSeconds: 15
          {{- end }}
{{- if .port }}
---
apiVersion: v1
kind: Service
metadata:
  name: {{ .name }}
spec:
  selector:
    app: {{ .name }}
  ports:
    - name: http
      port: {{ .port }}
      targetPort: {{ .port }}
    {{- with .grpcPort }}
    - name: grpc
      port: {{ . }}
      targetPort: {{ . }}
    {{- end }}
{{- end }}
{{- end }}
```

Install and upgrade:

```bash
helm lint dc3
helm install dc3 ./dc3 --namespace dc3 --create-namespace
helm upgrade dc3 ./dc3 --namespace dc3 --set image.tag=2026.7
```

> The `dc3-web` image is tagged with `latest` and full release versions rather than the series tag, so deploy it as
> its own template (see the Kubernetes section) instead of adding it to `services`.
