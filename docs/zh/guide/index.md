---
title: 部署运维
---

<script setup>
import GuideIndexDiagram from '../../.vitepress/theme/components/GuideIndexDiagram.vue'
</script>


# 部署运维

把 IoT DC3 从一台开发机的 `java -jar`
推到一组容器编排，再让它在生产里可观测、可排障——这一栏覆盖部署形态、镜像源、可观测栈、日志规范与故障排查。读完本页，你会知道每个话题在哪、以及"
本地开发"和"容器化部署"两条路线的分界在哪里。

> 你在这里：已经[本地起栈跑通第一个设备](../quickstart/)，现在要把它部署、观测、运维起来。

## 两条路线，先分清边界

部署运维的所有话题，最终都落在两条路线之一，两者的环境变量来源不同，混用是最常见的坑：

- **本地开发**：依赖（PostgreSQL、RabbitMQ、可选 EMQX/ELK/Prometheus）跑在容器里，但 Java 进程（网关与四个中心、驱动）在宿主机
  IDE 或 `java -jar` 里直接运行。这条路线由 [快速开始](../quickstart/) 负责，调试快、改代码即时生效。
- **容器化部署**：网关、四个中心、驱动连同依赖全部以容器形式编排启动。这条路线由 [部署模式与镜像源](./usage) 负责。

::: warning 环境变量不会自动串台
根目录 `.env` **只服务 Docker Compose**——它不会自动注入到本机的 Java 进程。本地以 IDE 或 `java -jar` 跑 Java 时，必须改用
`dc3/env/dev.env`（IDE EnvFile 插件读取）或 `source dc3/env/dev.env.sh`（shell 导出），把服务指向 Compose 在 `localhost`
上发布的端口（如 PostgreSQL `35432`、RabbitMQ `35672`）。把容器内主机名（`dc3-postgres`、`dc3-rabbitmq`）填进本地 Java
进程，连接必然失败。
:::

## 这一栏怎么读

四个子页各管一段运维生命周期：先把服务**起起来**（部署与镜像源），再让它**看得见**（可观测性、日志），最后在出问题时**修得动**
（故障排查）。

<GuideIndexDiagram lang="zh" />

- **[部署模式与镜像源](./usage)** — 容器镜像选择、镜像仓库切换、Compose 编排。`make` 选择镜像源用 `REGISTRY`（`auto`/
  `global`/`cn`），`global` 走默认仓库、`cn` 走中国大陆镜像；最快验证起栈是 `make up-db`，国内网络改用 `make up-db-cn`。
- **[可观测性](./observability)** — 应用与依赖如何接入 Grafana / Prometheus / ELK（可选 `optional` 栈）。用
  `make up-optional` 拉起 EMQX/ELK/Prometheus/Grafana 这套可选栈，端口见环境变量目录里的"Observability Stack"一节（Grafana
  `3000`、Kibana `5601`）。
- **[日志规范](./logging)** — `dc3-common-log` 统一输出控制台彩色日志（人工调试）与滚动 JSON 文件日志（机器解析，含
  timestamp/logger/thread/level/MDC/message/stack）；消息用英文稳定事件名 + SLF4J 参数化占位符，便于跨模块搜索关联。
- **[故障排查](./troubleshooting)** — 构建慢、JDK 版本、端口占用、DB/MQ 连接失败、Gateway 401/403、驱动无法注册等高频问题的定位与处理。

## 几个最常用的命令

容器栈的生命周期统一走 `make`，命令模式是 `make <op>-<stack>[-<registry>]`。下面是部署运维里最常敲的几条（在 `iot-dc3/`
目录执行）：

::: code-group

```bash [启动依赖栈]
# 启动 PostgreSQL + RabbitMQ（最小依赖）
make up-db

# 国内网络改用中国大陆镜像源
make up-db-cn

# 追加可选可观测栈：EMQX / ELK / Prometheus / Grafana
make up-optional
```

```bash [查看日志]
# 跟随某个栈的日志（最后 200 行）
make logs STACK=db

# 只看指定服务
make logs SERVICES="gateway agentic"
```

```bash [本地源码运行前置]
# 让本地 Java 进程指向 Compose 发布到 localhost 的端口
source dc3/env/dev.env.sh
```

:::

::: tip 启动顺序
分布式起栈时按 Auth → Manager → Data → Agentic → Gateway → Driver 顺序启动：Auth 无依赖最先起，四个中心健康后 Gateway 才启动（
`gateway` 的 `depends_on` 为 auth/manager/data/agentic 均 `service_healthy`），驱动依赖 Manager Center 与 RabbitMQ
就绪后才能注册。详见 [故障排查 · 驱动无法注册](./troubleshooting)。
:::

## 验证服务通了：调一次黄金路径

部署完成后，确认网关与鉴权链路是否打通，最快的方式是走一遍登录：先取盐，再用加盐口令换 token。所有对外请求都经唯一 HTTP
入口网关（默认 `8000`）。

```bash
# 1) 取盐（公开端点，建议 5 分钟内使用；以下租户/用户名为示例值）
curl -X POST http://localhost:8000/api/v3/auth/token/salt \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3"}'

# 2) 用盐对口令做哈希后换取 token（12 小时有效）
curl -X POST http://localhost:8000/api/v3/auth/token/generate \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"default","name":"dc3","salt":"<上一步返回的盐>","password":"<加盐哈希>"}'
```

拿到 token 后，受保护端点需要带上三个鉴权头：`X-Auth-Tenant`、`X-Auth-Login`、`X-Auth-Token`。若此处返回 401/403，多半是 token
缺失或过期，处理见 [故障排查 · Gateway 返回 401 或 403](./troubleshooting)。

## 延伸阅读

- [部署模式与镜像源](./usage) — 容器镜像、镜像仓库切换与 Compose 编排
- [可观测性](./observability) — Grafana / Prometheus / ELK 对接
- [日志规范](./logging) — 日志消息风格、级别与输出格式约定
- [故障排查](./troubleshooting) — 启动与连接问题的定位与处理
- [快速开始](../quickstart/) — 本地起栈并跑通第一个设备（本地开发路线起点）
