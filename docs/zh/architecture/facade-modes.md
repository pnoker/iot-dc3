---
title: Facade 模式：grpc 与 local
---

<script setup>
import FacadeModesDiagram from '../../.vitepress/theme/components/FacadeModesDiagram.vue'
</script>


# Facade 模式：grpc 与 local

中心服务之间相互调用（数据中心问管理中心要设备、智能中心问数据中心要位号值）有两种装配方式：`grpc`（各服务独立进程、跨进程调用）和
`local`（所有中心合一进程、进程内直调）。这页讲清 `dc3.facade.mode` 这个开关切的是**部署拓扑**而不是传输协议，以及业务代码为什么完全不用跟着改。

> 你在这里：已读过 [系统架构总览](./) 与 [服务与拓扑](./services)，想理解中心之间到底怎么互联。

## 它是部署拓扑开关，不是协议选择

容易误读的地方先说清：`dc3.facade.mode` 不是"用 gRPC 还是用 REST"的传输协议二选一，而是"中心服务**分几个进程跑**"的拓扑选择。

业务代码从不直接调对方的 gRPC stub 或 protobuf 类，而是依赖一组**协议中立的 `*Facade` 接口**——契约定义在
`dc3-common-facade-api`（如 `DeviceFacade`、`PointValueFacade`、`TenantFacade`、`PermissionFacade` 等 16 个接口）。每个接口都有两套实现，按
`dc3.facade.mode` 的值在启动时择一装配：

- `grpc` 模式装配 gRPC 实现（`dc3-common-facade-grpc`，如 `DeviceGrpcFacade`）——发起一次跨进程 gRPC 调用，去找独立运行的目标中心。
- `local` 模式装配进程内实现（`dc3-common-facade-local-*`，如 `DeviceLocalFacade`）——直接调用本进程里的目标 Service，*
  *没有网络开销**。

`DeviceFacade` 接口自己的 Javadoc 就把这件事说明白了：

> `DeviceLocalFacade` — in-process call into `DeviceService`, selected when `dc3.facade.mode=local` (single deployment).
> `DeviceGrpcFacade` — gRPC call against Manager Center, selected when `dc3.facade.mode=grpc` (distributed deployment,
> default).

两套实现接同一个接口、返回同样的 BO/Page 类型，所以**切换模式不改一行业务代码**——只换被注入的那个 Bean。

## 两种模式如何装配

装配靠 Spring Boot 的 `@ConditionalOnProperty`。gRPC 自动配置在 `dc3.facade.mode=grpc` 或该属性缺省时生效（
`matchIfMissing = true`）；local 自动配置只在 `dc3.facade.mode=local` 时生效。同一个 `*Facade` 接口，最终被装配成哪一套实现，完全由这个开关决定。

<FacadeModesDiagram lang="zh" />

图里 `9400` 是管理中心的 gRPC 端口（grpc 模式下数据中心要跨进程访问它）；local 模式下管理中心的 `DeviceService` 和调用方在同一个
JVM 里，`DeviceLocalFacade` 直接方法调用，连端口都不需要。

::: info 接口与实现的命名对应
`facade-api` 里的接口名是 `*Facade`（如 `DeviceFacade`）；gRPC 实现统一加 `Grpc` 中缀（`DeviceGrpcFacade`），进程内实现加
`Local` 中缀（`DeviceLocalFacade`）。看到 `*GrpcFacade` 就知道是跨进程那套，看到 `*LocalFacade` 就知道是进程内那套。
:::

## 何时用哪种

选择标准很直接——你打算把中心服务跑成几个进程：

| 维度   | `grpc`（默认）        | `local`                  |
|------|-------------------|--------------------------|
| 部署形态 | 各中心独立进程，分布式       | 所有中心合一进程，单体              |
| 调用方式 | 跨进程 gRPC          | 进程内方法调用，无网络开销            |
| 适用   | 分布式部署、需要水平扩展、生产   | 本地开发、小型单机、调试             |
| 典型搭配 | 完整 compose 栈（多服务） | `dc3-center-single` 单体服务 |

分布式部署或要按服务独立扩缩容时用 `grpc`：各中心是独立的 Spring Boot 服务，可以各自伸缩、各自重启。本地开发、小型单机或调试时用
`local`：配合把四个中心合一的 `dc3-center-single` 单体服务，省去起多个进程和它们之间的网络往返，启动快、断点直达。

::: tip 怎么选

- 你在本地起整套来开发/调 bug，或只想要一个最小可跑的单机 → 用 `local`，跑 `dc3-center-single`。
- 你要做分布式部署、按中心独立扩缩容，或这是生产环境 → 用 `grpc`（即默认），各中心独立成进程。
- 拿不准就用默认 `grpc`：它是分布式 env 的既定值，单体场景才需要显式切到 `local`。
  :::

## 默认值在哪里、谁覆盖谁

默认值随**部署形态**而不同，这点要看准，否则容易被某个 `application.yml` 里的字面值误导：

- **分布式中心**（如管理中心）：`application.yml` 写的是 `dc3.facade.mode: ${DC3_FACADE_MODE:grpc}`，且分布式编排把环境变量
  `DC3_FACADE_MODE=grpc` 显式设上（见 `.env.example` 与 `dc3/env/dev.env`）。所以分布式默认就是 `grpc`。
- **单体服务** `dc3-center-single`：`application.yml` 默认 `dc3.facade.mode: ${DC3_FACADE_MODE:local}`——单体跑在一个进程里，自然走进程内
  facade。

::: warning Auth 中心的 application.yml 写着 local，别被误导
鉴权中心 `dc3-center-auth` 的 base `application.yml` 里 `dc3.facade.mode` 直接写成 `local`（一个本地覆盖项）。但**分布式部署下
**，编排注入的 `DC3_FACADE_MODE=grpc` 会覆盖它——Auth 在分布式里实际跑的是 `grpc`。判断某个服务实际用哪种模式，**以注入的环境变量为准
**，不要只看某个 yml 里的字面值。
:::

切换模式只需改这一个开关，业务代码与接口签名都不动：

::: code-group

```bash [环境变量]
# 分布式（默认）：各中心独立进程，跨进程 gRPC
DC3_FACADE_MODE=grpc

# 单体：所有中心合一进程，进程内直调
DC3_FACADE_MODE=local
```

```yaml [application.yml]
dc3:
  facade:
    mode: ${DC3_FACADE_MODE:grpc}   # 分布式中心默认 grpc
    # mode: ${DC3_FACADE_MODE:local} # 单体 dc3-center-single 默认 local
```

:::

## 约束与边界

- `grpc` 与 `local` 是同一组 `*Facade` 接口的两套实现，**不是**两种传输协议；`local` 不是"facade 走 REST"这一档。环境变量页把
  `DC3_FACADE_MODE` 称作"facade 协议模式"，但它实际切的是装配哪套实现、即部署拓扑，以代码的 `@ConditionalOnProperty` 实现为准。
- 缺省即 `grpc`：gRPC 自动配置带 `matchIfMissing = true`，不显式配置时默认装配 gRPC 实现。
- `local` 模式要求被调用的目标 Service 必须在同一进程内——它是为 `dc3-center-single` 这类合一进程设计的；把分散的多个中心进程设成
  `local` 找不到对端 Service。
- 切换模式不改业务代码，但会改变运行拓扑与故障域：`grpc` 下一个中心崩了不拖垮其它中心，`local` 下它们共享同一个 JVM 进程。

## 延伸阅读

- [服务与拓扑](./services) — 六个服务、端口、gRPC 端口与启动依赖顺序
- [系统架构总览](./) — 网关 + 四中心 + 驱动如何协作的全景
