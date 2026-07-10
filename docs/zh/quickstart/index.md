---
title: 从源码本地开发
---

# 从源码本地开发

这页带你把 IoT DC3 从源码跑起来：先用 Compose 起好 PostgreSQL 与 RabbitMQ，再用一组本地环境变量把 Java
进程指向本机端口，构建、启动、跑测试。读完你能在自己的机器上跑通整套中心服务，并理解为什么启动有先后、为什么本地进程不能直接吃根
`.env`。

> 你在这里：想从源码开发或调试。只想点一遍最短闭环，请看 [第一个设备：端到端](./first-device)
> ；想理解服务怎么拼起来，请看 [系统架构总览](../architecture/)。

## 先决条件

本地开发是"半容器"模式：基础设施（数据库、消息中间件）跑在容器里，中心服务以本地 Java 进程运行，方便断点调试与热重启。所以你需要一套
JDK/构建工具，外加一个容器运行时。

- **JDK 21** —— 平台强制 Java 21，低版本编译会直接失败。
- **Maven 3.9+** —— 仓库内置 `.mvn/settings.xml` 与并行构建配置；多模块打包用它。
- **pnpm** —— 前端 `dc3-web/` 与同级目录 `dc3-cli/` 都用 pnpm（不要用 npm/yarn）。仅做后端开发可跳过。
- **Podman** —— 本仓库容器操作一律用 `podman`（`make` 默认 `podman compose`）。

## 在 JetBrains IDEA 中开发

如果你用 IntelliJ IDEA（Community 或 Ultimate），以下步骤帮你从零配好项目。

### 1. 打开项目

1. **File → Open**（不是 New → Project from Existing Sources）
2. 选择仓库根目录的 `pom.xml`
3. 弹出对话框选 **Open as Project**
4. 等待 Maven 索引完成（右下角进度条，首次 2-5 分钟）

### 2. 安装 EnvFile 插件

1. **Settings → Plugins → Marketplace**，搜索 **EnvFile**，安装后重启
2. 打开每个服务的 Run Configuration，在 **EnvFile** 标签页点击 `+`，添加 `dc3/env/dev.env`

### 3. 配置运行入口

打开入口类，点击 `main` 左侧绿色按钮 → **Modify Run Configuration**，在 EnvFile 标签页添加 `dc3/env/dev.env`：

| 服务         | 入口类                  | 模块                              |
|------------|----------------------|---------------------------------|
| Gateway    | `GatewayApplication` | `dc3-gateway`                   |
| Auth 中心    | `AuthApplication`    | `dc3-center/dc3-center-auth`    |
| Manager 中心 | `ManagerApplication` | `dc3-center/dc3-center-manager` |
| Data 中心    | `DataApplication`    | `dc3-center/dc3-center-data`    |
| Agentic 中心 | `AgenticApplication` | `dc3-center/dc3-center-agentic` |

### 4. 启动顺序

1. **Auth 中心**（8300）→ 2. **Manager 中心**（8400）→ 3. **Data 中心**（8500）→ 4. **Agentic 中心**（8600）→ 5. **Gateway**
   （8000）

### 5. 常见问题

- **Lombok 报红**：Settings → Annotation Processors → 勾选 **Enable annotation processing**
- **Maven 索引卡住**：File → Invalidate Caches → Invalidate and Restart
- **EnvFile 未生效**：检查 Run Configuration 的 EnvFile 标签页是否已勾选 `dev.env`
- **端口占用**：`lsof -i :8000` 查看，在 Run Configuration 的 Environment variables 中覆盖 `SERVER_PORT`

## 为什么是这五步

本地起栈的最短闭环是五步，每一步都有明确产物，下一步依赖上一步的产物：先有基础设施（容器），才能加载指向它们的环境变量；先构建出
jar，才能启动开发栈；服务起来后才谈得上跑测试。

```mermaid
flowchart LR
  A["make up-db<br/>起 PostgreSQL + RabbitMQ"] -->|"容器就绪<br/>localhost:35432 / 35672"| B["source dc3/env/dev.env.sh<br/>加载本地环境变量"]
  B -->|"Java 进程指向 localhost"| C["make package<br/>多模块构建"]
  C -->|"产出各服务 jar"| D["make up-dev<br/>起本地开发栈"]
  D -->|"中心服务运行<br/>Gateway:8000"| E["make test<br/>跑单元测试"]
  E -->|"验证通过"| F["接入第一个设备"]
```

下面逐步展开，每步都给出"做什么"和"怎么验证"。

## 第一步：起基础设施

`make up-db` 用 Compose 拉起 db 栈——PostgreSQL 与 RabbitMQ。PostgreSQL 首次启动会按文件名顺序执行 initdb
脚本（扩展、common、auth、data、manager、history、agentic），把租户、用户、菜单、元数据表全部建好，所以第一次起会比后续慢。

::: code-group

```bash [全球镜像源]
make up-db
```

```bash [中国大陆镜像源]
make up-db-cn
```

:::

容器对宿主发布的端口是固定的：PostgreSQL `localhost:35432`、RabbitMQ AMQP `localhost:35672`（容器内部仍是 `5432` / `5672`
）。验证：

```bash
podman ps                          # 应看到 dc3-postgres、dc3-rabbitmq 在 running
podman exec dc3-postgres psql -U dc3 -d dc3 -c '\dt dc3_auth.*'   # 能列出 auth 表即就绪
```

::: tip 可选可观测栈
需要 EMQX、ELK、Prometheus、Grafana 时，再执行 `make up-optional`。本地核心开发用不到，建议等中心服务稳定后再起，避免一次性拉起过多容器。
:::

## 第二步：加载本地环境变量

```bash
source dc3/env/dev.env.sh
```

这一步把数据库、RabbitMQ、MQTT、gRPC 目标主机等开发默认值导出到当前 shell。其中关键的是把 `POSTGRES_HOST=localhost`、
`POSTGRES_PORT=35432`、`RABBITMQ_HOST=localhost`、`RABBITMQ_PORT=35672`、以及
`CENTER_AUTH_HOST/MANAGER_HOST/DATA_HOST/AGENTIC_HOST=localhost` 指向本机，让本地 Java 进程能连上第一步发布出来的容器端口。

::: warning .env 只给 Docker Compose 插值，不会注入本地 Java 进程
根目录 `.env` 是 Compose 专用的——它只在 `docker compose` 解析时做变量插值（镜像仓库、镜像 tag、发布端口），**不会**自动注入到你本地起的
Java 进程。本地从源码跑，必须 `source dc3/env/dev.env.sh`；否则服务会沿用容器内 DNS 名（如 `dc3-postgres:5432`
），在本机根本解析不到，连接直接失败。

在 JetBrains IDEA 里运行时，用 EnvFile 插件加载不含 `export` 的 `dc3/env/dev.env`，或把其键值粘进运行配置的环境变量。
:::

验证：`echo $POSTGRES_PORT` 应回显 `35432`。

## 第三步：构建

```bash
make package                       # 等价于 mvn -s .mvn/settings.xml clean package
```

仓库已配好并行构建、强制 JDK 21/Maven 3.9+、Spring Java Format 校验。构建产物是各服务模块的可执行 jar（如
`dc3-gateway/target/dc3-gateway.jar`）。这一步只验证编译与打包，不依赖第一步的容器。

::: tip 快速编译检查
只想确认改动能编译、不想整包，用 `mvn -s .mvn/settings.xml -q -DskipTests compile`，比全量 `package` 快很多。
:::

## 第四步：起开发栈

构建出 jar 后，用 dev 栈把中心服务跑起来。`make up STACK=dev`（或简写 `make up-dev`）按依赖顺序启动
Gateway、Auth、Manager、Data、Agentic 与驱动。

::: code-group

```bash [全球镜像源]
make up-dev
```

```bash [中国大陆镜像源]
make up-dev-cn
```

:::

为什么有先后顺序？因为服务之间有依赖：

- **Auth 中心要先就绪**——它持有租户、用户、RBAC 与令牌签发逻辑，其它服务和网关的鉴权都依赖它。
- **Gateway 是唯一对外 HTTP 入口（8000）**，它聚合 Auth/Manager/Data/Agentic 的路由、抽取鉴权头、注入 principal
  上下文。它要在后端中心服务可达之后才能正确转发，所以排在依赖项之后。

::: details 完整启动顺序与端口
分布式默认走 gRPC（`DC3_FACADE_MODE=grpc`，dev.env 已设）。各服务端口：

| 服务                              | HTTP | gRPC |
|---------------------------------|------|------|
| Gateway / `dc3-gateway`（唯一对外入口） | 8000 | —    |
| 鉴权中心 / `dc3-center-auth`        | 8300 | 9300 |
| 管理中心 / `dc3-center-manager`     | 8400 | 9400 |
| 数据中心 / `dc3-center-data`        | 8500 | 9500 |
| 智能中心 / `dc3-center-agentic`     | 8600 | —    |

只有 Gateway（用户入口）与 listening-virtual 的 TCP 6270 / UDP 6271（设备入口）对宿主映射，其余后端端口都是内部端口。
:::

验证：栈起来后，对网关跑一次登录黄金路径。登录分两步——先取盐，再用盐哈希后的密码换 12 小时有效的 access token：

```bash
# 1) 取盐（公开端点，建议 5 分钟内使用）
curl -s -X POST http://localhost:8000/api/v3/auth/token/salt \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3"}'      # 返回字符串 salt（示例值）

# 2) 用 salt 哈希密码后换 token（公开端点，access token 12 小时有效）
curl -s -X POST http://localhost:8000/api/v3/auth/token/generate \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3","salt":"<上一步的 salt>","password":"<salt 哈希后的密码>"}'
```

拿到 token 后，受保护端点都通过网关访问，并带上三个鉴权头 `X-Auth-Tenant`、`X-Auth-Login`、`X-Auth-Token`。完整的"建驱动 →
建模板 → 建设备 → 读写位号"闭环见 [第一个设备：端到端](./first-device)。

## 第五步：跑测试

```bash
make test                          # 单元测试套件
```

需要更高层验证时：`make test-it` 跑集成测试（需容器运行时供 Testcontainers），`make test-e2e` 跑后端 E2E。日常开发改完一处，先
`make test` 兜住单元回归即可。

## 常见坑

- **没 `source dev.env.sh` 就起服务**——最常见。本地 Java 进程拿不到 `localhost:35432` / `35672`，会去连容器内 DNS
  名而连不上。每开一个新 shell 都要重新 `source`（变量只在当前 shell 生效）。
- **podman 没起**——`make up-db` 报连不上容器运行时，先确认 `podman` 守护进程/machine 已启动（macOS 上需
  `podman machine start`），再用 `podman ps` 确认。
- **端口被占**——`35432` / `35672` / `8000` 等被其它进程占用时启动失败。释放占用进程，或在根 `.env` 中覆盖 Compose
  发布端口（仅影响容器侧），本地进程端口用服务级环境变量覆盖。
- **首次起库很慢或表不全**——PostgreSQL 只在空卷首启时跑 initdb。若中途中断导致表不全，需重置卷重来：`make reset STACK=db`（需
  `CONFIRM_RESET_VOLUMES=true`，会删数据，谨慎）。

::: danger 生产密钥必须替换
`dev.env.sh` 里的 `DC3_SECURITY_KEY` 与 `AUTH_HMAC_SECRET` 是开发默认值。在 `pre`/`pro` 环境，若 `AUTH_HMAC_SECRET`
为空或仍等于默认值 `io.github.pnoker.dc3`，Gateway 会 fail-fast 拒绝启动。本地开发无妨，上线前务必换成环境专属随机值。
:::

## 延伸阅读

- [环境变量详解](./environment) — `.env` 与 `dev.env(.sh)` 的边界、每个变量的作用域与默认值
- [第一个设备：端到端](./first-device) — 登录后从建驱动到读写位号的最短闭环
- [系统架构总览](../architecture/) — 五个中心服务如何分工、数据与命令如何流转
