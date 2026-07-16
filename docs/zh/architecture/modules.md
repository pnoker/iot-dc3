---
title: 模块地图
---

<script setup>
import ModulesFlowDiagram from '../../.vitepress/theme/components/ModulesFlowDiagram.vue'
import ModulesClassDiagram from '../../.vitepress/theme/components/ModulesClassDiagram.vue'
</script>


# 模块地图

IoT DC3 的代码按"部署单元 + 共享契约 + 协议驱动"三类组织。这页从架构视角讲清楚：哪些模块会被打包成独立服务跑起来、它们靠哪些公共库与契约相互协作、28
个驱动如何按协议归类，以及驱动 SDK 暴露的 SPI 长什么样。读完你能定位任意一段功能落在哪个模块、它依赖谁。

> 你在这里：已读过 [系统架构总览](./) 与 [服务与拓扑](./services)
> ，想从模块/依赖角度看清边界。逐个模块的清单见 [模块清单](../modules/)。

## 三类模块，三种生命周期

不必把仓库里几十个 Maven 模块平铺记忆。它们只分三类，各有不同的存在理由：

- **部署单元**（`dc3-gateway`、`dc3-center-*`、`dc3-driver-*`）——会被打成可运行的 Spring Boot 进程、出现在 compose
  文件里、占一个端口。这是运维和拓扑关心的粒度。
- **公共与契约库**（`dc3-api-*`、`dc3-common-*`）——不单独运行，被部署单元依赖。它们承载"服务之间怎么说话"（gRPC 契约、facade
  接口）和"大家共用什么"（实体、枚举、DAL、消息配置）。
- **协议驱动**（`dc3-driver-*`）——一种特殊的部署单元：每个驱动是一个独立进程，但都站在同一个 SDK（`dc3-common-driver`
  ）之上，只填协议适配那一小块。

这三类的依赖方向是单向的：驱动与中心依赖公共库，公共库依赖契约库，契约层不反向依赖任何业务。下面先看这张依赖图，再逐类展开。

## 模块如何相互依赖

下图省略了基础设施（PostgreSQL / RabbitMQ）和逐个 common 子模块，只画"谁依赖谁"的骨架：网关与四个中心都建立在各自的
`dc3-common-*` 领域库上，跨服务调用统一走 facade 契约，驱动则通过 facade 把元数据请求打到管理中心、通过 RabbitMQ
与数据中心交换值和命令。

<ModulesFlowDiagram lang="zh" />

注意 facade 被画成一个被多方依赖的中间层——业务代码只编译期依赖 `dc3-common-facade-api` 里的接口，运行时由 `grpc` 或
`local` 实现注入，调用方对传输方式无感。这个"三态"是 IoT DC3
既能拆成分布式、又能合并成单体的关键，详见 [Facade 模式](./facade-modes)。

## 部署单元：网关、四中心与驱动

会被打包成进程跑起来的模块如下。端口与对外暴露策略以 compose 为准：只有网关的 HTTP `8000` 对外，其余中心的 HTTP/gRPC
端口都是集群内部端口。

| 部署单元                 | 角色                                     | HTTP   | gRPC   | 对外  |
|----------------------|----------------------------------------|--------|--------|-----|
| `dc3-gateway`        | 唯一对外 HTTP 入口、认证透传、MCP 资源服务器            | `8000` | —      | 是   |
| `dc3-center-auth`    | 认证 / 租户 / RBAC / OAuth 2.1             | `8300` | `9300` | 否   |
| `dc3-center-manager` | 驱动 / 模板 / 设备 / 位号等元数据管理                | `8400` | `9400` | 否   |
| `dc3-center-data`    | 位号值落库、命令分发与回执、告警                       | `8500` | `9500` | 否   |
| `dc3-center-agentic` | LLM 会话、工具调用、记忆                         | `8600` | —      | 否   |
| `dc3-center-single`  | auth + manager + data 合并的单体（本地 facade） | `8100` | `9100` | 视部署 |
| `dc3-driver-*`       | 协议适配（28 个独立进程）                         | 各自     | —      | 仅个别 |

`dc3-center-single` 把三个中心合进一个进程，用 `local` facade 在进程内直连，适合本地开发或资源受限的小规模部署；它和四中心分布式版本共享同一套
`dc3-common-*` 领域库，区别只在 facade 实现和打包方式。

::: info 智能中心没有 gRPC 端口
`dc3-center-agentic` 只暴露 HTTP（`8600`），不开 gRPC 服务端口——它作为 facade 的调用方去访问其他中心，自身不被其他中心通过
gRPC 反向调用。
:::

::: tip 驱动里只有少数对外暴露端口
绝大多数驱动是"主动出击"型：定时轮询设备、把值推到 RabbitMQ，不需要监听入站端口。例外是 `dc3-driver-listening-virtual`
这类反向接入驱动，它监听 TCP `6270` / UDP `6271`，让外部系统主动把数据推进来——这两个端口因此被映射到宿主机。
:::

## 公共与契约：服务之间怎么说话

部署单元之所以能各管一摊又协同工作，靠的是下面这层不单独运行的库。它们回答两个问题：**跨进程怎么传**（契约层）和**大家共用什么
**（公共层）。

**契约层 `dc3-api-*`** 是 protobuf / gRPC 的合约定义——`dc3-api-auth`、`dc3-api-data`、`dc3-api-driver`、`dc3-api-manager`
各自描述对应中心对外暴露的 RPC。改 proto 等于改服务间合约，需重新生成桩并跑契约测试。

**facade 三态** 是这页最该理解的一组模块，它把"调用哪个服务"和"用什么传输"解耦成三个职责清晰的模块：

| 模块                                            | 职责                            | 何时生效                          |
|-----------------------------------------------|-------------------------------|-------------------------------|
| `dc3-common-facade-api`                       | 定义跨服务调用的 Java 接口（业务代码只依赖它）    | 始终                            |
| `dc3-common-facade-grpc`                      | 接口的 gRPC 实现，底层走 `dc3-api-*` 桩 | `dc3.facade.mode=grpc`（分布式默认） |
| `dc3-common-facade-local-{auth,manager,data}` | 接口的进程内直连实现                    | `dc3.facade.mode=local`（单体）   |

控制器和 service 永远只 `@Autowired` `dc3-common-facade-api` 里的接口，永不直接绑定 gRPC
桩或某个具体服务——这正是 [Facade 模式](./facade-modes) 能在不改业务代码的前提下切换部署拓扑的原因。

**公共层 `dc3-common-*`** 是跨服务复用的基础设施与领域库，按职责分四组：

- 基础：`dc3-common-constant`（枚举、常量，如 `PointCommandTypeEnum`）、`dc3-common-model`（BO / VO / DTO，如
  `PointCommandDTO`）、`dc3-common-exception`、`dc3-common-public`（`R<T>` 响应封装）、`dc3-common-web`、`dc3-common-log`、
  `dc3-common-thread`。
- 数据访问：`dc3-common-dal`（MyBatis-Plus 基础能力，数据访问与查询封装）、`dc3-common-postgres`（多 schema 数据源）、
  `dc3-common-repository`（仓储抽象与位号值领域对象，如 `PointValueBO`）、`dc3-common-sql`。
- 通信：`dc3-common-rabbitmq`（交换机 / 队列配置，如 `dc3.e.value`）、`dc3-common-mqtt`。
- 领域：`dc3-common-{auth,manager,data,driver,gateway,agentic}`，每个对应一个部署单元的业务逻辑。例如 `dc3-center-manager`
  这个进程几乎只是 `dc3-common-manager` 的运行外壳。

::: info 运行时缓存用 Caffeine，不是 Redis
最新值缓存、Token 拒绝名单、权限缓存等都用进程内 Caffeine（如 `PointValueLocalCache`），不依赖独立的 Redis 基础设施。
:::

## 驱动按协议归类

28 个驱动是平台"协议广度"的载体。把它们按协议家族分组，比平铺一长串更容易找到你需要的那个。每个驱动是一个
`dc3-driver-<protocol>` 模块，都继承同一个 SDK，差异只在协议适配实现。

| 类别           | 代表驱动                                                                               | 说明                                                                                |
|--------------|------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| 工业现场总线 / PLC | `dc3-driver-modbus-tcp`、`dc3-driver-opc-ua`、`dc3-driver-plcs7`、`dc3-driver-iec104` | 工厂与电力 SCADA 现场最常见的一组；另含 modbus-rtu、opc-da、ethernet-ip、fins、melsec、bacnet-ip、sl651 |
| IoT 无线       | `dc3-driver-mqtt`、`dc3-driver-coap`、`dc3-driver-lwm2m`、`dc3-driver-http`           | 轻量级 / 受限设备；另含 ble、zigbee                                                          |
| 基础通信         | `dc3-driver-tcp-udp`、`dc3-driver-serial`、`dc3-driver-snmp`、`dc3-driver-can`        | 裸 socket、串口、网络管理与车载总线                                                             |
| 数据库桥接        | `dc3-driver-mysql`、`dc3-driver-postgresql`                                         | 把外部数据库当数据源接入；另含 oracle、sqlserver                                                  |
| 计量           | `dc3-driver-dlms`                                                                  | DLMS/COSEM 智能电表                                                                   |
| 仿真           | `dc3-driver-virtual`、`dc3-driver-listening-virtual`                                | 见下方说明                                                                             |

仿真这一类有两个角色完全不同的成员，别混淆：

- **`dc3-driver-virtual`** 是**驱动开发模板**。新写一个协议驱动就从它复制改名，它演示了 SDK
  的完整用法（注册、调度、读写、健康）。[快速开始](../quickstart/) 跑通"第一个设备"用的就是它产生的合成值。
- **`dc3-driver-listening-virtual`** 是**反向监听接入**。它不主动轮询，而是监听 TCP/UDP
  端口，等外部系统把数据推进来——适合那些"设备/系统主动上报"而非"平台主动采集"的场景。

::: danger `dc3.driver.code` 是稳定注册标识，不可随意改
每个驱动启动时用 `dc3.driver.code` 向管理中心注册；值/命令的 RabbitMQ routing key 则由驱动服务名 `dc3.driver.service`（
`driverProperties.getService()`）拼接派生——code 与 service 是 `DriverProperties` 上两个独立字段。改 code 等于换了注册身份、改
service 等于换了路由身份，都会让在途消息或注册信息找不到归属——除非配套迁移，否则不要改动已上线驱动的这两个值。
:::

## Driver SDK 的 SPI：一个聚合接口，七项契约

所有驱动共享的 SDK 在 `dc3-common-driver`。它面向驱动作者暴露的扩展点是 `DriverCustomService`——这个接口**自身不声明任何方法
**，只是把驱动通常要实现的 7 个能力接口聚合在一起。SDK 在需要"全部驱动钩子的并集"时注入它；而新驱动若只用到子集，也可以单独实现其中较小的几个接口。

<ModulesClassDiagram lang="zh" />

七项契约各管一段驱动生命：`DriverLifecycle` 管启动初始化与调度注册；`DriverMetadataListener.event(...)` 收元数据变更刷新本地缓存；
`DriverHealth` 与 `DeviceHealth` 分别上报驱动级和设备级健康；`DriverProtocol` 是核心读写——`read(...)` 返回
`ReadPointValue`、`write(...)` 返回 `Boolean`；`DriverCommand` 处理自定义命令；`DriverValidator` 负责校验，其
`simulate(...)` 是一个**确定性**合成值生成器（输出稳定，区别于 virtual 驱动 `read()` 内用 `ThreadLocalRandom` 现场生成的随机值）。

::: danger 写命令失败不回显值
`DriverProtocol.write(...)` 只有返回 `Boolean.TRUE` 才算成功；失败时命令结果的 `responseValue` 为 `null`，**不回显任何写入值
**——这是故意设计，防止把失败误判为成功。
:::

::: tip 健康状态 TTL 必须大于采集周期
驱动上报健康状态带 TTL，这个 TTL 必须大于读调度周期（例如 30s cron 配 TTL ≥ 25s），否则设备会在两次心跳间被判离线而反复抖动。
:::

SDK 还内置了注册（`DriverRegisterService`，指数退避重试）、调度（`DriverScheduleService`，Quartz 驱动）、发送（
`DriverSenderService`，含 `pointValueSender` / `deviceStatusSender`
等）等运行时服务，驱动作者一般无需触碰。完整开发流程见 [驱动开发](../development/driver-authoring)。

## 与模块清单页的分工

这页讲的是**架构与依赖**：模块分几类、谁依赖谁、facade 三态怎么解耦、驱动怎么归类、SDK 暴露什么。如果你要的是**逐个模块的用途速查
**（每个 `dc3-common-*` / `dc3-api-*` 子模块一行说明），去 [模块清单](../modules/)——那页是参考资料，本页是心智模型。

## 延伸阅读

- [模块清单](../modules/) — 逐个子模块的用途速查表
- [服务与拓扑](./services) — 部署单元的端口、启动顺序与健康检查
- [Facade 模式](./facade-modes) — `grpc` 与 `local` 两态如何切换部署拓扑
- [驱动开发](../development/driver-authoring) — 从 virtual 模板派生新协议驱动的完整流程
