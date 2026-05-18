---
layout: home

hero:
  name: IoT DC3
  text: 开源分布式物联网平台
  tagline: 基于 Spring Cloud 构建 · AI-Ready · 云原生交付 · 支持私有云、公有云与边缘部署
  image:
    src: /images/architecture-cn.png
    alt: IoT DC3 Architecture
  actions:
    - theme: brand
      text: 快速开始
      link: /quickstart/
    - theme: alt
      text: 在 GitHub 查看
      link: https://github.com/pnoker/iot-dc3

features:
  - icon: 🚀
    title: 快速开始
    details: 一键拉起本地开发环境，几分钟跑通核心服务。Make + Compose + Java 21,推荐工作流清单。
    link: /quickstart/
    linkText: 立即上手
  - icon: 📖
    title: 使用指南
    details: 镜像选择、Compose 编排、日志规范、故障排查的全流程指引。
    link: /guide/
    linkText: 查看指南
  - icon: 🏗️
    title: 架构总览
    details: Driver / Data / Management / Application 四层架构,gRPC 内部 facade,RabbitMQ 异步解耦,多租户隔离。
    link: /architecture/
    linkText: 了解架构
  - icon: 🛠️
    title: 二次开发
    details: 基于 dc3-driver-virtual 派生新协议驱动,测试规范,版本变更说明。
    link: /development/
    linkText: 开始开发
  - icon: 🧩
    title: 模块清单
    details: 网关、4 大中心服务、7 类协议驱动、API facade、20+ 通用组件,按职责分类。
    link: /modules/
    linkText: 浏览模块
  - icon: 🤝
    title: 加入社区
    details: 贡献指南、行为准则、安全披露策略。开源 AGPL-3.0,欢迎 PR 与议题反馈。
    link: /community/contributing
    linkText: 参与贡献
---

## 平台简介

IoT DC3 是一个基于 Spring Cloud 构建的完全开源、分布式物联网平台。它加速 IoT 解决方案交付,简化设备全生命周期管理,并以完整架构支撑稳定、可生产落地的 IoT 系统。平台具备 AI-Ready 能力,可无缝集成智能连接、自动化与数据驱动运营。所有组件和代码均为开源,保证透明性、灵活性与社区驱动的持续创新。

## 架构分层

| 层 | 职责 | 主要模块 |
|---|---|---|
| **驱动层** Driver Layer | 设备接入、协议适配、南向数据采集与命令执行 | `dc3-driver-*`(Modbus / OPC / MQTT / S7 / Virtual) |
| **数据层** Data Layer | 实时与历史数据采集、存储、查询 | `dc3-center-data` |
| **管理层** Management Layer | 服务注册、设备/驱动管理、配置治理、命令编排 | `dc3-center-manager` / `dc3-center-auth` / `dc3-center-agentic` |
| **应用层** Application Layer | API 网关、对外开放、第三方集成、AI 增强 | `dc3-gateway` / `dc3-web` |

## 技术栈

- [Java 21](https://www.java.com)
- [Spring Boot 3.5.5](https://spring.io/projects/spring-boot)
- [Spring Cloud 2025.0.0](https://spring.io/projects/spring-cloud)

## 开源协议

`IoT DC3` 开源平台基于 [AGPL 3.0 License](https://github.com/pnoker/iot-dc3/blob/release/LICENSE-AGPL.txt) 协议。仓库许可证说明和商业授权关系请参阅 [LICENSE.txt](https://github.com/pnoker/iot-dc3/blob/release/LICENSE.txt)。
