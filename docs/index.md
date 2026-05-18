---
hide:
  - navigation
  - toc
---

# IoT DC3

<p align="center">
  <img src="assets/images/logo-blue.zh.png" width="380" alt="IoT DC3 Logo">
</p>

<p align="center">
  <strong>基于 Spring Cloud 构建的完全开源、分布式物联网 (IoT) 平台</strong>
</p>

<p align="center">
  加速 IoT 解决方案交付 · 简化设备全生命周期管理 · AI-Ready 能力 · 云原生交付
</p>

---

![iot-dc3-architecture](assets/images/architecture-cn.png)

## 平台简介

IoT DC3 是一个基于 Spring Cloud 构建的完全开源、分布式物联网平台。它加速 IoT 解决方案交付，简化设备全生命周期管理，并以完整架构支撑稳定、可生产落地的 IoT 系统。平台具备 AI-Ready 能力，可无缝集成智能连接、自动化与数据驱动运营。所有组件和代码均为开源，保证透明性、灵活性与社区驱动的持续创新。

## 架构分层

- **驱动层 (Driver Layer)** — 提供 SDK，支持标准与私有协议的设备接入，负责南向数据采集与命令执行
- **数据层 (Data Layer)** — 提供可靠的设备数据采集、存储与查询能力，支撑实时与历史数据服务
- **管理层 (Management Layer)** — 分布式微服务协作核心，承担服务注册、设备/驱动管理、命令编排与配置治理
- **应用层 (Application Layer)** — 数据开放、任务调度、告警消息、日志管理、第三方集成及 AI 增强自动化场景

## 核心目标

- **可扩展性**：基于 Spring Cloud 支持横向扩展，满足分布式高吞吐 IoT 场景
- **韧性**：通过可替换服务节点与容错设计，降低单点故障风险
- **性能**：面向大规模设备接入与遥测数据处理需求
- **可扩展开发**：通过 SDK 与服务注册机制，快速集成新协议和自定义驱动
- **部署灵活性**：支持私有云、公有云与边缘部署
- **运营效率**：简化设备接入、注册与权限校验流程
- **安全与多租户**：支持传输加密、命名空间隔离与租户级隔离
- **云原生交付**：面向 Kubernetes 优化，并通过 Docker 容器化实现一致部署
- **AI-Ready 演进**：支持智能自动化与数据驱动运营能力集成

## 快速导航

<div class="grid cards" markdown>

- :material-rocket-launch: **[快速开始](quickstart/index.md)**

    一键拉起本地开发环境，几分钟跑通核心服务

- :material-book-open-variant: **[使用指南](guide/index.md)**

    镜像选择、日志规范、故障排查全流程指引

- :material-sitemap: **[架构](architecture/index.md)**

    Maven 模块依赖、运行时数据流、组件交互

- :material-tools: **[开发](development/index.md)**

    驱动开发、测试规范、版本变更说明

- :material-puzzle: **[模块](modules/index.md)**

    各 `dc3-*` 模块的职责与文档清单

- :material-account-group: **[社区](community/contributing.md)**

    贡献流程、行为准则、安全披露策略

</div>

## 技术栈

- [Java 21](https://www.java.com)
- [Spring Boot 3.5.5](https://spring.io/projects/spring-boot)
- [Spring Cloud 2025.0.0](https://spring.io/projects/spring-cloud)

## 开源协议

`IoT DC3` 开源平台基于 [AGPL 3.0 License](https://github.com/pnoker/iot-dc3/blob/release/LICENSE-AGPL.txt) 协议。仓库许可证说明和商业授权关系请参阅 [LICENSE.txt](https://github.com/pnoker/iot-dc3/blob/release/LICENSE.txt)。

---

> 本文档站基于 MkDocs Material 构建，源文件位于仓库 [`docs/`](https://github.com/pnoker/iot-dc3/tree/release/docs) 目录。欢迎通过页面右上角「编辑此页」按钮提交修订。
