---
layout: home

hero:
  name: IoT DC3
  text: 面向 AI 场景演进的分布式工业物联网平台
  tagline: 覆盖设备接入、数据采集、运营管理与智能分析。28 个多协议驱动接入异构设备，通过 Spring AI 让大模型读写设备数据、下发命令，形成"感知—决策—执行—反馈"闭环。分布式、多租户、全开源。
  image:
    src: /images/hero-logo.svg
    alt: IoT DC3
  actions:
    - theme: brand
      text: 快速开始
      link: ./quickstart/
    - theme: alt
      text: 跑通第一个设备
      link: ./quickstart/first-device
    - theme: alt
      text: 在 GitHub 查看
      link: https://github.com/pnoker/iot-dc3

features:
  - icon: 🧭
    title: 平台介绍
    details: 它是什么、给谁用、解决什么问题，以及与同类平台的真正差异。
    link: ./introduction/
    linkText: 了解定位
  - icon: 🚀
    title: 快速开始
    details: 本地起依赖、装环境、从虚拟驱动到看见第一条实时数据，全程可复制命令。
    link: ./quickstart/
    linkText: 立即上手
  - icon: 🏗️
    title: 系统架构
    details: 服务拓扑、数据平面与命令平面、鉴权租户、领域模型——配时序图与状态机。
    link: ./architecture/
    linkText: 读懂架构
  - icon: 🤖
    title: AI
    details: Agentic 中心做自然语言运营，MCP 把工具安全接给外部 AI Agent。
    link: ./ai/
    linkText: 接入 AI
  - icon: ⚡
    title: 自动化
    details: dc3 CLI 命令行脚本化操作——本地调试、CI 流水线、运维脚本。
    link: ./automation/
    linkText: 脚本化操作
  - icon: 🧰
    title: 操作指南
    details: 接入设备、采集与读写命令、告警与通知——日常运营的操作流程。
    link: ./operation/
    linkText: 查看流程
  - icon: 🛠️
    title: 二次开发
    details: 基于 Driver SDK 从模板派生新协议驱动，含 API 文档、测试与编码规范。
    link: ./development/
    linkText: 开始开发
  - icon: 📦
    title: 部署运维
    details: 部署模式与镜像源、可选可观测性栈（EMQX/ELK/Prometheus/Grafana）、日志与排障。
    link: ./guide/
    linkText: 部署上线
  - icon: 🤝
    title: 参与社区
    details: 贡献指南、行为准则与安全披露策略，欢迎提交驱动、修复与文档改进。
    link: ./community/contributing
    linkText: 参与贡献
---

## IoT DC3 是什么

IoT DC3 是一个开源、面向 AI 场景演进的分布式物联网平台（基于 AGPL-3.0），覆盖**设备接入、数据采集、运营管理与智能分析**，帮助构建工业 IoT 解决方案。它内置 **28 个接入驱动模块**，把异构设备的数据采上来、归一为带语义的位号值；再通过 **Spring AI** 把大语言模型接入运营流程——模型不仅能查询设备、读写位号、执行命令，还能做告警分析与数据洞察，把"感知—决策—执行—反馈"打通成闭环。

它适合需要接入多类工业协议、管理设备与位号、查询实时/历史数据，并希望在 Spring 生态里做二次开发、甚至引入 AI 辅助运营的团队。想先理解它解决什么问题、与同类平台的差异，请看 [平台定位](./introduction/)。

## 架构一览

平台由一个网关、四个中心服务和一组协议驱动组成，对外只暴露网关的 HTTP 入口；中心服务之间通过 gRPC 协作，驱动与数据中心之间通过 RabbitMQ 异步解耦。

<Architecture lang="zh" />

每一跳如何流转、为什么这样设计，见 [系统架构](./architecture/)。

## 技术栈

- **语言与框架**：[Java 21](https://www.java.com) · [Spring Boot 4](https://spring.io/projects/spring-boot) · [Spring Cloud 2025](https://spring.io/projects/spring-cloud) · [Spring AI 2.0.0](https://spring.io/projects/spring-ai)
- **数据、缓存与调度**：PostgreSQL（+ TimescaleDB / AGE / pgvector）· Caffeine · MyBatis-Plus · Quartz
- **消息与通信**：RabbitMQ · gRPC · MQTT（Paho + EMQX）· Protobuf
- **安全与认证**：Spring Security · JWT · BouncyCastle
- **前端**：Vue 3 · TypeScript 6 · Vite 8 · Element Plus · AntV G2/G6（源码在独立 [`iot-dc3-web`](https://github.com/pnoker/iot-dc3-web) 仓库）

## 开源协议

IoT DC3 基于 [AGPL-3.0 License](https://github.com/pnoker/iot-dc3/blob/release/LICENSE-AGPL.txt) 发布。仓库许可证说明与商业授权关系请参阅 [LICENSE.txt](https://github.com/pnoker/iot-dc3/blob/release/LICENSE.txt)。
