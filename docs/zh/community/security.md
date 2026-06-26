---
title: 安全策略
---

# 安全策略

IoT DC3 是连接现场设备的工业物联网平台，一处安全缺口可能同时影响数据与控制。这页写给两类人：想知道**哪些版本仍在维护、发现漏洞该怎么私下上报
**的使用者，以及准备把平台投入生产、需要一份**最小安全基线清单**的运维与部署人员。

> 你在这里：已了解平台并准备落地。生产前请同时过一遍 [环境变量详解](../quickstart/environment)
> 与 [部署模式与镜像源](../guide/usage)。

## 受支持的版本

我们只为当前活跃维护的主线版本提供安全补丁与更新。版本号采用 `YYYY.M.x` 的年月方案（如 `2026.5.x` 表示 2026 年 5
月线），同一主线内的补丁号 `x` 持续向前滚动。当前发行线为 `2026.5.x`（最新 `2026.5.22`，镜像 tag `2026.6`）。

下表列出当前接受安全更新的版本线；不在表内的旧版本不再回补，请升级到受支持的主线后再上报。

| 版本线            | 是否受支持  |
|----------------|--------|
| `2026.5.x`     | ✅ 受支持  |
| `2026.4.x`     | ✅ 受支持  |
| `2025.x.x` 及更早 | ❌ 不再维护 |

::: tip 升级优先
报告漏洞前，先确认问题在受支持版本上仍能复现。许多安全问题已在新主线修复，升级往往是最快的处置路径。
:::

## 漏洞披露流程

我们对安全问题非常重视：一旦漏洞被确认，会尽快修复，并在发布说明（release notes）中披露修复信息。

::: danger 请勿公开披露
**不要**在 GitHub / Gitee 的 Issues 或讨论区公开潜在漏洞。公开的 PoC 会让尚未修复的实例直接暴露在攻击面下。请走下面的私有渠道。
:::

发现潜在安全漏洞时，请通过以下任一私有渠道上报：

1. **邮件上报**：向项目维护团队发送邮件，并在邮件主题中包含关键词 `Security Vulnerability`，便于优先识别与分流。
2. **私信上报**：通过 Gitee 或 GitHub 的私信功能直接联系项目维护者。

为便于复现与定位，建议在上报中包含：受影响的版本线、复现步骤或最小复现用例、影响面（数据泄露 / 越权 /
命令注入等）、以及你认为合理的修复建议。漏洞验证后我们会着手修复，并在对应版本的发布说明中同步修复信息。

## 生产安全基线

平台的默认配置面向本地开发，**怎么开发方便就怎么来**
：弱口令、明文端口、开发用密钥一应俱全。投产前必须逐项收紧。下面三条是最关键的硬约束，其余配置项见 [环境变量详解](../quickstart/environment)。

### 一、密钥必须随机，且 pre/pro 会强制校验

平台有两把对外不可泄露的密钥，默认值仅供开发：

- `AUTH_HMAC_SECRET` — 网关（Gateway）向后端服务签名 `X-Auth-Principal` 的 HMAC-SHA256 密钥，默认 `io.github.pnoker.dc3`。
- `DC3_SECURITY_KEY` — 鉴权中心（Auth Center / `dc3-center-auth`）生成与校验登录 Token 的签名密钥，默认
  `dc3.security.key.2026.io.github.pnoker`。

::: danger HMAC 密钥在 pre/pro 环境 fail-fast
当激活的 Spring profile（或 `spring.env` 属性）命中 `pre` 或 `pro` 时，`AUTH_HMAC_SECRET` 若为空、或仍等于默认值
`io.github.pnoker.dc3`，启动阶段会直接抛出 `IllegalStateException` 让服务**起不来**
。这是有意为之——宁可启动失败，也不让生产实例带着开发密钥对外服务。生产请用强随机值，例如 `openssl rand -base64 48`
生成，并通过环境变量注入，切勿硬编码或写入日志。
:::

```bash
# 为两把密钥各生成一段强随机值（示例输出，请勿照抄）
openssl rand -base64 48   # → 用作 AUTH_HMAC_SECRET
openssl rand -base64 48   # → 用作 DC3_SECURITY_KEY
```

`DC3_SECURITY_KEY` 不像 HMAC 那样有启动期 fail-fast，但同样必须改成强随机值——它一旦泄露，攻击者可伪造登录 Token。

### 二、启用 TLS，不要让消息总线与 Broker 明文跑

平台依赖 RabbitMQ 与（可选的）EMQX MQTT Broker。两者默认均**关闭** TLS，仅适合本地。投向生产或跨网络部署时启用加密：

- RabbitMQ：置 `RABBITMQ_SSL_ENABLED=true`，连接走 TLS 端口（`5671`，对外发布端口 `DC3_RABBITMQ_TLS_PORT`，默认 `35671`
  ），并视情况开启 `RABBITMQ_SSL_VALIDATE_SERVER_CERTIFICATE` 与 `RABBITMQ_SSL_VERIFY_HOSTNAME`（默认均为 `false`）。
- EMQX：使用 MQTT-over-TLS 端口（`DC3_EMQX_MQTTS_PORT`，默认 `38883`）与安全 WebSocket（`DC3_EMQX_WSS_PORT`，默认 `38084`
  ），而非明文的 `31883` / `38083`。

对外的 HTTP 入口（网关 `dc3-gateway`，默认 `8000`）应置于反向代理 / 负载均衡之后，由其终结 HTTPS。所有外部接口启用 HTTPS /
SSL，并对外部调用做访问审计。

### 三、最小暴露端口，别把现场协议端口暴露到公网

`DC3_BIND_HOST` 默认 `127.0.0.1`，即所有发布端口只绑定到本机；只有显式改为 `0.0.0.0` 才会对网络开放。生产请只对外暴露**必须
**对外的端口，其余一律收在内网或安全组之后。

::: danger 现场协议端口禁止直连公网
Modbus、TCP/UDP、各类 PLC 网关等现场协议端口（如监听型驱动 `dc3-driver-listening-virtual` 的
`DC3_LISTENING_VIRTUAL_TCP_PORT=6270` / `UDP=6271`）多无内建认证，**绝不可**直接暴露到公网。设备侧通过
VPN、专网或网关白名单接入；唯一应对公网开放的业务入口是经反代加固的网关 HTTP 端口。
:::

理想的暴露面只有一个：网关。鉴权中心（`8300`）、管理中心（`8400`）、数据中心（`8500`）、Agentic 中心（`8600`）以及各 gRPC 端口（
`9300/9400/9500`）都属内部链路，不对外发布。端口清单与默认值见 [环境变量详解](../quickstart/environment)
的「网关与服务端口」与「gRPC / facade」小节。

### 其余通用实践

- ✅ 始终运行受支持的版本，定期更新系统依赖与容器镜像。
- 🔑 改掉所有默认口令：PostgreSQL（`POSTGRES_PASSWORD` 默认 `dc3dc3dc3`）、RabbitMQ（`RABBITMQ_PASSWORD`）、MQTT（`MQTT_PASSWORD`
  ）等默认凭据全部替换为强随机值。
- 🧩 只授权受信任的设备与用户接入；对外部接口遵循最小权限原则并做访问审计。租户隔离与 RBAC
  的实现见 [鉴权·租户·RBAC](../architecture/auth-rbac)。

## 延伸阅读

- [环境变量详解](../quickstart/environment) — 全部安全相关变量的默认值、作用域与生产取值指引
- [部署模式与镜像源](../guide/usage) — 容器化部署、端口发布与镜像源选择
- [鉴权·租户·RBAC](../architecture/auth-rbac) — 登录、租户隔离与权限模型如何保障多租户安全
