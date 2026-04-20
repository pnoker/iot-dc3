## 📦 Usage

### 🍭 Requirements

> These are the core requirements needed to build and run the application. Make sure to have these tools installed and
> properly configured in your development environment.

- **JDK 21**: Java Development Kit version 21 or higher
- **Maven**: Build automation and dependency management tool
- **Podman**: Container platform for building and running applications

### 🍻 Quick Start

> Choose one of the following container registries:

#### 🦁 Docker Hub

> Global access with standard Docker registry service

```bash
cd iot-dc3
make dev-db
make app
```

#### 🐱 Aliyun Container Registry

> Optimized registry service for users in mainland China

```bash
cd iot-dc3
make dev-db REGISTRY=domestic
make app REGISTRY=domestic
```

> You can also start full workflows or other compose stacks with the same selector, for example:

```bash
make dev-all
make dev-all REGISTRY=domestic
make app-all REGISTRY=aliyun
make compose-up STACK=optional
make compose-up STACK=optional REGISTRY=cn
make compose-up STACK=grafana REGISTRY=domestic
make compose-logs STACK=dev REGISTRY=global
```

## 🐳 Container

### ⛳ Platform Support

All images are built for multiple platforms:

- `linux/amd64` - For Intel/AMD 64-bit systems
- `linux/arm64` - For ARM 64-bit systems (Apple Silicon, ARM servers)

### 🚥 Version Tags

- `${SERVICE_VERSION}` - Specific version (recommended for production)
- `latest` - Latest stable version (may change)

### 🍉 Images

| Description              | Docker Hub                                               | Aliyun Container Registry                                                          |
|--------------------------|----------------------------------------------------------|------------------------------------------------------------------------------------|
| Gateway                  | `pnoker/dc3-gateway:${SERVICE_VERSION}`                  | `registry.cn-beijing.aliyuncs.com/dc3-gateway:${SERVICE_VERSION}`                  |
| Auth Center              | `pnoker/dc3-center-auth:${SERVICE_VERSION}`              | `registry.cn-beijing.aliyuncs.com/dc3-center-auth:${SERVICE_VERSION}`              |
| Data Center              | `pnoker/dc3-center-data:${SERVICE_VERSION}`              | `registry.cn-beijing.aliyuncs.com/dc3-center-data:${SERVICE_VERSION}`              |
| Manager Center           | `pnoker/dc3-center-manager:${SERVICE_VERSION}`           | `registry.cn-beijing.aliyuncs.com/dc3-center-manager:${SERVICE_VERSION}`           |
| Single Center            | `pnoker/dc3-center-single:${SERVICE_VERSION}`            | `registry.cn-beijing.aliyuncs.com/dc3-center-single:${SERVICE_VERSION}`            |
| Listening Virtual Driver | `pnoker/dc3-driver-listening-virtual:${SERVICE_VERSION}` | `registry.cn-beijing.aliyuncs.com/dc3-driver-listening-virtual:${SERVICE_VERSION}` |
| Modbus TCP Driver        | `pnoker/dc3-driver-modbus-tcp:${SERVICE_VERSION}`        | `registry.cn-beijing.aliyuncs.com/dc3-driver-modbus-tcp:${SERVICE_VERSION}`        |
| MQTT Driver              | `pnoker/dc3-driver-mqtt:${SERVICE_VERSION}`              | `registry.cn-beijing.aliyuncs.com/dc3-driver-mqtt:${SERVICE_VERSION}`              |
| OPC DA Driver            | `pnoker/dc3-driver-opc-da:${SERVICE_VERSION}`            | `registry.cn-beijing.aliyuncs.com/dc3-driver-opc-da:${SERVICE_VERSION}`            |
| OPC UA Driver            | `pnoker/dc3-driver-opc-ua:${SERVICE_VERSION}`            | `registry.cn-beijing.aliyuncs.com/dc3-driver-opc-ua:${SERVICE_VERSION}`            |
| Siemens S7 Driver        | `pnoker/dc3-driver-plcs7:${SERVICE_VERSION}`             | `registry.cn-beijing.aliyuncs.com/dc3-driver-plcs7:${SERVICE_VERSION}`             |
| Virtual Driver           | `pnoker/dc3-driver-virtual:${SERVICE_VERSION}`           | `registry.cn-beijing.aliyuncs.com/dc3-driver-virtual:${SERVICE_VERSION}`           |
