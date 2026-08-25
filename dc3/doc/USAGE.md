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

## 🚢 Deployment

> Single-host Compose is only the first of five deployment modes. The full runbook - every
> command, the scaling semantics, and the production hardening checklist - lives in
> [`dc3/doc/DEPLOYMENT.md`](https://github.com/pnoker/iot-dc3/blob/main/dc3/doc/DEPLOYMENT.md).

| Mode            | Files                                                | Use for                                     |
|-----------------|------------------------------------------------------|---------------------------------------------|
| Single host     | `docker-compose-db.yml` + `docker-compose.yml`       | evaluation, demo, small production          |
| Compose scaling | `docker-compose-db.yml` + `docker-compose-scale.yml` | replicated services on one host (`--scale`) |
| Docker Swarm    | `docker-compose-swarm.yml`                           | multi-node swarm (`make stack-deploy`)      |
| Kubernetes      | `dc3/deploy/k8s/`                                    | production Kubernetes (`make k8s-apply`)    |
| Helm            | `dc3/deploy/helm/dc3/`                               | GitOps installs (`make helm-install`)       |

Facts that hold in every mode:

- **Dependency images** (`dc3-postgres`, `dc3-rabbitmq`) are built locally by the `db` stack and are **not published**
  by the release CI. For swarm/k8s/helm, build and push them first
  (`DC3_IMAGE_REGISTRY=my.registry/dc3 ./dc3/deploy/k8s/scripts/push-images.sh`) or point the stack at managed services
  instead.
- **`dc3-web` is tagged `latest` and full release versions only** - it has no series tag like
  `2026.6`. Backend images (gateway, centers, drivers) carry `latest`, the series tag, and the full version. The
  deployment configs account for this; pin a full release version for
  `dc3-web` when you need reproducible rollouts.
- **DNS names are the contract.** Keep the default service names (`dc3-postgres`,
  `dc3-rabbitmq`, `dc3-center-*`, `dc3-gateway`, `dc3-web`) so the images resolve each other, or override
  `POSTGRES_HOST`, `RABBITMQ_HOST`, `MQTT_BROKER_HOST`, `CENTER_*_HOST`, and
  `APP_API_HOST` to point at services deployed differently.
- **`NODE_ENV` selects the Spring profile** at runtime: `test` (the default in every deployment config) or `pro`
  (hardened: OpenAPI/Swagger UI disabled, weak secrets rejected).

Mode-specific runbooks: `dc3/deploy/k8s/README.md`, `dc3/deploy/helm/dc3/README.md`, and the
`stack-deploy` / `stack-rm` / `k8s-apply` / `k8s-delete` / `helm-install` / `helm-uninstall`
targets in the root `Makefile`.
