# 快速开始

从源码到运行，一站式本地开发流程。

::: tip 运行目录
除非特别说明，下文命令都在仓库根目录执行。
:::

## 前置依赖

- JDK 21
- Maven 3.9+
- Podman 或 Docker
- Make（可选，但推荐）

## 1. 启动基础设施

```bash
# 全球镜像源
make dev-db

# 中国大陆镜像源
make dev-db REGISTRY=cn
```

会拉起 PostgreSQL 与 RabbitMQ。

## 2. （可选）启动可选依赖

```bash
make dev-optional
# 或
make dev-optional REGISTRY=cn
```

通常用于 EMQX 等 MQTT broker。

## 3. 加载环境变量（源码运行必需）

```bash
source dc3/env/dev.env.sh
```

文件中会导出数据库、消息中间件、gRPC 目标、可选 AI 集成等开发默认值。源码方式启动 Java 进程时必须加载这组变量，否则服务会继续使用容器内 DNS 名称或默认端口，无法正确连接本机已发布的 PostgreSQL、RabbitMQ 和中心服务。

根目录 `.env.example` 与 `dc3/env/dev.env(.sh)` 的具体区别参见 [环境变量](environment.md)。

## 4. 在 JetBrains IDEA 中运行

IDEA 运行配置可使用 `dc3/env/dev.env` 配合 EnvFile 插件，或将其键值对粘贴到运行配置的环境变量中。仅在需要时按服务覆盖：
`SERVER_PORT`、`GRPC_SERVER_PORT`、`TCP_PORT`、`UDP_PORT`、`POSTGRES_SCHEMA`。

详情见 [环境变量 § JetBrains IDEA](environment.md)。

## 5. 源码构建

```bash
# 并行构建（快）
mvn -s .mvn/settings.xml clean package

# 部署 API/Common 模块（如有需要）
make deploy
```

仓库已配置：

- 并行构建（`-T 1C`）
- 强制 JDK 21、Maven 3.9+
- Spring Java Format 校验

`make package` 和上面的 Maven 命令都会执行标准打包流程；是否跳过测试以当前 Maven 配置和命令参数为准。

## 6. 启动服务（推荐顺序）

```bash
java -jar dc3-gateway/target/dc3-gateway.jar
java -jar dc3-center/dc3-center-auth/target/dc3-center-auth.jar
java -jar dc3-center/dc3-center-manager/target/dc3-center-manager.jar
java -jar dc3-center/dc3-center-data/target/dc3-center-data.jar
java -jar dc3-center/dc3-center-agentic/target/dc3-center-agentic.jar
java -jar dc3-driver/dc3-driver-virtual/target/dc3-driver-virtual.jar
```

## 7. 通过 Compose 启动（替代方式）

```bash
# 本地开发栈
make dev
make dev REGISTRY=cn

# 完整本地环境（db + optional + dev）
make dev-all
make dev-all REGISTRY=cn

# 打包镜像应用栈
make app
make app REGISTRY=cn
make app-all REGISTRY=cn
```

## 8. 常用维护命令

```bash
# 打印解析后的 compose 文件
make compose-file STACK=dev

# 查看日志
make compose-logs STACK=dev

# 容器状态
make compose-ps STACK=dev

# 重启
make compose-restart STACK=dev

# 仅启动选定服务（前端/接口测试）
make dev-db REGISTRY=cn
make up SERVICES="gateway agentic" REGISTRY=cn
make logs SERVICES="gateway agentic"
```

## 9. 默认服务端口

| 服务             | HTTP | gRPC |
|----------------|------|------|
| Gateway        | 8000 | -    |
| Auth Center    | 8300 | 9300 |
| Manager Center | 8400 | 9400 |
| Data Center    | 8500 | 9500 |
| Agentic Center | 8600 | -    |

## 10. 推荐本地工作流

1. `make dev-db`
2. `mvn -s .mvn/settings.xml clean package`
3. 按 Gateway → Auth → Manager → Data → Agentic → Driver 顺序启动
4. 通过 Gateway `http://localhost:8000/api/v3/...` 测试 API
5. 如需可观测栈，待核心平台稳定后再启动 Grafana / ELK

## 11. 常见坑

- 如果 pre/pro profile 需要 Nacos，确保注册中心可用
- 端口被占用时，可在根 `.env` 中覆盖 Compose 发布端口，或通过服务级环境变量覆盖进程端口
- 调试时如希望单 JVM 跑全部服务，使用 `dc3-center-single`：

```bash
java -jar dc3-center/dc3-center-single/target/dc3-center-single.jar
```
