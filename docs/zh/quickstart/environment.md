---
title: 环境变量详解
---

<script setup>
import EnvironmentDiagram from '../../.vitepress/theme/components/EnvironmentDiagram.vue'
</script>


# 环境变量详解

IoT DC3 有两套环境变量文件，读取者完全不同：根目录 `.env` 给 Docker Compose 做插值，`dc3/env/dev.env(.sh)` 给本地 Java
进程。读完这页，你能分清哪个变量该写进哪个文件、`localhost` 端口与容器内端口为何不一样，以及生产前必须改掉的两个密钥默认值。

> 你在这里：已经[从源码本地开发](./)或用 Compose 起栈。下一步看[部署模式与镜像源](../guide/usage)了解整套栈如何拉起。

## 为什么有两套文件

同一个变量名（如 `POSTGRES_HOST`），在容器里和在你笔记本上的 Java 进程里，含义不一样。容器之间靠 Compose 网络上的服务名（
`dc3-postgres`）互访；而你本地 IDE 里跑的 Java 进程在宿主机上，只能通过 Compose **发布到宿主机的端口**（如 `localhost:35432`
）连进去。两套文件就是为这两条互不重叠的路径准备的——混用会让本地进程去连一个解析不了的容器名，或让容器去连一个它根本看不到的
`localhost`。

| 文件                   | 读取者               | 用途                                      | 注入方式             |
|----------------------|-------------------|-----------------------------------------|------------------|
| `.env.example`       | Docker Compose 模板 | 复制为根目录 `.env`，定义镜像仓库、镜像标签、发布端口          | Compose 变量插值     |
| `.env`               | Docker Compose    | 本机未跟踪配置，供 `dc3/docker-compose*.yml` 插值  | Compose 变量插值     |
| `dc3/env/dev.env`    | IDE（EnvFile 插件）   | 本地 Java 进程环境变量，**不带** `export`          | IDE EnvFile 插件读取 |
| `dc3/env/dev.env.sh` | Shell             | 本地 Java 进程环境变量，带 `export`，用 `source` 加载 | Shell 环境注入       |

::: warning 根 `.env` 不会注入本地 Java 进程
根目录 `.env` 只服务 Docker Compose，**不会**自动注入到本地 IDE/命令行启动的 Java 进程。本地源码运行必须用
`dc3/env/dev.env(.sh)`，让进程指向 Compose 发布在 `localhost` 上的依赖端口。在根 `.env` 里写 `POSTGRES_HOST=localhost`
也不会改变任何容器的运行时环境。
:::

## 两条互不注入的路径

下图说明两套文件如何沿各自路径生效。关键是这两条路径**互不交叉**：Compose 不读 `dev.env.sh`，本地 Java 进程也不自动读根
`.env`。

<EnvironmentDiagram lang="zh" />

host 端口与 internal 端口的对应关系，是理解这张图的核心：

| 依赖            | host（本地进程用）       | internal（容器互访用）      |
|---------------|-------------------|----------------------|
| PostgreSQL    | `localhost:35432` | `dc3-postgres:5432`  |
| RabbitMQ AMQP | `localhost:35672` | `dc3-rabbitmq:5672`  |
| EMQX MQTT     | `localhost:31883` | `dc3-emqx:1883`（约定值） |

## 怎么用

### Compose 起栈

先从模板创建本地 `.env`，再用 `make`（底层是 `podman compose`）拉起：

::: code-group

```bash [make]
cp .env.example .env
make up-db && make up-optional && make up-dev
```

```bash [podman compose]
cp .env.example .env
podman compose -f dc3/docker-compose-dev.yml config --quiet
```

:::

根 `.env` 的变量用于 compose 文件插值，例如镜像与发布端口：

```yaml
image: ${DC3_IMAGE_REGISTRY:-pnoker}/dc3-gateway:${DC3_IMAGE_TAG:-2026.6}
ports:
  - "${DC3_BIND_HOST:-127.0.0.1}:${DC3_GATEWAY_PORT:-8000}:8000"
```

Compose 不会把 `.env` 里每个变量都注入每个容器——只有 compose 文件通过 `environment`、`env_file` 显式引用的变量才会进入容器。

### 本地源码运行

从命令行启动 Java 进程前先 `source`：

```bash
source dc3/env/dev.env.sh
```

不加载时，本地进程会回退到容器内服务名（`dc3-postgres`、`dc3-rabbitmq`、`dc3-center-manager`）或默认端口，从而连不上本机依赖。JetBrains
IDEA 用户改用 `dc3/env/dev.env`（同样内容、无 `export`）：安装 EnvFile 插件 → 在 Run Configuration 启用 EnvFile → 添加
`dc3/env/dev.env`。不要把 `.env.example` 直接当作 IDEA 环境变量文件——它是 Compose 模板，不是 Java 运行时配置。

## 按场景分组的关键变量

下面按"为某个场景实际需要"的角度分组列出关键变量。完整长表在文末 `::: details` 折叠，这里只列你会真正改动或排错时查的那些。变量的
`Scope` 三类含义：`Runtime`（本地或容器内 Java 进程都读）、`Compose only`（仅根 `.env` 给 Compose）、`Per-process`（单服务覆盖）。

### 安全密钥（Runtime）

两个密钥是平台的身份根。它们自带默认值仅为方便首次启动，绝不可带进生产。

| 变量                 | 默认值                                      | 用途                                                  |
|--------------------|------------------------------------------|-----------------------------------------------------|
| `DC3_SECURITY_KEY` | `dc3.security.key.2026.io.github.pnoker` | 鉴权中心生成/校验登录 Token 的签名密钥                             |
| `AUTH_HMAC_SECRET` | `io.github.pnoker.dc3`                   | Gateway 向后端服务签名 `X-Auth-Principal` 的 HMAC-SHA256 密钥 |

::: danger 生产必须改成强随机值，且切勿提交/打印
`DC3_SECURITY_KEY` 与 `AUTH_HMAC_SECRET` 自带默认值，生产环境必须改成强随机值，切勿提交进仓库、写入日志或打印真实密钥。当
Spring profile 为 `pre`/`pro` 且 `AUTH_HMAC_SECRET` 为空或仍等于默认值 `io.github.pnoker.dc3` 时，服务会 fail-fast 抛
`IllegalStateException` 拒绝启动——这是有意为之的安全闸门，不要绕过。
:::

### PostgreSQL（Runtime）

本地进程连 `localhost:35432`；容器内连 `dc3-postgres:5432`。`POSTGRES_SCHEMA` 只在单服务进程里做 schema 覆盖（如
`dc3_manager`、`dc3_data`），不要全局设。

| 变量                  | 默认值         | Scope        | 用途                                  |
|---------------------|-------------|--------------|-------------------------------------|
| `POSTGRES_HOST`     | `localhost` | Runtime      | 本地用 `localhost`，容器内用 `dc3-postgres` |
| `POSTGRES_PORT`     | `35432`     | Runtime      | host 发布端口；internal 为 `5432`         |
| `POSTGRES_USERNAME` | `dc3`       | Runtime      | 用户名                                 |
| `POSTGRES_PASSWORD` | `dc3dc3dc3` | Runtime      | 密码                                  |
| `POSTGRES_DB`       | `dc3`       | Runtime      | 数据库名                                |
| `POSTGRES_SCHEMA`   | (未设)        | Per-process  | 单服务 schema 覆盖                       |
| `DC3_POSTGRES_PORT` | `35432`     | Compose only | 容器发布到宿主机的端口                         |

### RabbitMQ（Runtime）

AMQP host 端口 `35672`，internal `5672`。开启 TLS 时内部端口切到 `5671`。

| 变量                             | 默认值         | Scope        | 用途                                |
|--------------------------------|-------------|--------------|-----------------------------------|
| `RABBITMQ_HOST`                | `localhost` | Runtime      | 本地 `localhost`，容器内 `dc3-rabbitmq` |
| `RABBITMQ_PORT`                | `35672`     | Runtime      | AMQP host 端口；internal `5672`      |
| `RABBITMQ_USERNAME`            | `dc3`       | Runtime      | 用户名                               |
| `RABBITMQ_PASSWORD`            | `dc3dc3dc3` | Runtime      | 密码                                |
| `RABBITMQ_VIRTUAL_HOST`        | `dc3`       | Runtime      | virtual host                      |
| `RABBITMQ_SSL_ENABLED`         | `false`     | Runtime      | 启用 TLS（true 时走 5671）              |
| `DC3_RABBITMQ_PORT`            | `35672`     | Compose only | AMQP 发布端口                         |
| `DC3_RABBITMQ_MANAGEMENT_PORT` | `15672`     | Compose only | 管理界面发布端口                          |

### EMQX / MQTT（Runtime）

MQTT broker host 端口 `31883`。EMQX 还发布 WebSocket、Dashboard 等多个端口，详见折叠长表。

| 变量                        | 默认值         | Scope        | 用途                                   |
|---------------------------|-------------|--------------|--------------------------------------|
| `MQTT_BROKER_HOST`        | `localhost` | Runtime      | broker 主机                            |
| `MQTT_BROKER_PORT`        | `31883`     | Runtime      | broker 端口（EMQX 发布；internal 约 `1883`） |
| `MQTT_USERNAME`           | `dc3`       | Runtime      | 用户名                                  |
| `MQTT_PASSWORD`           | `dc3dc3dc3` | Runtime      | 密码                                   |
| `DC3_EMQX_MQTT_PORT`      | `31883`     | Compose only | MQTT 发布端口                            |
| `DC3_EMQX_DASHBOARD_PORT` | `18083`     | Compose only | Dashboard 发布端口                       |

### gRPC / facade（Runtime）

中心服务之间通过 facade 互联。分布式部署默认 `DC3_FACADE_MODE=grpc`，本地进程用 `CENTER_*_HOST` 指到 `localhost`。

| 变量                            | 默认值         | Scope   | 用途                                    |
|-------------------------------|-------------|---------|---------------------------------------|
| `CENTER_AUTH_HOST`            | `localhost` | Runtime | 鉴权中心主机                                |
| `CENTER_MANAGER_HOST`         | `localhost` | Runtime | 管理中心主机                                |
| `CENTER_DATA_HOST`            | `localhost` | Runtime | 数据中心主机                                |
| `CENTER_AGENTIC_HOST`         | `localhost` | Runtime | 智能中心主机                                |
| `DC3_FACADE_MODE`             | `grpc`      | Runtime | facade 协议模式                           |
| `DC3_FACADE_GRPC_DEADLINE_MS` | `3000`      | Runtime | gRPC 单次请求 deadline，`0` 关闭客户端 deadline |

### 网关与服务端口（Compose only）

网关是唯一对外 HTTP 入口（`8000`）。各中心 HTTP/gRPC 发布端口如下；`SERVER_PORT`/`GRPC_SERVER_PORT`
仅在本地同时跑多个服务、需要错开端口时作单进程覆盖。

| 变量                               | 默认值    | Scope        | 用途                          |
|----------------------------------|--------|--------------|-----------------------------|
| `DC3_GATEWAY_PORT`               | `8000` | Compose only | 网关 HTTP 发布端口（入口）            |
| `DC3_AUTH_PORT`                  | `8300` | Compose only | 鉴权中心 HTTP                   |
| `DC3_MANAGER_PORT`               | `8400` | Compose only | 管理中心 HTTP                   |
| `DC3_DATA_PORT`                  | `8500` | Compose only | 数据中心 HTTP                   |
| `DC3_AGENTIC_PORT`               | `8600` | Compose only | 智能中心 HTTP                   |
| `DC3_AUTH_GRPC_PORT`             | `9300` | Compose only | 鉴权中心 gRPC                   |
| `DC3_MANAGER_GRPC_PORT`          | `9400` | Compose only | 管理中心 gRPC                   |
| `DC3_DATA_GRPC_PORT`             | `9500` | Compose only | 数据中心 gRPC                   |
| `DC3_LISTENING_VIRTUAL_TCP_PORT` | `6270` | Compose only | Listening Virtual 驱动 TCP 发布 |
| `DC3_LISTENING_VIRTUAL_UDP_PORT` | `6271` | Compose only | Listening Virtual 驱动 UDP 发布 |
| `SERVER_PORT`                    | (未设)   | Per-process  | 单服务 HTTP 端口覆盖               |
| `GRPC_SERVER_PORT`               | (未设)   | Per-process  | 单中心 gRPC 端口覆盖               |

::: warning `DC3_LISTENING_VIRTUAL_*_PORT` 是宿主机发布端口
`DC3_LISTENING_VIRTUAL_TCP_PORT`/`DC3_LISTENING_VIRTUAL_UDP_PORT` 是 Compose 发布到宿主机的端口；进程内部端口用
`TCP_PORT`/`UDP_PORT`（Per-process），两者别混。
:::

### Agentic / AI（Runtime）

仅当 `dc3_model_provider` 没有配置可用提供方时，才回退到这组 `AGENTIC_FALLBACK_OPENAI_*`。会话记忆默认关闭。

| 变量                                    | 默认值                            | 用途                                                               |
|---------------------------------------|--------------------------------|------------------------------------------------------------------|
| `AGENTIC_FALLBACK_OPENAI_BASE_URL`    | `https://api.openai.com`       | fallback OpenAI 兼容 API 地址                                        |
| `AGENTIC_FALLBACK_OPENAI_API_KEY`     | (空)                            | fallback API key（端点需鉴权时填）                                        |
| `AGENTIC_FALLBACK_OPENAI_MODEL`       | `gpt-4o`                       | fallback 模型名                                                     |
| `AGENTIC_FALLBACK_OPENAI_TEMPERATURE` | `0.7`                          | 采样温度（0.0–2.0）                                                    |
| `AGENTIC_FALLBACK_OPENAI_MAX_TOKENS`  | `2048`                         | 最大输出 token                                                       |
| `AGENTIC_MEMORY_SCHEMA_INIT`          | `never`                        | Spring AI JDBC 记忆表初始化模式（`always`/`never`/`create_if_not_exists`） |
| `AGENTIC_MEMORY_ENABLED`              | `false`                        | 是否启用持久化会话记忆                                                      |
| `AGENTIC_TOOL_CALLING_ENABLED`        | `true`                         | 是否启用工具调用                                                         |
| `AGENTIC_MEMORY_MAX_MESSAGES`         | `50`                           | 每个对话窗口保留的最大消息数                                                   |
| `AGENTIC_ATTACHMENT_STORAGE_PATH`     | `dc3/data/agentic/attachments` | 附件存储路径                                                           |

::: info `AGENTIC_MEMORY_SCHEMA_INIT` 的建表绑定以代码为准
`AGENTIC_MEMORY_SCHEMA_INIT` 默认 `never`，意在控制记忆表初始化模式。但该变量经 compose 注入容器后，仓库内未见
`application*.yml` 把它绑定到 Spring AI 的 `initialize-schema`；记忆表实际由 initdb 脚本预建。是否真能通过设为 `always`
触发自动建表，请以代码为准，不要当成已接线的可用开关。
:::

::: danger 真实 API key 不进文档/日志/提交历史
`AGENTIC_FALLBACK_OPENAI_API_KEY` 等敏感值绝不能写入文档、日志或提交历史。正式提供方应配置在 `dc3_model_provider`
表，fallback 仅作兜底。
:::

::: info 表中默认值取自 `.env.example`，与代码内回退默认不一致
上表 `AGENTIC_MEMORY_ENABLED`（`false`）、`AGENTIC_ATTACHMENT_STORAGE_PATH`（`dc3/data/agentic/attachments`）取自
`.env.example`——走 `cp .env.example .env` + `source` 路径时会被显式注入这些值。但 `application-agentic.yml`
的代码内回退默认不同（未设环境变量时记忆默认开启、附件路径为 `dc3/data/upload/agentic/attachment`）。不走 `.env.example`
路径时以代码为准。
:::

### 批处理（Runtime）

MQTT 与位号值各有一组"数量阈值 + 间隔"参数，由 Quartz 定时按 `interval`（秒）把累积缓冲一次性刷出，
`speed = count / interval`。

| 变量                     | 默认值   | 用途               |
|------------------------|-------|------------------|
| `MQTT_BATCH_SPEED`     | `100` | MQTT 批量大小阈值（条/批） |
| `MQTT_BATCH_INTERVAL`  | `5`   | MQTT 批处理间隔（秒）    |
| `POINT_BATCH_SPEED`    | `100` | 位号值批量大小阈值        |
| `POINT_BATCH_INTERVAL` | `5`   | 位号值批处理间隔（秒）      |

### 镜像源（Compose only）

中国大陆网络可把 `REGISTRY` 设为 `cn` 走阿里云镜像。注意：Makefile 读 `REGISTRY`，Compose 插值读 `DC3_IMAGE_REGISTRY`
，两者各管一段。

| 变量                   | 默认值         | 用途                                               |
|----------------------|-------------|--------------------------------------------------|
| `REGISTRY`           | `auto`      | Makefile 镜像源选择器（仅接受 `auto`/`global`/`cn`，其它值会报错） |
| `DC3_IMAGE_REGISTRY` | `pnoker`    | 镜像命名空间                                           |
| `DC3_IMAGE_TAG`      | `2026.6`    | 所有服务/依赖镜像标签                                      |
| `DC3_BIND_HOST`      | `127.0.0.1` | 发布端口绑定地址（`0.0.0.0` 对外）                           |

### 可观测性（Compose only / Runtime）

可选栈（EMQX、ELK、Prometheus、Grafana）通过 `make up-optional` 拉起，端口与 JVM 参数如下。

| 变量                   | 默认值                     | Scope        | 用途                  |
|----------------------|-------------------------|--------------|---------------------|
| `GF_SERVER_ROOT_URL` | `http://localhost:3000` | Runtime      | Grafana 对外 root URL |
| `DC3_GRAFANA_PORT`   | `3000`                  | Compose only | Grafana 发布端口        |
| `DC3_KIBANA_PORT`    | `5601`                  | Compose only | Kibana 发布端口         |
| `DC3_ES_JAVA_OPTS`   | `-Xms512m -Xmx512m`     | Runtime      | Elasticsearch JVM 堆 |
| `DC3_LS_JAVA_OPTS`   | `-Xms256m -Xmx256m`     | Runtime      | Logstash JVM 堆      |
| `APM_AGENT_ENABLE`   | `false`                 | Runtime      | 是否启用 Java APM agent |

::: details 完整变量参考（折叠）

#### Security & Authentication（Runtime）

| 变量                 | 默认值                                      | 用途                                |
|--------------------|------------------------------------------|-----------------------------------|
| `DC3_SECURITY_KEY` | `dc3.security.key.2026.io.github.pnoker` | 登录 Token 签名密钥                     |
| `AUTH_HMAC_SECRET` | `io.github.pnoker.dc3`                   | `X-Auth-Principal` HMAC-SHA256 密钥 |

#### PostgreSQL

| 变量                  | 默认值         | Scope        |
|---------------------|-------------|--------------|
| `POSTGRES_HOST`     | `localhost` | Runtime      |
| `POSTGRES_PORT`     | `35432`     | Runtime      |
| `POSTGRES_USERNAME` | `dc3`       | Runtime      |
| `POSTGRES_PASSWORD` | `dc3dc3dc3` | Runtime      |
| `POSTGRES_DB`       | `dc3`       | Runtime      |
| `POSTGRES_SCHEMA`   | (未设)        | Per-process  |
| `DC3_POSTGRES_PORT` | `35432`     | Compose only |

#### RabbitMQ

| 变量                                         | 默认值          | Scope        |
|--------------------------------------------|--------------|--------------|
| `RABBITMQ_HOST`                            | `localhost`  | Runtime      |
| `RABBITMQ_PORT`                            | `35672`      | Runtime      |
| `RABBITMQ_USERNAME`                        | `dc3`        | Runtime      |
| `RABBITMQ_PASSWORD`                        | `dc3dc3dc3`  | Runtime      |
| `RABBITMQ_VIRTUAL_HOST`                    | `dc3`        | Runtime      |
| `RABBITMQ_MQTT_EXCHANGE`                   | `dc3.e.mqtt` | Runtime      |
| `RABBITMQ_SSL_ENABLED`                     | `false`      | Runtime      |
| `RABBITMQ_SSL_ALGORITHM`                   | `TLS`        | Runtime      |
| `RABBITMQ_SSL_VALIDATE_SERVER_CERTIFICATE` | `false`      | Runtime      |
| `RABBITMQ_SSL_VERIFY_HOSTNAME`             | `false`      | Runtime      |
| `RABBITMQ_CONTAINER_PORT`                  | `5672`       | Runtime      |
| `DC3_RABBITMQ_PORT`                        | `35672`      | Compose only |
| `DC3_RABBITMQ_TLS_PORT`                    | `35671`      | Compose only |
| `DC3_RABBITMQ_MANAGEMENT_PORT`             | `15672`      | Compose only |

#### EMQX / MQTT

| 变量                        | 默认值         | Scope        |
|---------------------------|-------------|--------------|
| `MQTT_BROKER_HOST`        | `localhost` | Runtime      |
| `MQTT_BROKER_PORT`        | `31883`     | Runtime      |
| `MQTT_USERNAME`           | `dc3`       | Runtime      |
| `MQTT_PASSWORD`           | `dc3dc3dc3` | Runtime      |
| `MQTT_BATCH_SPEED`        | `100`       | Runtime      |
| `MQTT_BATCH_INTERVAL`     | `5`         | Runtime      |
| `DC3_EMQX_WS_PORT`        | `38083`     | Compose only |
| `DC3_EMQX_WSS_PORT`       | `38084`     | Compose only |
| `DC3_EMQX_MQTT_PORT`      | `31883`     | Compose only |
| `DC3_EMQX_MQTTS_PORT`     | `38883`     | Compose only |
| `DC3_EMQX_DASHBOARD_PORT` | `18083`     | Compose only |

#### gRPC / facade

| 变量                            | 默认值         | Scope        |
|-------------------------------|-------------|--------------|
| `CENTER_AUTH_HOST`            | `localhost` | Runtime      |
| `CENTER_MANAGER_HOST`         | `localhost` | Runtime      |
| `CENTER_DATA_HOST`            | `localhost` | Runtime      |
| `CENTER_AGENTIC_HOST`         | `localhost` | Runtime      |
| `DC3_FACADE_MODE`             | `grpc`      | Runtime      |
| `DC3_FACADE_GRPC_DEADLINE_MS` | `3000`      | Runtime      |
| `DC3_AUTH_GRPC_PORT`          | `9300`      | Compose only |
| `DC3_MANAGER_GRPC_PORT`       | `9400`      | Compose only |
| `DC3_DATA_GRPC_PORT`          | `9500`      | Compose only |

#### HTTP Gateway & 服务端口

| 变量                               | 默认值    | Scope        |
|----------------------------------|--------|--------------|
| `DC3_GATEWAY_PORT`               | `8000` | Compose only |
| `DC3_AUTH_PORT`                  | `8300` | Compose only |
| `DC3_MANAGER_PORT`               | `8400` | Compose only |
| `DC3_DATA_PORT`                  | `8500` | Compose only |
| `DC3_AGENTIC_PORT`               | `8600` | Compose only |
| `SERVER_PORT`                    | (未设)   | Per-process  |
| `GRPC_SERVER_PORT`               | (未设)   | Per-process  |
| `DC3_LISTENING_VIRTUAL_TCP_PORT` | `6270` | Compose only |
| `DC3_LISTENING_VIRTUAL_UDP_PORT` | `6271` | Compose only |
| `TCP_PORT`                       | (未设)   | Per-process  |
| `UDP_PORT`                       | (未设)   | Per-process  |
| `GATEWAY_ROUTE_AUTH_TOKEN_URI`   | (未设)   | Per-process  |
| `GATEWAY_ROUTE_AUTH_URI`         | (未设)   | Per-process  |
| `GATEWAY_ROUTE_MANAGER_URI`      | (未设)   | Per-process  |
| `GATEWAY_ROUTE_DATA_URI`         | (未设)   | Per-process  |
| `GATEWAY_ROUTE_AGENTIC_URI`      | (未设)   | Per-process  |

#### Agentic / AI（Runtime）

| 变量                                    | 默认值                            |
|---------------------------------------|--------------------------------|
| `AGENTIC_FALLBACK_OPENAI_BASE_URL`    | `https://api.openai.com`       |
| `AGENTIC_FALLBACK_OPENAI_API_KEY`     | (空)                            |
| `AGENTIC_FALLBACK_OPENAI_MODEL`       | `gpt-4o`                       |
| `AGENTIC_FALLBACK_OPENAI_TEMPERATURE` | `0.7`                          |
| `AGENTIC_FALLBACK_OPENAI_MAX_TOKENS`  | `2048`                         |
| `AGENTIC_MEMORY_SCHEMA_INIT`          | `never`                        |
| `AGENTIC_MEMORY_ENABLED`              | `false`                        |
| `AGENTIC_MEMORY_MAX_MESSAGES`         | `50`                           |
| `AGENTIC_TOOL_CALLING_ENABLED`        | `true`                         |
| `AGENTIC_ATTACHMENT_STORAGE_PATH`     | `dc3/data/agentic/attachments` |

#### 批处理 / 镜像 / 可观测性

| 变量                     | 默认值                     | Scope        |
|------------------------|-------------------------|--------------|
| `POINT_BATCH_SPEED`    | `100`                   | Runtime      |
| `POINT_BATCH_INTERVAL` | `5`                     | Runtime      |
| `REGISTRY`             | `auto`                  | Compose only |
| `DC3_IMAGE_REGISTRY`   | `pnoker`                | Compose only |
| `DC3_IMAGE_TAG`        | `2026.6`                | Compose only |
| `DC3_LOG_MAX_SIZE`     | `10M`                   | Compose only |
| `DC3_LOG_MAX_FILE`     | `20`                    | Compose only |
| `DC3_BIND_HOST`        | `127.0.0.1`             | Compose only |
| `GF_SERVER_ROOT_URL`   | `http://localhost:3000` | Runtime      |
| `DC3_GRAFANA_PORT`     | `3000`                  | Compose only |
| `DC3_KIBANA_PORT`      | `5601`                  | Compose only |
| `DC3_ES_JAVA_OPTS`     | `-Xms512m -Xmx512m`     | Runtime      |
| `DC3_LS_JAVA_OPTS`     | `-Xms256m -Xmx256m`     | Runtime      |
| `APM_AGENT_ENABLE`     | `false`                 | Runtime      |
| `NODE_ENV`             | `dev`                   | Runtime      |

:::

## 约束与常见误区

- 改 `.env.example` 不会影响运行——必须先 `cp .env.example .env`。
- `dc3/env/dev.env` 与根 `.env` 用途不同，不是同一个文件，别互相复制。
- 服务发布端口统一用 `DC3_*_PORT`；进程内部仍用 `SERVER_PORT`、`GRPC_SERVER_PORT` 等 Spring Boot 原生命名。
- Per-process 变量（`POSTGRES_SCHEMA`、`SERVER_PORT`、`TCP_PORT` 等）只作单服务覆盖，不要全局滥用。
- Compose 应用栈可以用与本地源码不同的 `NODE_ENV`。

## 延伸阅读

- [从源码本地开发](./) — 起栈、登录、跑通第一个设备的完整路径
- [部署模式与镜像源](../guide/usage) — 整套栈如何拉起、`REGISTRY=cn` 怎么用
