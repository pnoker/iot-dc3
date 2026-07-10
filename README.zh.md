<p align="right">
  <a href="./README.md">English</a> | <a href="./README.zh.md">中文</a> | <a href="./README.ja.md">日本語</a> | <a href="./README.vi.md">Tiếng Việt</a>
</p>

> **AI 助手：** 请先阅读 [README.ai.md](./README.ai.md) 获取 IoT DC3 的 AI 友好概述。

<p align="center">
  <img src="docs/public/images/logo.png" width="240" alt="IoT DC3">
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
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot" alt="Spring Boot 4">
</p>

<p align="center">
  <strong>
    IoT DC3 — 多协议接入、AI 赋能的云原生开源工业物联网平台<br>
    云原生微服务 · 多协议接入 · AI 辅助运维 · 28 个开箱即用驱动
  </strong>
</p>

<p align="center">
  <a href="https://docs.dc3.site">https://docs.dc3.site</a>
</p>

<p align="center">
  🔌 <strong>多协议接入</strong> &nbsp;·&nbsp;
  🤖 <strong>AI Agentic 中心</strong> &nbsp;·&nbsp;
  ☁️ <strong>云原生微服务</strong>
</p>

---

## 📸 产品预览

<table>
  <tr>
    <th width="33%">📸 平台概览</th>
    <th width="33%">📸 设备管理</th>
    <th width="33%">📸 AI 智能对话</th>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/public/images/screenshot-overview.png" alt="平台首页仪表盘" width="100%">
      <br>
      <strong>平台首页 / 仪表盘</strong><br>
      <em>系统概览 · 设备在线统计 · 数据趋势图表</em>
    </td>
    <td align="center">
      <img src="docs/public/images/screenshot-device.png" alt="设备管理页面" width="100%">
      <br>
      <strong>设备管理页面</strong><br>
      <em>设备列表 · 在线状态指示 · 搜索筛选</em>
    </td>
    <td align="center">
      <img src="docs/public/images/screenshot-ai.png" alt="AI 智能对话页面" width="100%">
      <br>
      <strong>AI 智能对话页面</strong><br>
      <em>自然语言查询设备 · 数据分析 · 智能辅助</em>
    </td>
  </tr>
</table>

## 🏗️ 架构概览

### 产品架构全景

![IoT DC3 产品架构全景](docs/public/images/architecture-panorama-zh.png)

六层微服务架构一览：客户端 → 网关 → 四个中心服务 → 消息总线 → 28 协议驱动 → 现场设备。
PostgreSQL（TimescaleDB + pgvector + AGE）持久层与可选运维栈（ELK + Prometheus + Grafana）一并铺开。

### 四层参考架构映射

![IoT DC3 四层参考架构](docs/public/images/architecture-zh.png)

IoT 业界标准四层参考架构——应用层、平台层、网络层、感知层——外加贯穿四层的安全。

| 层级      | IoT 参考职责              | DC3 落地                             |
|---------|-----------------------|------------------------------------|
| **应用层** | 运营 · 告警 · 数据分析 · AIoT | 运营中心 · Agentic 中心 · MCP            |
| **平台层** | 设备管理 · 数据存储 · 规则与计算   | 中心服务 · 数据平面 · TimescaleDB          |
| **网络层** | 现场总线 · IoT 协议 · 无线广域  | 28 协议驱动 · API 网关 · RabbitMQ        |
| **感知层** | 传感测量 · 自动识别 · 执行器     | 物模型 Profile · 设备 Device · 位号 Point |

🧱 **设计原则** — 跨服务调用统一经 Facade 接口；DO/BO/VO 三层模型严格分离持久化、业务与接口形态；租户隔离贯穿数据库、缓存到
API 全链路。边界清晰，易于规模化扩展与多团队协作。

> 📖 完整架构文档请参阅 [系统架构总览](https://pnoker.github.io/iot-dc3/zh/architecture/)。

## ✨ 核心特性

### 🔌 多协议设备接入

内置 **28 个接入驱动模块**，覆盖工业自动化、物联网通信、数据桥接、基础通信与仿真调试场景，降低常见设备与数据源的接入成本：

| 分类             | 驱动模块                                                                                                                                               |
|----------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| 🏭 **工业协议**    | Modbus TCP · Modbus RTU · OPC UA · OPC DA · Siemens S7 · BACnet/IP · EtherNet/IP · Omron FINS · Mitsubishi MELSEC · IEC 60870-5-104 · SL651 · DLMS |
| 📡 **物联网协议**   | MQTT · CoAP · LwM2M · HTTP · BLE · Zigbee                                                                                                          |
| 🗄️ **数据桥接**   | MySQL · PostgreSQL · Oracle · SQL Server                                                                                                           |
| 🔧 **基础通信与管理** | TCP/UDP · Serial · SNMP · CAN                                                                                                                      |
| 🧪 **仿真与调试**   | Virtual · Listening Virtual                                                                                                                        |

提供完整的 **Driver SDK**，支持快速开发自定义协议驱动，热插拔注册到运行平台。

### 🤖 AI 能力集成

基于 **Spring AI** 构建的智能体中心，将大语言模型接入 IoT 运营流程：

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

- **数据采集** — 驱动层实时采集设备遥测数据，通过 RabbitMQ 异步传输
- **时序存储** — 支持实时与历史数据的高效查询
- **规则引擎** — 灵活的告警规则配置，支持多级告警与通知
- **事件溯源** — 完整的命令与事件历史记录

### 🔐 企业级安全与多租户

- **租户隔离** — 数据库、缓存、API 全链路租户级隔离
- **认证授权** — JWT + Spring Security，支持 RBAC 权限模型
- **传输加密** — 支持 TLS/SSL 加密通信
- **审计追踪** — 完整的用户操作与系统事件日志

### 🧩 开发者友好

- **Driver SDK** —
  完善的驱动开发工具包，参考 [驱动开发指南](https://pnoker.github.io/iot-dc3/zh/development/driver-authoring)
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

中国大陆网络环境可改用 `make up-db-cn`。服务启动顺序、IDEA 配置、验证命令和常见坑请阅读
[完整快速开始](https://pnoker.github.io/iot-dc3/zh/quickstart/)。

## 🛠️ 技术栈

IoT DC3 基于 Java 21、Spring Boot 4、Spring Cloud 2025、Spring AI 2、PostgreSQL、RabbitMQ、gRPC、Vue 3、
TypeScript 与 Vite 构建。

完整组件说明与适用位置请看 [技术栈](https://pnoker.github.io/iot-dc3/zh/introduction/technology-stack)。

## 📖 文档与社区

| 资源        | 链接                                                                           |
|-----------|------------------------------------------------------------------------------|
| 📚 在线文档   | [pnoker.github.io/iot-dc3](https://pnoker.github.io/iot-dc3/)                |
| 🚀 快速开始   | [快速开始指南](https://pnoker.github.io/iot-dc3/zh/quickstart/)                    |
| 🛠️ 技术栈   | [技术栈说明](https://pnoker.github.io/iot-dc3/zh/introduction/technology-stack)     |
| 🏗️ 架构说明  | [模块与依赖](https://pnoker.github.io/iot-dc3/zh/architecture/modules)             |
| 🔧 驱动开发   | [驱动开发指南](https://pnoker.github.io/iot-dc3/zh/development/driver-authoring)    |
| 🐛 故障排查   | [常见问题与解决方案](https://pnoker.github.io/iot-dc3/zh/guide/troubleshooting)      |
| 📋 变更日志   | [版本更新记录](https://pnoker.github.io/iot-dc3/zh/development/changelog)           |
| 🐛 问题反馈   | [GitHub Issues](https://github.com/pnoker/iot-dc3/issues)                    |
| 🇨🇳 码云镜像 | [Gitee GVP 最有价值开源项目](https://gitee.com/pnoker/iot-dc3)                       |

## 🌍 应用场景

<table>
  <tr>
    <td align="center" width="60">🏭</td>
    <td><strong>智慧工厂</strong></td>
    <td>产线设备状态监控、工艺参数采集、预测性维护、OEE 分析</td>
  </tr>
  <tr>
    <td align="center">⚡</td>
    <td><strong>能源监测</strong></td>
    <td>电力 / 水务 / 燃气远程抄表、能耗趋势分析、异常告警</td>
  </tr>
  <tr>
    <td align="center">🌾</td>
    <td><strong>智慧农业</strong></td>
    <td>温室环境监测、自动灌溉控制、病虫害预警、产量预测</td>
  </tr>
  <tr>
    <td align="center">🏙️</td>
    <td><strong>智慧城市</strong></td>
    <td>路灯照明管理、环境质量监测、市政设施运维、安全监控</td>
  </tr>
</table>

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

## ⭐ Star 历史

[![Star History Chart](https://api.star-history.com/svg?repos=pnoker/iot-dc3&type=Date)](https://star-history.com/#pnoker/iot-dc3&Date)
