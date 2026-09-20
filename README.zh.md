<p align="right">
  <a href="./README.md">English</a> | <a href="./README.zh.md">中文</a> | <a href="./README.ja.md">日本語</a> | <a href="./README.vi.md">Tiếng Việt</a> | <a href="./README.ko.md">한국어</a> | <a href="./README.es.md">Español</a> | <a href="./README.ru.md">Русский</a>
</p>

> **AI 助手：** 请先阅读 [README.ai.md](./README.ai.md) 获取 IoT DC3 的 AI 友好概述。

<p align="center">
  <img src="./.github/brand/png/banner.zh.png" alt="IoT DC3 — 连接物理世界与 AI，面向 Physical AI 的开源工业物联网 Runtime">
</p>

<p align="center">
  <a href="https://github.com/pnoker/iot-dc3/stargazers">
    <img src="https://img.shields.io/github/stars/pnoker/iot-dc3?style=flat&logo=github&color=green" alt="GitHub Stars">
  </a>
  <a href="https://gitee.com/pnoker/iot-dc3/stargazers">
    <img src="https://gitee.com/pnoker/iot-dc3/badge/star.svg?theme=gvp" alt="Gitee Star">
  </a>
  <a href="https://gitee.com/pnoker/iot-dc3/members">
    <img src="https://gitee.com/pnoker/iot-dc3/badge/fork.svg?theme=gvp" alt="Gitee Fork">
  </a>
  <a href="https://github.com/pnoker/iot-dc3/graphs/contributors">
    <img src="https://img.shields.io/github/contributors/pnoker/iot-dc3?label=contributors&color=orange" alt="Contributors">
  </a>
  <img src="https://img.shields.io/badge/License-AGPL%203.0-blue" alt="License">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot" alt="Spring Boot 4.1">
</p>

<p align="center">
  <strong>
    IoT DC3 — 连接物理世界与 AI<br>
    面向 Physical AI 的开源工业物联网 Runtime
  </strong>
</p>

<p align="center">
  <a href="https://dc3.site">https://dc3.site</a>
</p>

<p align="center">
  🔌 <strong>多协议接入</strong> &nbsp;·&nbsp;
  🤖 <strong>MCP 工具网关</strong> &nbsp;·&nbsp;
  ☁️ <strong>云原生微服务</strong>
</p>

<p align="center">
  <em>
    IoT DC3 将<strong>设备</strong>与<strong>应用</strong>解耦：驱动把标准化后的点位数据推入消息总线，应用通过统一 API
    消费数据——换设备无需改动应用，加应用无需触碰设备。
  </em>
</p>

---

## 📸 产品预览

<table>
  <tr>
    <th width="33%">📸 平台概览</th>
    <th width="33%">📸 设备管理</th>
    <th width="33%">📸 智能体</th>
  </tr>
  <tr>
    <td align="center">
      <img src="https://docs.dc3.site/images/screenshot-overview.png" alt="平台首页仪表盘" width="100%">
      <br>
      <strong>平台首页 / 仪表盘</strong><br>
      <em>系统概览 · 设备在线统计 · 数据趋势图表</em>
    </td>
    <td align="center">
      <img src="https://docs.dc3.site/images/screenshot-device.png" alt="设备管理页面" width="100%">
      <br>
      <strong>设备管理页面</strong><br>
      <em>设备列表 · 在线状态指示 · 搜索筛选</em>
    </td>
    <td align="center">
      <img src="https://docs.dc3.site/images/screenshot-ai.png" alt="智能体助手页面" width="100%">
      <br>
      <strong>智能体助手</strong><br>
      <em>自然语言查询设备 · 数据洞察 · 受控执行</em>
    </td>
  </tr>
</table>

## 🏗️ 架构概览

### 产品架构全景

![IoT DC3 产品架构全景](https://docs.dc3.site/images/architecture-panorama-zh.png)

六层微服务架构一览：客户端 → 网关 → 四个中心服务 → 消息总线 → 36 协议驱动 → 现场设备。PostgreSQL（TimescaleDB + pgvector +
AGE）持久层与可选运维栈（ELK + Prometheus + Grafana）一并铺开。

🧱 **设计原则** — 跨服务调用统一经 Facade 接口；DO/BO/VO 三层模型严格分离持久化、业务与接口形态；租户隔离贯穿数据库、缓存到
API 全链路。边界清晰，易于规模化扩展与多团队协作。

> 📖 完整架构文档请参阅 [系统架构总览](https://docs.dc3.site/zh/architecture/)。

## ✨ 核心特性

### 🔌 多协议设备接入

内置 **36 个接入驱动模块**，覆盖工业自动化、物联网通信、数据桥接、基础通信与仿真调试场景，降低常见设备与数据源的接入成本：

| 分类                        | 驱动模块                                                                                                                                                                                     |
|-----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 🏭 **工业协议**             | Modbus TCP · Modbus RTU · OPC UA · OPC DA · Siemens S7 · BACnet/IP · EtherNet/IP · Omron FINS · Mitsubishi MELSEC · IEC 60870-5-104 · IEC 61850 · DNP3 · DLMS · DLT645 · KNX · M-Bus · SL651 |
| 📡 **物联网协议**           | MQTT · CoAP · LwM2M · HTTP · BLE · Zigbee · LoRaWAN                                                                                                                                          |
| 🗄️ **数据桥接**             | MySQL · PostgreSQL · Oracle · SQL Server · Redis                                                                                                                                             |
| 🔧 **基础通信、消息与管理** | TCP/UDP · Serial · SNMP · CAN · Kafka                                                                                                                                                        |
| 🧪 **仿真与调试**           | Virtual · Listening Virtual                                                                                                                                                                  |

提供完整的 **Driver SDK**，支持快速开发自定义协议驱动，热插拔注册到运行平台。

### 🤖 从设备数据到 Physical AI

智能体中心基于 **Spring AI** 构建，平台通过 MCP 工具网关向 AI 智能体开放设备能力，让智能体在**安全、可控、可追溯**的闭环中作用于物理世界：

- **MCP 工具网关** — 外部 AI 智能体通过 Model Context Protocol 接入：OAuth 客户端注册、按工具授权、每次工具调用全程审计
- **自然语言辅助运维** — LLM 通过 Tool-Calling 机制，在权限受控下查询设备、读写数据点、辅助执行命令
- **智能告警分析** — AI 辅助分析告警原因，提供处置建议
- **数据洞察** — 自然语言查询设备数据，自动生成可视化图表
- **多模型支持** — 兼容 OpenAI API 标准，可接入 GPT、Claude、DeepSeek、通义千问等主流模型
- **对话记忆** — 支持多轮对话与上下文记忆，持久化到数据库

### 🏗️ 云原生微服务

基于 **Spring Boot 4 + Spring Cloud 2025** 构建的分布式微服务架构：

- **服务治理** — Spring Cloud Gateway 统一入口，静态路由 + 环境变量灵活配置
- **高效通信** — gRPC 服务间调用，Protobuf 序列化
- **横向扩展** — 无状态设计，支持按业务负载独立扩缩容
- **容错韧性** — 可替换服务节点，故障自动隔离

### 📊 实时数据引擎

- **数据采集** — 驱动层实时采集设备遥测数据，通过内部消息队列异步传输——可按部署插拔选择：RabbitMQ（默认）、Kafka、Pulsar 或任意 MQTT 5 broker（[消息队列选型指南](docs/mq-brokers.md)）
- **时序存储** — 支持实时与历史数据的高效查询
- **规则引擎** — 灵活的告警规则配置，支持多级告警与通知
- **事件溯源** — 完整的命令与事件历史记录

### 🔐 企业级安全与多租户

- **租户隔离** — 数据库、缓存、API 全链路租户级隔离
- **认证授权** — JWT + Spring Security，支持 RBAC 权限模型
- **传输加密** — 支持 TLS/SSL 加密通信
- **审计追踪** — 完整的用户操作与系统事件日志

### 🧩 开发者友好

- **Driver SDK** — 完善的驱动开发工具包，参考 [驱动开发指南](https://docs.dc3.site/zh/development/driver-authoring)
- **前后端分离** — Vue 3 + TypeScript 前端，RESTful + gRPC 双协议 API
- **容器化部署** — Podman / Docker Compose 一键启动，便于迁移到 Kubernetes 等容器平台
- **完整文档** — 在线文档站 + 快速开始指南 + 故障排查手册

## ⚡ 快速开始

源码本地开发时，先启动 PostgreSQL 与 RabbitMQ，再加载本地环境变量并构建：

```bash
make up-db
source dc3/env/dev.env.sh
mvn -s .mvn/settings.xml clean package
```

中国大陆网络环境可改用 `make up-db-cn`。

> 📖 服务启动顺序、IDEA 配置、验证命令和常见坑请参阅 [完整快速开始](https://docs.dc3.site/zh/quickstart/)。

## 🛠️ 技术栈

IoT DC3 基于 Java 21、Spring Boot 4、Spring Cloud 2025、Spring AI 2、PostgreSQL、可插拔消息队列（RabbitMQ、Kafka、Pulsar 或 MQTT 5——[选型指南](docs/mq-brokers.md)）、gRPC、Vue 3、TypeScript 与 Vite 构建。

完整组件说明与适用位置请看 [技术栈](https://docs.dc3.site/zh/development/technology-stack)。

## 📖 文档与社区

| 资源        | 链接                                                                  |
|-------------|-----------------------------------------------------------------------|
| 📚 在线文档 | [docs.dc3.site](https://docs.dc3.site/)                               |
| 🎬 在线演示 | [demo.dc3.site](https://demo.dc3.site/)                               |
| 🏭 行业演示 | [dc3.site/zh/demo](https://dc3.site/zh/demo/)                         |
| 🚀 快速开始 | [快速开始指南](https://docs.dc3.site/zh/quickstart/)                  |
| 🛠️ 技术栈   | [技术栈说明](https://docs.dc3.site/zh/development/technology-stack)   |
| 🏗️ 架构说明 | [模块与依赖](https://docs.dc3.site/zh/architecture/modules)           |
| 🔧 驱动开发 | [驱动开发指南](https://docs.dc3.site/zh/development/driver-authoring) |
| 🐛 故障排查 | [常见问题与解决方案](https://docs.dc3.site/zh/guide/troubleshooting)  |
| 📋 变更日志 | [版本更新记录](https://docs.dc3.site/zh/development/changelog)        |
| 💰 定价与授权 | [版本计划与商业授权](https://dc3.site/zh/pricing/)                  |
| 🐛 问题反馈 | [GitHub Issues](https://github.com/pnoker/iot-dc3/issues)             |
| 🇨🇳 码云镜像 | [Gitee GVP 最有价值开源项目](https://gitee.com/pnoker/iot-dc3)        |

## 🌍 应用场景

基于 IoT DC3 构建的 12 个行业演示看板（示例数据）展示平台在各场景的落地方式。[浏览全部演示](https://dc3.site/zh/demo/)。

| | | |
|---|---|---|
| 🏭 [智慧工厂](https://dc3.site/zh/demo/smart-factory/) — OEE 产线监控 | 💧 [智慧水务](https://dc3.site/zh/demo/water-network/) — 供水管网数字孪生 | ⚡ [能源微电网](https://dc3.site/zh/demo/microgrid/) — 光储协同 |
| 🌾 [精准农业](https://dc3.site/zh/demo/precision-agri/) — 大棚墒情微气候 | 🏢 [智慧楼宇](https://dc3.site/zh/demo/smart-building/) — 能耗暖通占用 | 🚦 [智慧交通](https://dc3.site/zh/demo/smart-traffic/) — 路网信号自适应 |
| 🛢️ [油气管网](https://dc3.site/zh/demo/oil-gas/) — 管线压力管存 | ⛏️ [智慧矿山](https://dc3.site/zh/demo/smart-mine/) — 瓦斯通风预警 | ❄️ [冷链物流](https://dc3.site/zh/demo/cold-chain/) — 温湿度轨迹追溯 |
| 🌿 [智慧环保](https://dc3.site/zh/demo/eco-monitor/) — 大气水质监测 | ⚓ [智慧港口](https://dc3.site/zh/demo/smart-port/) — 岸桥泊位调度 | 🔌 [新能源充电](https://dc3.site/zh/demo/ev-charging/) — 光储充协同 |

## 🤝 参与贡献

我们欢迎任何形式的贡献！请遵循以下流程：

1. **Fork & 分支** — 从 `main` 创建分支，命名格式：`feature/your_name/feature_description`
   （例：`feature/pnoker/mqtt_driver`）
2. **开发 & 提交** — 在新分支上完成修改并提交，遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范
3. **发起 PR** — 提交 Pull Request 到 `develop` 分支，维护者会审核并合并

## 📄 开源协议

IoT DC3 基于 [AGPL 3.0](./LICENSE-AGPL.txt) 协议开源。

- ✅ **个人学习、研究、内部使用** — 完全免费
- ✅ **修改代码并开源你的修改** — 欢迎
- ⚠️ **作为商业服务提供给第三方且未开源修改** — 需要商业授权

商业授权详情请参阅 [LICENSE.txt](./LICENSE.txt)。
