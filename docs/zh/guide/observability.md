---
title: 可观测性
---

<script setup>
import ObservabilityDiagram from '../../.vitepress/theme/components/ObservabilityDiagram.vue'
</script>


# 可观测性

IoT DC3 的日志聚合与指标监控是一套**可选**栈：一条命令 `make up-optional` 拉起 EMQX、ELK（Elasticsearch + Logstash +
Kibana）、Prometheus 与 Grafana。读完这页你能知道每个组件干什么、暴露在哪个端口、怎么把服务日志接进 Kibana、把服务指标接进
Grafana，以及用哪些环境变量调堆内存和开关 APM。

> 你在这里：已能[部署并启动平台](./usage)，想给运行中的环境加上日志检索与指标看板。

::: info 可观测性栈为可选
这套栈不在默认启动范围内。`make up-db`（PostgreSQL + RabbitMQ）与应用栈起来后平台即可运行；可观测性组件需要单独
`make up-optional` 才会启动，对内存有额外要求，按需开启。
:::

## 为什么单独成栈

把可观测性从核心栈里拆出来，是为了让"跑通平台"和"观测平台"两件事互不绑定。评估者只想验证黄金路径时，不必为 Elasticsearch
付出几百兆堆内存；运维要排障、看趋势时，再把这套栈叠加上来即可。它由 `dc3/docker-compose-optional.yml` 定义，与 db/dev/app
三个核心栈平行，共享同一个 `dc3net` 网络，因此各服务能用容器别名（如 `dc3-elasticsearch`、`dc3-prometheus`）互相寻址。

这套栈解决两类问题：

- **日志（logs）**——服务输出的 JSON 文件日志由 Logstash 收集、归一，写入 Elasticsearch，在 Kibana 里按字段检索与关联。
- **指标（metrics）**——Prometheus 周期抓取各服务与 exporter 暴露的指标，Grafana 把它们画成看板。

此外 EMQX 作为 MQTT broker 一并放在这个栈里，供 MQTT 类驱动与设备直连接入。

## 组件与端口

`make up-optional` 一次拉起以下容器（端口为宿主机发布端口，默认绑定 `127.0.0.1`，由 `DC3_BIND_HOST` 控制）。下表只作速查，逐项作用见后文。

| 容器                      | 作用                      | 宿主机端口（默认）                                               | 控制变量                                             |
|-------------------------|-------------------------|---------------------------------------------------------|--------------------------------------------------|
| `dc3-emqx`              | MQTT broker + Dashboard | MQTT `31883`、Dashboard `18083`、WS `38083`、MQTTS `38883` | `DC3_EMQX_MQTT_PORT` / `DC3_EMQX_DASHBOARD_PORT` |
| `dc3-elasticsearch`     | 日志存储与检索引擎               | 内部 `9200`（不发布）                                          | `DC3_ES_JAVA_OPTS`                               |
| `dc3-logstash`          | 日志采集与归一管道               | 内部（不发布）                                                 | `DC3_LS_JAVA_OPTS`                               |
| `dc3-kibana`            | 日志检索与可视化 UI             | `5601`                                                  | `DC3_KIBANA_PORT`                                |
| `dc3-apm`               | APM（应用性能）数据接收           | 内部（不发布）                                                 | `APM_AGENT_ENABLE`（应用侧开关）                        |
| `dc3-prometheus`        | 指标抓取与时序存储               | 内部 `9090`（不发布），保留 `7d`                                  | —                                                |
| `dc3-postgres-exporter` | PostgreSQL 指标导出         | 内部（不发布）                                                 | —                                                |
| `dc3-nginx-exporter`    | 前端 nginx 指标导出           | 内部（不发布）                                                 | —                                                |
| `dc3-grafana`           | 指标看板 UI                 | `3000`                                                  | `DC3_GRAFANA_PORT` / `GF_SERVER_ROOT_URL`        |

::: tip 只想起其中几个？
不必整栈拉起。用 `SERVICES` 过滤，例如只要监控：

```bash
make up STACK=optional SERVICES="prometheus grafana"
```

:::

EMQX 的端口在 [部署模式与镜像源](./usage) 里也会用到——MQTT 驱动连 `31883`、运维登录 Dashboard 看连接情况走 `18083`
。Kibana（`5601`）与 Grafana（`3000`）是两个面向人的入口：前者查日志，后者看指标。Elasticsearch、Logstash、APM、Prometheus 与两个
exporter 都**不对宿主机发布端口**，只在 `dc3net` 内部互通——它们是后端管线，不直接给人访问。

## 日志如何接入（ELK）

服务以 JSON 文件形式落日志（消息风格见[日志规范](./logging)），落盘目录通过名为 `logs` 的 Docker 卷共享：核心栈把日志写进该卷，Logstash
把同一个卷挂载到 `/usr/share/logstash/dc3/logs` 读取。Logstash 解析、打标后写入 Elasticsearch，最终在 Kibana 里检索。

<ObservabilityDiagram lang="zh" />

接入步骤就是把这条链路跑起来：

1. 先确保核心栈（dev 或 app）在跑，日志已写进 `logs` 卷。
2. `make up-optional` 启动 ELK，Logstash 自动从 `logs` 卷读取。
3. 浏览器打开 `http://localhost:5601` 进入 Kibana，按服务名、`tenantId`、事件名等字段检索。

::: warning Elasticsearch 吃内存，先调堆
Elasticsearch 与 Logstash 的 JVM 堆默认偏小，便于在开发机起得来：`DC3_ES_JAVA_OPTS` 默认 `-Xms512m -Xmx512m`，
`DC3_LS_JAVA_OPTS` 默认 `-Xms256m -Xmx256m`。生产或日志量大时按机器内存上调，例如：

```bash
DC3_ES_JAVA_OPTS="-Xms2g -Xmx2g" make up-optional
```

:::

### APM 默认关闭

`dc3-apm` 容器随 ELK 一起起来，但**应用是否上报 APM 数据由 `APM_AGENT_ENABLE` 决定，默认 `false`**。该变量作用在核心栈（
`docker-compose.yml` / `docker-compose-dev.yml`）上，控制服务是否挂载 Java APM Agent 向 `dc3-apm` 上报性能数据。要启用需在启动核心栈时显式打开：

```bash
APM_AGENT_ENABLE=true make up STACK=app
```

::: info 起了 apm 容器 ≠ 开了 APM
只 `make up-optional` 不会自动采集 APM——`dc3-apm` 仅是接收端。没有把核心栈的 `APM_AGENT_ENABLE` 设为 `true`，应用不会挂载
Agent，也就没有数据上报。
:::

## 指标如何接入（Prometheus / Grafana）

各服务通过 Micrometer 暴露 Prometheus 格式的指标端点；Prometheus 按其配置周期抓取这些端点，并抓取两个 exporter——
`postgres-exporter`（数据库指标）与 `nginx-exporter`（前端 nginx 指标）。Prometheus 本地保留 7 天时序数据（
`--storage.tsdb.retention.time=7d`），Grafana 以它为数据源画看板。

接入步骤：

1. `make up-optional` 启动 Prometheus、两个 exporter 与 Grafana。
2. 浏览器打开 `http://localhost:3000` 进入 Grafana 看板。
3. Grafana 的外部访问地址由 `GF_SERVER_ROOT_URL` 控制（默认 `http://localhost:3000`）；若通过反向代理或非本机访问，相应调整该变量，避免生成的链接指向
   `localhost`。

```bash
# 反代/远程访问时修正 Grafana 根地址（示例值）
GF_SERVER_ROOT_URL="https://ops.example.com/grafana" make up-optional
```

Prometheus 自身不对宿主机发布端口，通常通过 Grafana 间接查询；要直接看 Prometheus，可临时在 compose 里为其加一个端口映射，或用
`podman exec` 进容器排查。

## 约束与边界

- **非默认启动**：核心链路不依赖这套栈。它宕了不影响设备接入、命令下发与数据落库——只是少了日志检索与指标看板。
- **内存门槛**：Elasticsearch 是这套栈里最重的组件。在 8GB 内存的开发机上，建议按 `DC3_ES_JAVA_OPTS` 默认值或更低运行，并优先用
  `SERVICES` 过滤只起需要的组件。
- **数据保留**：Prometheus 固定保留 7 天；更长留存需改 `--storage.tsdb.retention.time` 或外接长期存储。Elasticsearch 与
  Logstash 的数据落在各自的命名卷（`elasticsearch`、`logstash`、`logs`），`make reset` 会一并删除，谨慎使用。
- **APM 双重开关**：`dc3-apm` 容器在可选栈、`APM_AGENT_ENABLE` 开关在核心栈，两者都到位才有 APM 数据（见上文）。
- **端口绑定**：默认只绑 `127.0.0.1`。要从其他机器访问 Kibana/Grafana/EMQX Dashboard，需把 `DC3_BIND_HOST` 设为 `0.0.0.0`
  （或具体网卡 IP），并自行评估暴露面。

## 延伸阅读

- [部署模式与镜像源](./usage) — 四个 compose 栈（db/dev/app/optional）与 `make` 启动方式的全貌
- [日志规范](./logging) — 服务日志的消息风格与字段约定，决定你在 Kibana 里能按什么检索
- [故障排查](./troubleshooting) — 起不来、连不上、端口冲突时的排查路径
