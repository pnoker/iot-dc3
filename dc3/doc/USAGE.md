# 📦 Usage

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

#### 🐱 China Registry

> Optimized registry service for users in mainland China

```bash
cd iot-dc3
make dev-db REGISTRY=cn
make app REGISTRY=cn
```

> You can also start full workflows or other compose stacks with the same selector, for example:

```bash
make dev-all
make dev-all REGISTRY=cn
make app-all REGISTRY=cn
make compose-up STACK=optional
make compose-up STACK=optional REGISTRY=cn
make compose-logs STACK=dev REGISTRY=global
```

For frontend and API testing, use the service-level shortcuts to start only the modules under test:

```bash
make dev-db REGISTRY=cn
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
| MQTT Driver              | `pnoker/dc3-driver-mqtt:${DC3_IMAGE_TAG}`              | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-mqtt:${DC3_IMAGE_TAG}`              |
| OPC DA Driver            | `pnoker/dc3-driver-opc-da:${DC3_IMAGE_TAG}`            | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-opc-da:${DC3_IMAGE_TAG}`            |
| OPC UA Driver            | `pnoker/dc3-driver-opc-ua:${DC3_IMAGE_TAG}`            | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-opc-ua:${DC3_IMAGE_TAG}`            |
| Siemens S7 Driver        | `pnoker/dc3-driver-plcs7:${DC3_IMAGE_TAG}`             | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-plcs7:${DC3_IMAGE_TAG}`             |
| Virtual Driver           | `pnoker/dc3-driver-virtual:${DC3_IMAGE_TAG}`           | `registry.cn-beijing.aliyuncs.com/dc3/dc3-driver-virtual:${DC3_IMAGE_TAG}`           |
