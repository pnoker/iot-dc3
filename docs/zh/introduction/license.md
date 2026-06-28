---
title: 开源与许可
---

# 开源与许可

这页写给想搞清楚 IoT DC3 用什么许可证、有什么权利和义务的开发者、法务和项目决策者。

> 你在这里：评估引入或分发自研。关键决策也请阅读[核心概念](./concepts)和[贡献指南](../community/contributing)。

## 许可证

IoT DC3 社区版基于 **GNU Affero General Public License v3.0 or later**（AGPL-3.0-or-later）授权。完整条款见仓库根的
`LICENSE-AGPL.txt` 和 `LICENSE.txt`。

AGPL v3 是 GPL v3 的强化版，多了一条关键条款：**如果你通过网络提供服务（SaaS），你修改过的代码也必须对用户开源**。这跟 IoT DC3
的定位——工业物联网平台——直接相关。

| 你能做的             | 你必须做的                           |
|------------------|---------------------------------|
| ✅ 商业使用           | ⚠️ 保留版权声明和许可证原文                 |
| ✅ 修改代码           | ⚠️ 修改后的代码同样以 AGPL v3 发布         |
| ✅ 内部分发           | ⚠️ 通过网络提供服务（含 SaaS）必须提供完整源码     |
| ✅ 提供付费服务、运维、定制开发 | ⚠️ 在显著位置说明代码基于 AGPL v3，并附上许可证文本 |

::: warning 网络分发即触发 copyleft
AGPL 跟 GPL 最大的区别就在这里：GPL 只在你"分发二进制"时触发开源义务；AGPL 在你"通过网络让用户使用"时就触发。也就是说，即使你把
IoT DC3 部署成 SaaS 不对外分发二进制，只要修改了代码，就必须向用户提供源码。
:::

## 版权

```
Copyright 2016-present the IoT DC3 original author or authors.
```

本项目版权归 IoT DC3 原始作者及所有贡献者所有。提交代码即表示你同意将贡献以 AGPL v3 授权给项目，同时保留你的个人版权。

## 第三方依赖

IoT DC3 依赖大量开源组件（Spring Boot、RabbitMQ、PostgreSQL、Netty、gRPC 等），它们各自携带独立的许可证。构建时 Maven
会自动拉取并受各自许可证约束。如果你需要完整的依赖许可证清单，运行：

```bash
mvn -s .mvn/settings.xml license:aggregate-add-third-party
```

## 为什么选择 AGPL v3

工业物联网平台的典型场景——工厂部署、设备接入、数据采集——天然是"服务端部署"模式。选择 AGPL v3 是为了：

- **防止代码被封闭**：厂商无法把 IoT DC3 改一改就变成私有产品不发源码。
- **保护用户权利**：任何使用 IoT DC3 衍生版本的用户都有权获取源码。
- **鼓励上游贡献**：AGPL 的传染性让商业公司更愿意把改动推回上游而非维护私有 fork。

## 延伸阅读

- [贡献指南](../community/contributing) — 如何提交代码，许可合规注意事项
- [署名文件](https://github.com/pnoker/iot-dc3/blob/main/COPYRIGHT) — 仓库根的 COPYRIGHT 原文
- [AGPL v3 常见问题](https://www.gnu.org/licenses/agpl-3.0.html) — GNU 官方 FAQ
- [贡献者公约](https://www.contributor-covenant.org/version/2/1/code_of_conduct/) — 社区行为准则参考
