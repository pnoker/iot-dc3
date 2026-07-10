---
layout: home

hero:
  name: IoT DC3
  text: 面向 AI 场景演进的分布式工业物联网平台
  tagline: 覆盖设备接入、数据采集、运营管理与智能分析。28 个多协议驱动接入异构设备，通过 Spring AI 让大模型读写设备数据、下发命令，形成"感知—决策—执行—反馈"闭环。分布式、多租户、全开源。
  image:
    src: /images/logo.svg
    alt: IoT DC3
  actions:
    - theme: brand
      text: 跑通第一个设备
      link: ./quickstart/first-device
    - theme: alt
      text: 从源码运行
      link: ./quickstart/

features:
  - icon: 🧭
    title: 总览
    details: 平台定位、核心概念与按角色的学习路径——先懂它是什么、给谁用、怎么入门。
    link: ./introduction/
    linkText: 了解定位
  - icon: 🏗️
    title: 架构
    details: 服务拓扑、数据平面与命令平面、鉴权租户、领域模型与模块地图——配时序图与状态机。
    link: ./architecture/
    linkText: 读懂架构
  - icon: 🔌
    title: 驱动
    details: 28 个多协议驱动接入异构设备，含设备接入流程与驱动能力矩阵。
    link: ./drivers/
    linkText: 浏览驱动
  - icon: 📚
    title: 基础
    details: 物联网四层技术体系——感知、网络、平台、应用与安全，每一层都接回 DC3 怎么实现。
    link: ./foundations/
    linkText: 读懂体系
  - icon: 🛠️
    title: 开发
    details: 基于 Driver SDK 派生新驱动，API 文档与测试，dc3 CLI 与 AI Agent / MCP 集成。
    link: ./development/
    linkText: 开始开发
  - icon: 🧰
    title: 运维
    details: 采集与读写命令、告警通知、部署模式与镜像源、可观测性、日志与排障。
    link: ./operation/
    linkText: 运营运维
---

## IoT DC3 是什么

IoT DC3 是一个开源、面向 AI 场景演进的分布式物联网平台（基于 AGPL-3.0），覆盖**设备接入、数据采集、运营管理与智能分析**，帮助构建工业
IoT 解决方案。它内置 **28 个接入驱动模块**，把异构设备的数据采上来、归一为带语义的位号值；再通过 **Spring AI**
把大语言模型接入运营流程——模型不仅能查询设备、读写位号、执行命令，还能做告警分析与数据洞察，把"感知—决策—执行—反馈"打通成闭环。

它适合需要接入多类工业协议、管理设备与位号、查询实时/历史数据，并希望在 Spring 生态里做二次开发、甚至引入 AI
辅助运营的团队。想先理解它解决什么问题、与同类平台的差异，请看 [平台定位](./introduction/)。

## 架构一览

平台由一个网关、四个中心服务和一组协议驱动组成，对外只暴露网关的 HTTP 入口；中心服务之间通过 gRPC 协作，驱动与数据中心之间通过
RabbitMQ 异步解耦。

<TopologyDiagram lang="zh" />

每一跳如何流转、为什么这样设计，见 [系统架构](./architecture/)。

## 技术栈

- **语言与框架
  **：[Java 21](https://www.java.com) · [Spring Boot 4](https://spring.io/projects/spring-boot) · [Spring Cloud 2025](https://spring.io/projects/spring-cloud) · [Spring AI 2.0.0](https://spring.io/projects/spring-ai)
- **数据、缓存与调度**：PostgreSQL（+ TimescaleDB / AGE / pgvector）· Caffeine · MyBatis-Plus · Quartz
- **消息与通信**：RabbitMQ · gRPC · MQTT（Paho + EMQX）· Protobuf
- **安全与认证**：Spring Security · JWT · BouncyCastle
- **前端**：Vue 3 · TypeScript 6 · Vite 8 · Element Plus · AntV G2/G6（源码在本仓库 `dc3-web/` 目录，原独立仓库
  `iot-dc3-web` 已归档）

完整说明见 [技术栈](./introduction/technology-stack)。

## 开源协议

IoT DC3 基于 [AGPL-3.0 License](https://github.com/pnoker/iot-dc3/blob/release/LICENSE-AGPL.txt)
发布。仓库许可证说明与商业授权关系请参阅 [LICENSE.txt](https://github.com/pnoker/iot-dc3/blob/release/LICENSE.txt)。
