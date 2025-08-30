## 📦 使用说明

### 🍭 环境要求

> 以下是构建和运行应用所需的核心环境，请确保这些工具已在你的开发环境中正确安装和配置。

- **JDK 21**: Java 开发工具包，版本 21 或更高
- **Maven**: 构建自动化与依赖管理工具
- **Docker**: 用于构建和运行应用的容器平台

### 🍻 快速开始

> 从以下容器镜像仓库中任选一个：

#### 🦁 Docker Hub

> 全球可访问的标准 Docker 镜像仓库服务

```bash
cd iot-dc3/dc3
docker-compose -f docker-compose-db.yml up -d
docker-compose -f docker-compose.yml up -d
```

#### 🐱 阿里云容器镜像服务

> 针对中国大陆用户优化的镜像仓库服务

```bash
cd iot-dc3/dc3
docker-compose -f docker-compose-db-aliyun.yml up -d
docker-compose -f docker-compose-aliyun.yml up -d
```

## 🐳 容器

### ⛳ 平台支持

所有镜像均支持多平台构建：

- `linux/amd64` - 适用于 Intel/AMD 64 位系统
- `linux/arm64` - 适用于 ARM 64 位系统（Apple Silicon、ARM 服务器）

### 🚥 版本标签

- `${SERVICE_VERSION}` - 指定版本（推荐用于生产环境）
- `latest` - 最新稳定版（可能会变化）

### 🍉 镜像列表

| 描述            | Docker Hub                                               | 阿里云容器镜像服务                                                                          |
|---------------|----------------------------------------------------------|------------------------------------------------------------------------------------|
| 网关服务          | `pnoker/dc3-gateway:${SERVICE_VERSION}`                  | `registry.cn-beijing.aliyuncs.com/dc3-gateway:${SERVICE_VERSION}`                  |
| 认证中心          | `pnoker/dc3-center-auth:${SERVICE_VERSION}`              | `registry.cn-beijing.aliyuncs.com/dc3-center-auth:${SERVICE_VERSION}`              |
| 数据中心          | `pnoker/dc3-center-data:${SERVICE_VERSION}`              | `registry.cn-beijing.aliyuncs.com/dc3-center-data:${SERVICE_VERSION}`              |
| 管理中心          | `pnoker/dc3-center-manager:${SERVICE_VERSION}`           | `registry.cn-beijing.aliyuncs.com/dc3-center-manager:${SERVICE_VERSION}`           |
| 单体中心          | `pnoker/dc3-center-single:${SERVICE_VERSION}`            | `registry.cn-beijing.aliyuncs.com/dc3-center-single:${SERVICE_VERSION}`            |
| 监听虚拟驱动        | `pnoker/dc3-driver-listening-virtual:${SERVICE_VERSION}` | `registry.cn-beijing.aliyuncs.com/dc3-driver-listening-virtual:${SERVICE_VERSION}` |
| Modbus TCP 驱动 | `pnoker/dc3-driver-modbus-tcp:${SERVICE_VERSION}`        | `registry.cn-beijing.aliyuncs.com/dc3-driver-modbus-tcp:${SERVICE_VERSION}`        |
| MQTT 驱动       | `pnoker/dc3-driver-mqtt:${SERVICE_VERSION}`              | `registry.cn-beijing.aliyuncs.com/dc3-driver-mqtt:${SERVICE_VERSION}`              |
| OPC DA 驱动     | `pnoker/dc3-driver-opc-da:${SERVICE_VERSION}`            | `registry.cn-beijing.aliyuncs.com/dc3-driver-opc-da:${SERVICE_VERSION}`            |
| OPC UA 驱动     | `pnoker/dc3-driver-opc-ua:${SERVICE_VERSION}`            | `registry.cn-beijing.aliyuncs.com/dc3-driver-opc-ua:${SERVICE_VERSION}`            |
| Siemens S7 驱动 | `pnoker/dc3-driver-plcs7:${SERVICE_VERSION}`             | `registry.cn-beijing.aliyuncs.com/dc3-driver-plcs7:${SERVICE_VERSION}`             |
| 虚拟驱动          | `pnoker/dc3-driver-virtual:${SERVICE_VERSION}`           | `registry.cn-beijing.aliyuncs.com/dc3-driver-virtual:${SERVICE_VERSION}`           |
