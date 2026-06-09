# 环境变量

IoT DC3 同时支持 Compose 容器运行和本地源码运行。两类环境变量的读取者不同，不能混用。

## 文件区别

| 文件 | 读取者 | 用途 |
|------|--------|------|
| `.env.example` | Compose 模板 | 复制为根目录 `.env` 后，供 `dc3/docker-compose*.yml` 做变量插值 |
| `.env` | Compose | 本机未跟踪配置，控制镜像仓库、镜像标签、发布端口等 |
| `dc3/env/dev.env` | IDE EnvFile 插件或手动复制 | 本地 Java 进程环境变量，不带 `export` |
| `dc3/env/dev.env.sh` | Shell | 本地 Java 进程环境变量，使用 `source` 加载 |

本地源码运行 Java 服务时，应使用 `dc3/env/dev.env.sh` 或 `dc3/env/dev.env`。根目录 `.env` 主要给 Compose 插值，不会自动注入到所有本地 Java 进程。

## Compose 用法

先从模板创建本地 `.env`：

```bash
cp .env.example .env
```

然后在仓库根目录运行 Compose 相关命令：

```bash
make dev-all
make app-all REGISTRY=cn
podman compose -f dc3/docker-compose-dev.yml config
```

根目录 `.env` 常用于 Compose 插值，例如：

```yaml
image: ${DC3_IMAGE_REGISTRY:-pnoker}/dc3-gateway:${DC3_IMAGE_TAG:-2026.5}
ports:
  - "${DC3_BIND_HOST:-127.0.0.1}:${DC3_GATEWAY_PORT:-8000}:8000"
```

Compose 不会把 `.env` 中的每个变量都注入到每个容器。只有 compose 文件中通过 `environment`、`env_file` 或其他配置引用的变量才会进入容器。

## 本地源码运行

从命令行启动 Java 进程前，加载：

```bash
source dc3/env/dev.env.sh
```

这组变量把应用指向本机已发布端口：

```bash
POSTGRES_HOST=localhost
POSTGRES_PORT=35432
RABBITMQ_HOST=localhost
RABBITMQ_PORT=35672
CENTER_AUTH_HOST=localhost
NODE_ENV=dev
DC3_FACADE_MODE=grpc
```

如果不加载，本地 Java 进程可能继续使用容器内服务名或默认端口，例如 `dc3-postgres`、`dc3-rabbitmq`、`dc3-center-manager`，从而无法连接本机依赖。

## JetBrains IDEA 用法

`dc3/env/dev.env` 是给 IDE 使用的文件，内容与 `dev.env.sh` 对齐，但没有 `export`。

推荐配置：

1. 安装 JetBrains EnvFile 插件。
2. 打开目标服务的 Run Configuration。
3. 启用 EnvFile。
4. 添加 `dc3/env/dev.env`。
5. 只在需要时补充服务级覆盖项，例如 `SERVER_PORT`、`GRPC_SERVER_PORT`、`TCP_PORT`、`UDP_PORT`、`POSTGRES_SCHEMA`。

如果不使用 EnvFile 插件，可以把 `dc3/env/dev.env` 中的键值对复制到 Run Configuration 的 `Environment variables`。

不要把 `.env.example` 直接作为 IDEA 环境变量文件。它是 Compose 模板，不是本地 Java 进程运行时配置。

## 变量参考

### Compose 通用变量

| 变量 | 范围 | 说明 |
|------|------|------|
| `DC3_IMAGE_REGISTRY` | Compose | DC3 镜像仓库命名空间；中国大陆镜像可用 `registry.cn-beijing.aliyuncs.com/dc3` |
| `DC3_IMAGE_TAG` | Compose | DC3 应用和依赖镜像默认标签 |
| `DC3_BIND_HOST` | Compose | 发布端口绑定地址；本机调试建议 `127.0.0.1` |
| `DC3_LOG_MAX_SIZE` | Compose | 单个容器日志文件轮转大小 |
| `DC3_LOG_MAX_FILE` | Compose | 保留的日志文件数量 |
| `APM_AGENT_ENABLE` | Compose / Runtime | 是否启用 Java APM agent |

### PostgreSQL 和 RabbitMQ

| 变量 | 范围 | 说明 |
|------|------|------|
| `POSTGRES_HOST` | Runtime | Java 进程看到的 PostgreSQL 主机；源码运行通常为 `localhost` |
| `POSTGRES_PORT` | Runtime | Java 进程看到的 PostgreSQL 端口；源码运行通常为 `35432` |
| `POSTGRES_USERNAME` | Runtime / Compose | PostgreSQL 用户名 |
| `POSTGRES_PASSWORD` | Runtime | PostgreSQL 密码 |
| `POSTGRES_DB` | Runtime / Compose | PostgreSQL 数据库名 |
| `POSTGRES_SCHEMA` | Per-process | 单服务 schema 覆盖，例如 `dc3_manager` 或 `dc3_data` |
| `DC3_POSTGRES_PORT` | Compose | PostgreSQL 容器发布到宿主机的端口 |
| `RABBITMQ_HOST` | Runtime | RabbitMQ 主机；源码运行通常为 `localhost` |
| `RABBITMQ_PORT` | Runtime | RabbitMQ AMQP 端口；源码运行通常为 `35672` |
| `RABBITMQ_USERNAME` | Runtime | RabbitMQ 用户名 |
| `RABBITMQ_PASSWORD` | Runtime | RabbitMQ 密码 |
| `RABBITMQ_VIRTUAL_HOST` | Runtime | RabbitMQ virtual host |
| `DC3_RABBITMQ_PORT` | Compose | RabbitMQ AMQP 发布端口 |
| `DC3_RABBITMQ_TLS_PORT` | Compose | RabbitMQ TLS 发布端口 |
| `DC3_RABBITMQ_MANAGEMENT_PORT` | Compose | RabbitMQ 管理界面发布端口 |

### 应用端口和运行模式

| 变量 | 范围 | 说明 |
|------|------|------|
| `DC3_WEB_HTTP_PORT` | Compose | `dc3-web` HTTP 发布端口 |
| `DC3_WEB_HTTPS_PORT` | Compose | `dc3-web` HTTPS 发布端口 |
| `DC3_WEB_VERSION` | Compose | 前端镜像标签 |
| `APP_API_HOST` | Runtime | `dc3-web` 容器看到的 Gateway 主机 |
| `APP_API_PORT` | Runtime | `dc3-web` 容器看到的 Gateway 端口 |
| `DC3_GATEWAY_PORT` | Compose | Gateway HTTP 发布端口 |
| `DC3_AUTH_PORT` | Compose | Auth Center HTTP 发布端口 |
| `DC3_AUTH_GRPC_PORT` | Compose | Auth Center gRPC 发布端口 |
| `DC3_MANAGER_PORT` | Compose | Manager Center HTTP 发布端口 |
| `DC3_MANAGER_GRPC_PORT` | Compose | Manager Center gRPC 发布端口 |
| `DC3_DATA_PORT` | Compose | Data Center HTTP 发布端口 |
| `DC3_DATA_GRPC_PORT` | Compose | Data Center gRPC 发布端口 |
| `DC3_AGENTIC_PORT` | Compose | Agentic Center HTTP 发布端口 |
| `DC3_LISTENING_VIRTUAL_TCP_PORT` | Compose | Listening Virtual 驱动 TCP 发布端口 |
| `DC3_LISTENING_VIRTUAL_UDP_PORT` | Compose | Listening Virtual 驱动 UDP 发布端口 |
| `SERVER_PORT` | Per-process | 单个 Spring Boot 进程 HTTP 端口覆盖 |
| `GRPC_SERVER_PORT` | Per-process | 单个中心服务 gRPC 端口覆盖 |
| `TCP_PORT` | Per-process | Listening Virtual 驱动内部 TCP 端口 |
| `UDP_PORT` | Per-process | Listening Virtual 驱动内部 UDP 端口 |
| `NODE_ENV` | Runtime | Spring profile 分组；源码运行通常为 `dev` |
| `DC3_FACADE_MODE` | Runtime | 跨服务 facade 模式；开发环境通常为 `grpc` |

### 中心服务地址和 Gateway 路由

| 变量 | 范围 | 说明 |
|------|------|------|
| `CENTER_AUTH_HOST` | Runtime | 本地进程访问 Auth Center 的主机 |
| `CENTER_MANAGER_HOST` | Runtime | 本地进程访问 Manager Center 的主机 |
| `CENTER_DATA_HOST` | Runtime | 本地进程访问 Data Center 的主机 |
| `CENTER_AGENTIC_HOST` | Runtime | 本地进程访问 Agentic Center 的主机 |
| `GATEWAY_ROUTE_AUTH_TOKEN_URI` | Per-process | Auth token 路由覆盖 |
| `GATEWAY_ROUTE_AUTH_URI` | Per-process | Auth 服务路由覆盖 |
| `GATEWAY_ROUTE_MANAGER_URI` | Per-process | Manager 服务路由覆盖 |
| `GATEWAY_ROUTE_DATA_URI` | Per-process | Data 服务路由覆盖 |
| `GATEWAY_ROUTE_AGENTIC_URI` | Per-process | Agentic 服务路由覆盖 |

### Agentic 和 OpenAI-compatible API

| 变量 | 范围 | 说明 |
|------|------|------|
| `AGENTIC_FALLBACK_OPENAI_BASE_URL` | Runtime | fallback OpenAI-compatible API 地址 |
| `AGENTIC_FALLBACK_OPENAI_API_KEY` | Runtime | fallback API key |
| `AGENTIC_FALLBACK_OPENAI_MODEL` | Runtime | fallback 模型名 |
| `AGENTIC_FALLBACK_OPENAI_TEMPERATURE` | Runtime | fallback 采样温度 |
| `AGENTIC_FALLBACK_OPENAI_MAX_TOKENS` | Runtime | fallback 最大输出 token |
| `AGENTIC_MEMORY_SCHEMA_INIT` | Runtime | Spring AI JDBC memory schema 初始化模式 |
| `AGENTIC_MEMORY_ENABLED` | Runtime | 是否启用持久化会话记忆 |
| `AGENTIC_TOOL_CALLING_ENABLED` | Runtime | 是否启用工具调用 |
| `AGENTIC_MEMORY_MAX_MESSAGES` | Runtime | 每个对话窗口保留的最大消息数 |
| `AGENTIC_ATTACHMENT_STORAGE_PATH` | Runtime | Agentic 附件存储路径 |

真实 API key 不应写入文档、日志或提交历史。

### MQTT 和点位处理

| 变量 | 范围 | 说明 |
|------|------|------|
| `MQTT_BROKER_HOST` | Runtime | MQTT broker 主机 |
| `MQTT_BROKER_PORT` | Runtime | MQTT broker 端口；源码运行通常为 EMQX 发布端口 `31883` |
| `MQTT_USERNAME` | Runtime | MQTT 用户名 |
| `MQTT_PASSWORD` | Runtime | MQTT 密码 |
| `MQTT_BATCH_SPEED` | Runtime | MQTT 消息批处理速度阈值 |
| `MQTT_BATCH_INTERVAL` | Runtime | MQTT 批处理调度间隔 |
| `POINT_BATCH_SPEED` | Runtime | 点位值处理批处理速度阈值 |
| `POINT_BATCH_INTERVAL` | Runtime | 点位值批处理调度间隔 |

### gRPC Facade

| 变量 | 范围 | 说明 |
|------|------|------|
| `DC3_FACADE_GRPC_DEADLINE_MS` | Runtime | gRPC facade 单次请求 deadline，设置 `0` 可关闭客户端 deadline |

### Auth 和 HMAC 签名

| 变量 | 范围 | 说明 |
|------|------|------|
| `AUTH_HMAC_SECRET` | Runtime | Gateway 与后端服务之间用于签名 `X-Auth-User` 的共享 HMAC-SHA256 密钥；生产环境应设置强随机值 |

### 可选依赖和可观测栈

| 变量 | 范围 | 说明 |
|------|------|------|
| `DC3_EMQX_WS_PORT` | Compose | EMQX WebSocket 发布端口 |
| `DC3_EMQX_WSS_PORT` | Compose | EMQX Secure WebSocket 发布端口 |
| `DC3_EMQX_MQTT_PORT` | Compose | EMQX MQTT 发布端口 |
| `DC3_EMQX_MQTTS_PORT` | Compose | EMQX MQTTS 发布端口 |
| `DC3_EMQX_DASHBOARD_PORT` | Compose | EMQX Dashboard 发布端口 |
| `GF_SERVER_ROOT_URL` | Compose | Grafana 对外 root URL |
| `DC3_GRAFANA_PORT` | Compose | Grafana 发布端口 |
| `DC3_KIBANA_PORT` | Compose | Kibana 发布端口 |
| `DC3_ES_JAVA_OPTS` | Compose | Elasticsearch JVM 参数 |
| `DC3_LS_JAVA_OPTS` | Compose | Logstash JVM 参数 |

## 对齐规则

- Compose-only 变量保留在 `.env.example`，例如 `DC3_IMAGE_REGISTRY`、`DC3_IMAGE_TAG`、`DC3_BIND_HOST`、`DC3_*_PORT`。
- 本地源码运行变量应在 `.env.example`、`dc3/env/dev.env` 和 `dc3/env/dev.env.sh` 中保持一致。
- Per-process 变量只作为单服务覆盖项使用，不应全局滥用。
- 服务发布端口变量使用 `DC3_*_PORT`，内部 Spring Boot 变量保留 `SERVER_PORT`、`GRPC_SERVER_PORT` 等原生命名。

## 常见误区

- 修改 `.env.example` 不会影响运行；需要先复制为 `.env`。
- `dc3/env/dev.env` 和根目录 `.env` 用途不同，不要当作同一个文件。
- 在根目录 `.env` 设置 `POSTGRES_HOST=localhost` 不会自动改变每个容器的运行时环境。
- Compose 应用栈可以使用与本地源码不同的 `NODE_ENV`。
- `DC3_LISTENING_VIRTUAL_TCP_PORT` / `DC3_LISTENING_VIRTUAL_UDP_PORT` 是宿主机发布端口；`TCP_PORT` / `UDP_PORT` 是进程内部端口。
