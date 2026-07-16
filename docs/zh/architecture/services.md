---
title: 服务与拓扑
---

<script setup>
import ServicesFlowDiagram from '../../.vitepress/theme/components/ServicesFlowDiagram.vue'
import ServicesSequenceDiagram from '../../.vitepress/theme/components/ServicesSequenceDiagram.vue'
</script>


# 服务与拓扑

IoT DC3 不是一个单体进程，而是一组可独立部署、靠 gRPC 与 RabbitMQ 协作的服务。这页讲清平台由哪些可部署单元构成、它们之间怎么连线、以及为什么必须按某个固定顺序启动——读完你能看懂
`docker-compose.yml` 里的每一条 `depends_on`，也能自己排查"为什么网关起不来"。

> 你在这里：已读 [系统架构总览](./)，想把"五个中心 + 驱动"
> 落到具体进程、端口和启动次序。下一步可看 [Facade 模式](./facade-modes)
> 理解服务间怎么调用，或直接 [快速开始](../quickstart/) 把这套栈跑起来。

## 为什么拆成这么多单元

把平台拆成网关、四个中心和一组驱动，不是为了"微服务而微服务"
，而是因为这几类职责的伸缩与故障边界天然不同：南向的协议驱动数量多、按现场扩，和北向的元数据管理完全两回事；鉴权是所有请求的必经关卡，应当独立且先就绪；时序数据写入吞吐高，需要单独的数据中心承接
RabbitMQ 消息洪峰。拆开之后，每一类单元可以单独扩容、单独重启、单独定位问题。

平台共有**六类可部署单元**，外加一个把全部中心打包进单进程的 single 单体：

- **网关 Gateway（`dc3-gateway`）**——唯一的对外 API 聚合入口，做鉴权头解析、HMAC 签名注入、路由转发，并承载 MCP 资源服务器
  `/mcp`（app 栈下经 Web 前端反代触达，见下文端口小节）。
- **鉴权中心 Auth Center（`dc3-center-auth`）**——认证、租户、RBAC、OAuth 2.1 授权服务器。无业务依赖，最先就绪。
- **管理中心 Manager Center（`dc3-center-manager`）**——驱动、模板、设备、位号等元数据管理。
- **数据中心 Data Center（`dc3-center-data`）**——位号值落库、命令分发与回执、告警引擎。
- **智能中心 Agentic Center（`dc3-center-agentic`）**——Spring AI 会话、工具调用、对话持久化。
- **协议驱动 Drivers（`dc3-driver-*`）**——驱动目录共 28 个协议适配实现，`docker-compose.yml` 默认内置其中 22 个驱动容器（未内置
  `ble`/`iec104`/`lwm2m`/`sl651`/`zigbee`/`can` 这 6 个，需要时自行启动对应容器）；南向接设备、北向经 RabbitMQ
  与数据中心解耦。
- **single 单体（`dc3-center-single`）**——把四个中心的能力合并进一个进程，用 `dc3.facade.mode: local`
  在进程内直连，适合本地开发与轻量部署（见 [Facade 模式](./facade-modes)）。

::: info 中心服务首次出现给"中文名 + 标识"
后文沿用术语表口径，"数据中心"即 `dc3-center-data`，"网关"即 `dc3-gateway`，不再重复全称。
:::

## 谁监听哪个端口

每个单元同时可能暴露一个 HTTP 端口（对外或对内的 REST 入口）和一个 gRPC 端口（中心之间 facade 调用）。**关键约束：网关是唯一的对外
API 聚合入口，但它的 HTTP `8000` 在 app 栈里并不发布到宿主机**——`docker-compose.yml` 只把 Web 前端 `8080/8443` 与
listening-virtual 驱动的设备入站口映射到宿主机，外部请求经 Web 前端的 nginx 反代到容器内的 `dc3-gateway:8000`。网关 `8000`
与其余中心的 HTTP 端口一样只在容器网络内可达；只有在 dev 栈（`docker-compose-dev.yml`）里网关 `8000`
才发布到宿主机（同时各中心端口也一并发布）。生产环境不应把后端端口映射到宿主机。

下图按"谁依赖谁先就绪"画出服务拓扑：实线箭头是 `depends_on` 健康依赖，标注的是各服务的 HTTP / gRPC 端口。

<ServicesFlowDiagram lang="zh" />

端口分配是有规律的：HTTP 端口 `83/84/85/86xx` 与 gRPC 端口 `93/94/95xx` 一一对应到 auth/manager/data。智能中心目前只暴露
HTTP `8600`。single 单体则独占 HTTP `8100` / gRPC `9100`（`DC3_SINGLE_PORT` / `DC3_SINGLE_GRPC_PORT`），与分布式栈端口不冲突，可在同机并存。

下表把上图的端口与对宿主机的发布情况固化为参考；写代码或配 Nginx 反向代理时以此为准。"对宿主机发布"一列指 app 栈（
`docker-compose.yml`）的实际 `ports:` 映射：

| 单元                            | HTTP            | gRPC   | app 栈对宿主机发布                               | 环境变量（发布端口）                                   |
|-------------------------------|-----------------|--------|-------------------------------------------|----------------------------------------------|
| Web 前端 `dc3-web`              | `8080` / `8443` | —      | **是（app 栈唯一 HTTP 入口，nginx 反代到网关）**        | `DC3_WEB_HTTP_PORT` / `DC3_WEB_HTTPS_PORT`   |
| 网关 `dc3-gateway`              | `8000`          | —      | 否（仅容器网络内可达；dev 栈才用 `DC3_GATEWAY_PORT` 发布） | `DC3_GATEWAY_PORT`                           |
| 鉴权中心 `dc3-center-auth`        | `8300`          | `9300` | 否                                         | `DC3_AUTH_PORT` / `DC3_AUTH_GRPC_PORT`       |
| 管理中心 `dc3-center-manager`     | `8400`          | `9400` | 否                                         | `DC3_MANAGER_PORT` / `DC3_MANAGER_GRPC_PORT` |
| 数据中心 `dc3-center-data`        | `8500`          | `9500` | 否                                         | `DC3_DATA_PORT` / `DC3_DATA_GRPC_PORT`       |
| 智能中心 `dc3-center-agentic`     | `8600`          | —      | 否                                         | `DC3_AGENTIC_PORT`                           |
| single 单体 `dc3-center-single` | `8100`          | `9100` | 视部署                                       | `DC3_SINGLE_PORT` / `DC3_SINGLE_GRPC_PORT`   |

::: warning 后端 HTTP 端口在 app 栈里都不发布到宿主机
在 `docker-compose.yml`（app 栈）里，网关与 auth/manager/data/agentic 都**没有** `ports:` 映射——它们只在 `dc3net`
容器网络内可达。被映射到宿主机的只有 Web 前端 `8080/8443`（`DC3_WEB_HTTP_PORT` / `DC3_WEB_HTTPS_PORT`）和 listening-virtual
驱动的设备入站口 TCP `6270` / UDP `6271`（`DC3_LISTENING_VIRTUAL_TCP_PORT` / `..._UDP_PORT`）。因此在 app 栈下，从外部访问业务
API 要走 Web 前端 `8080`，由其 nginx 反代到容器内的 `dc3-gateway:8000`；宿主机无法直连 `8000`。只有 dev 栈（
`docker-compose-dev.yml`）才把网关 `8000`（及各中心端口）发布到宿主机，可直连。
:::

## 以什么顺序启动

服务之间有硬性的就绪顺序：网关要给请求注入鉴权信息，就得 auth 先在；data 落库和分发命令要先有 manager 的元数据；agentic
要读数据、调命令，就得 auth/manager/data 都在。这套顺序不是写在文档里靠人记，而是用 Compose 的
`depends_on: condition: service_healthy` 强制——**被依赖方健康检查通过，依赖方才启动**。

健康判定统一用各服务的 `/actuator/health/readiness`（readiness 探针）。注意中心服务的 readiness 路径带 base-path 前缀，网关不带：

- 网关：`http://127.0.0.1:8000/actuator/health/readiness`
- 鉴权中心：`http://127.0.0.1:8300/auth/actuator/health/readiness`
- 管理中心：`http://127.0.0.1:8400/manager/actuator/health/readiness`
- 数据中心：`http://127.0.0.1:8500/data/actuator/health/readiness`
- 智能中心：`http://127.0.0.1:8600/agentic/actuator/health/readiness`

下图按依赖链画出从基础依赖到驱动的整条就绪时序——每一跳都等上游 readiness 通过后才开始。

<ServicesSequenceDiagram lang="zh" />

这张图也解释了一个常见现象：**驱动只依赖 manager**，不依赖网关或数据中心。驱动起来后向管理中心 gRPC
注册自己（带协议属性定义），再开始按计划采集、经 RabbitMQ 把位号值推给数据中心——所以驱动可以和网关并行启动，无需等网关就绪。

::: danger 基础依赖必须先于全部应用就绪
`docker-compose.yml`（应用栈）里的中心服务**不包含** PostgreSQL 与 RabbitMQ——它们在独立的 `docker-compose-db.yml`（db
栈）里，分别以 `pg_isready` 和 `rabbitmq-diagnostics ping` 做健康检查。应用栈假定这两者已经健康。因此正确的启动序列是**先起
db 栈、等其健康，再起应用栈**；跳过这一步，auth 会因连不上库反复重启。对应命令见下。
:::

## 把这套栈跑起来

实际操作分两步：先拉起 db 栈（PostgreSQL + RabbitMQ），再拉起应用栈。Makefile 把 Compose 细节包好了，命令在 `iot-dc3/` 目录下执行：

::: code-group

```bash [make（推荐）]
# 1. 先起基础依赖，等健康
make up-db

# 2. 起应用栈（构建镜像 + 按 depends_on 顺序启动）
make up STACK=app

# 跟随日志，确认各服务 readiness 依次通过
make logs
```

```bash [podman compose（底层）]
# db 栈：postgres + rabbitmq
podman compose -f dc3/docker-compose-db.yml up -d

# 应用栈：gateway + 四中心 + 驱动
podman compose -f dc3/docker-compose.yml up -d

# 校验 compose 语法
podman compose -f dc3/docker-compose.yml config --quiet
```

:::

起栈后确认整链就绪。注意 app 栈下网关 `8000` 不发布到宿主机，宿主机直连 `127.0.0.1:8000` 会连接失败——要么进网关容器内探（与容器
healthcheck 同址），要么从宿主机走 Web 前端 `8080`：

```bash
# app 栈：在网关容器内探 readiness（期望返回 {"status":"UP"}）
podman exec dc3-gateway curl -fsS http://127.0.0.1:8000/actuator/health/readiness

# 宿主机入口是 Web 前端 8080（nginx 反代到 dc3-gateway:8000）
curl -fsS http://127.0.0.1:8080/
```

::: info dev 栈才能从宿主机直连网关 8000
若用 `make up-dev`（dev 栈，`docker-compose-dev.yml`）启动，网关 `8000` 会发布到宿主机，此时可直接 `curl
-fsS http://127.0.0.1:8000/actuator/health/readiness`。
:::

::: tip 本地开发可用 single 单体免去多进程编排
若只想在本机快速验证业务逻辑，不必拉起六个容器：`dc3-center-single` 以 `dc3.facade.mode: local` 在进程内直连各中心能力，监听
HTTP `8100` / gRPC `9100`。分布式与单体之间只是部署拓扑差异，不改变业务语义——详见 [Facade 模式](./facade-modes)。
:::

## 约束与边界

- **网关是唯一的对外 API 聚合入口，但宿主机入口因栈而异**。在 app 栈（`docker-compose.yml`）里只有 Web 前端 `8080/8443` 与
  listening-virtual 的设备入站口 TCP `6270`/UDP `6271` 映射到宿主机，网关 `8000` 不发布、外部请求经 Web 前端 nginx 反代到
  `dc3-gateway:8000`；dev 栈（`docker-compose-dev.yml`）才把网关 `8000` 与各中心端口一并发布到宿主机。无论哪种栈，其余后端端口都只在容器网络内可达，生产环境不要映射到宿主机。
- **启动顺序由健康检查保证，不靠人工 sleep**。`depends_on: condition: service_healthy` 让依赖方等到被依赖方 readiness
  通过才启动；但这只覆盖应用栈内部，db 栈必须由你先行拉起。
- **readiness 路径带 base-path**。中心服务用 `webflux.base-path`（如 auth 的 `/auth`），探针路径相应带前缀；网关不带。写监控/探活脚本时别漏掉前缀。
- **分布式默认走 gRPC facade**。manager 等中心的 `dc3.facade.mode` 默认 `${DC3_FACADE_MODE:grpc}`，`dc3/env/dev.env` 也设为
  `grpc`；single 单体的基础 `application.yml` 才声明 `local`
  。这是部署拓扑选择，不是协议选择，细节见 [Facade 模式](./facade-modes)。

## 延伸阅读

- [系统架构总览](./) — 闭环的整体视角与各角色定位
- [Facade 模式](./facade-modes) — `grpc`（分布式）与 `local`（单体）如何切换、为何这是拓扑而非协议选择
- [快速开始](../quickstart/) — 本地从零起栈、跑通第一个设备
- [鉴权 · 租户 · RBAC](./auth-rbac) — 网关如何注入鉴权头与 HMAC 签名
