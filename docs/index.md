---
layout: home

hero:
  name: IoT DC3
  text: 分布式工业物联网平台
  tagline: Connect devices, organize data, and bring AI into industrial IoT operations.
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
    details: 启动本地依赖、加载环境变量、构建并运行核心服务。
    link: /quickstart/
    linkText: 立即上手
  - icon: 🧭
    title: 操作手册
    details: 按概念、设备接入、数据命令和 AI 辅助运营组织使用路径。
    link: /operation/
    linkText: 查看流程
  - icon: 📖
    title: 使用指南
    details: 镜像选择、Compose 编排、日志规范和故障排查。
    link: /guide/
    linkText: 查看指南
  - icon: 🏗️
    title: 架构总览
    details: Gateway、Auth、Manager、Data、Agentic 和 Driver 的运行时关系。
    link: /architecture/
    linkText: 了解架构
  - icon: 🛠️
    title: 二次开发
    details: 基于 Driver SDK 扩展协议驱动，查看测试、API 文档和变更记录。
    link: /development/
    linkText: 开始开发
  - icon: 🧩
    title: 模块清单
    details: 网关、5 个中心服务、28 个接入驱动模块、API 合约和公共组件。
    link: /modules/
    linkText: 浏览模块
  - icon: 🗂️
    title: Superpowers
    details: 工程设计、历史分析、待办池和旧资料归档，供维护者继续演进项目。
    link: /superpowers/
    linkText: 查看资料
  - icon: 🤝
    title: 加入社区
    details: 贡献指南、行为准则和安全披露策略。
    link: /community/contributing
    linkText: 参与贡献
---

## 平台简介

IoT DC3 是一个面向工业物联网场景的开源分布式平台，用于连接设备、采集数据、管理元数据、分发命令，并把 AI 辅助能力接入 IoT 运营流程。

项目适合需要接入多类工业协议、管理设备与点位、查询实时/历史数据，并通过 Spring 生态进行二次开发的团队。

## 架构分层

| 层                         | 职责                      | 主要模块                                                            |
|---------------------------|-------------------------|-----------------------------------------------------------------|
| **驱动层** Driver Layer      | 设备接入、协议适配、南向数据采集与命令执行   | `dc3-driver-*`(Modbus / OPC / MQTT / S7 / Virtual)              |
| **数据层** Data Layer        | 实时与历史数据采集、存储、查询         | `dc3-center-data`                                               |
| **管理层** Management Layer  | 服务注册、设备/驱动管理、配置治理、命令编排  | `dc3-center-manager` / `dc3-center-auth` / `dc3-center-agentic` |
| **应用层** Application Layer | API 网关、对外开放、第三方集成、AI 增强 | `dc3-gateway` / `dc3-web`                                       |

## 技术栈

- [Java 21](https://www.java.com)
- [Spring Boot 4.0.6](https://spring.io/projects/spring-boot)
- [Spring Cloud 2025.1.1](https://spring.io/projects/spring-cloud)
- [Spring AI 2.0.0-M8](https://spring.io/projects/spring-ai)
- PostgreSQL、RabbitMQ、gRPC、Protobuf、Caffeine、Quartz

## 开源协议

`IoT DC3` 开源平台基于 [AGPL 3.0 License](https://github.com/pnoker/iot-dc3/blob/release/LICENSE-AGPL.txt)
协议。仓库许可证说明和商业授权关系请参阅 [LICENSE.txt](https://github.com/pnoker/iot-dc3/blob/release/LICENSE.txt)。
