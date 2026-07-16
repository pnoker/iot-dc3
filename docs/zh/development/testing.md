---
title: 测试
---

<script setup>
import TestingDiagram from '../../.vitepress/theme/components/TestingDiagram.vue'
</script>


# 测试

这页讲清 IoT DC3 后端的测试怎么分层、什么时候必须补测试、用哪条命令跑哪一层。读完你能选对测试类型、在本地把单元/集成/E2E
跑起来，并知道改一处代码该补哪种测试才算"完成"。

> 你在这里：已读过[开发概览与规范](./)、想把改动验证到可合并。写驱动的测试要点另见[驱动开发](./driver-authoring)。

## 为什么要分层

测试不是越重越好。一个断言能在毫秒级捕获的逻辑错误，没必要拉起 PostgreSQL 容器去验；而跨服务的消息契约、时序库的落库行为，靠纯
Mock 又测不出真实问题。IoT DC3 据此把测试分成三层，越往下越快、越多、越孤立，越往上越慢、越少、越接近真实链路：

- **单元测试**最多，跑得最快，验证孤立的业务逻辑，不启动 Spring 上下文。
- **集成测试**用 Testcontainers 拉起真实 PostgreSQL/TimescaleDB、RabbitMQ、MQTT，验证 DAL、gRPC、消息这些跨组件协作。
- **E2E 测试**最少，验证端到端业务链路（命令下发、事件路由、时序表操作），默认关闭，靠环境变量显式启用。

选层的判断很直接：能不碰外部依赖就用单元测试；非要真容器才能复现的（SQL、消息、gRPC 契约）才上集成；只有验证整条链路时才动 E2E。

## 三层金字塔与对应命令

下图把三层、各自的范围与启动方式、以及触发命令对齐在一起。底层执行器（Surefire/Failsafe）和门禁（`DC3_E2E`）决定了同一次 `mvn`
会跑到哪一层。

<TestingDiagram lang="zh" />

::: warning 集成与 E2E 需要容器运行时
`make test-it` 和 `make test-e2e` 都通过 Testcontainers 在运行时拉起 PostgreSQL/TimescaleDB、RabbitMQ
等容器，因此本地必须有可用的容器运行时（podman 或 Docker）。E2E 在此基础上还会在共享 Docker
网络上引导一整套真实依赖。没有容器运行时时，这两条命令会失败而非跳过——`make test`（纯单元）不受影响。
:::

各层的目标、执行器与技术栈对照如下（作参考，细节以上文为准）：

| 层级     | 目标             | 执行器 / 门禁                                           | 技术方式                                                   |
|--------|----------------|----------------------------------------------------|--------------------------------------------------------|
| 单元测试   | 快速验证孤立业务逻辑     | Surefire                                           | JUnit 5、Mockito 5、AssertJ；Reactor `StepVerifier` 验证响应式 |
| 集成测试   | 验证真实基础设施与跨模块协作 | Failsafe；`*IT.java`                                | Testcontainers、gRPC InProcess、RabbitMQ harness         |
| E2E 测试 | 验证端到端业务链路      | `@EnabledIfEnvironmentVariable(named = "DC3_E2E")` | `dc3-e2e`、Testcontainers（共享 Docker 网络）                 |

## 本地怎么跑

后端命令统一走 `iot-dc3/` 下的 Makefile。最常用的四条：

```bash
make test                 # 单元测试（Surefire）
make test-it              # 集成测试（Failsafe + Testcontainers，需容器运行时）
make test-e2e             # E2E：等价于 DC3_E2E=true mvn -s .mvn/settings.xml -pl dc3-e2e -am -Pe2e verify
make coverage             # 聚合 JaCoCo 报告（dc3-coverage -am verify）
```

`make test-e2e` 本身已设置 `DC3_E2E=true` 并只在 `dc3-e2e` 模块上跑 `-Pe2e`，无需手动导出环境变量。只想跑单个模块或单个用例时，直接用
Maven：

```bash
# 指定模块的单元测试
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-manager

# 单个测试类 / 单个方法
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-manager -Dtest=DeviceControllerTest
mvn -s .mvn/settings.xml test -pl dc3-common/dc3-common-public -Dtest="RTest#testOkWithData"
```

`make coverage` 完成后，聚合报告落在：

```text
dc3-coverage/target/site/jacoco-aggregate/index.html
```

::: info Failsafe 为何指向 outputDirectory
父 POM 给 Failsafe 配置了 `classesDirectory=${project.build.outputDirectory}`。原因是 Spring Boot 会把模块产物 repackage
成可执行 fat jar，那个 jar 在 Failsafe classpath 上不可直接加载；让集成测试针对未重打包的普通编译类运行，才能正确加载被测类。这是改动驱动等可执行模块的集成测试时需要留意的坑。
:::

## 前端测试命令

前端（`dc3-web/`）用 pnpm + Vitest（单元/接口/组件/视图）与 Playwright（E2E），与后端独立：

::: code-group

```bash [Vitest 分套]
pnpm test                 # 全部 Vitest 套件
pnpm test:unit            # tests/unit
pnpm test:api             # tests/api
pnpm test:component       # tests/component
pnpm test:views           # tests/views
pnpm test:guard           # tests/guardrails（AI 编码护栏）
pnpm test:ci              # vitest run --coverage（CI 门禁）
```

```bash [Playwright E2E]
pnpm test:e2e             # headless chromium
pnpm test:e2e:headed      # 可见浏览器（E2E_HEADLESS=false）
```

:::

## 什么时候必须补测试

核心约定：**改 bug 先写能复现的回归测试，再修。** 把"这个 bug 不再出现"变成一条可执行、会失败的测试，修完它变绿，才算闭环。其余变更按风险补测：

| 变更类型          | 要求                                    |
|---------------|---------------------------------------|
| Bug 修复        | 先补能复现问题的回归测试，再实现修复                    |
| 新功能 / 行为变化    | 补单元测试，按风险追加集成测试                       |
| 重构            | 保持现有覆盖；隐式契约补 contract 测试              |
| DAL / SQL 变更  | 补 Testcontainers 测试并运行 `make test-it` |
| gRPC proto 变更 | 同步更新服务端、客户端与契约测试                      |
| 纯文档 / 格式变更    | 不需要 Java 测试，做文档构建或格式检查即可              |

## 可复用的测试基础设施

`dc3-common-test` 模块沉淀了跨模块共享的容器与基类，避免每个模块各搭一套。集成测试直接复用这些单例容器和 harness：

| 工具                       | 用途                                                    |
|--------------------------|-------------------------------------------------------|
| `PgTimescaleContainer`   | 单例 `timescale/timescaledb-ha:pg18` 容器，数据库与时序表测试       |
| `RabbitContainer`        | RabbitMQ 容器，消息发布、确认、消费测试                              |
| `MqttContainer`          | MQTT 容器，MQTT 驱动测试                                     |
| `GrpcInProcessExtension` | JUnit 5 扩展：每个测试一组 in-process gRPC server + 托管 channel |
| `RabbitTestHarness`      | 测试内收发 RabbitMQ，`awaitTrue()` 基于 Awaitility            |
| `FixedClockConfig`       | `@TestConfiguration`，把 `Clock` bean 固定到确定时刻           |

两个契约测试基类用于守住横切约定：

- `EnumContractTest<E>`：通过 `@TestFactory` 验证枚举 `getIndex()` 唯一、`ofIndex()` 往返、常量名稳定。
- `SecretFieldContractTest`：验证 `apiKey`、`password`、`secret`、`token` 等敏感字段不从 `@ToString` 与序列化中泄漏。

::: tip 时间、随机数与等待
注入 `java.time.Clock`，不要直接 `LocalDateTime.now()`；需要固定时间用 `FixedClockConfig`。异步等待用 Awaitility，禁止裸用
`Thread.sleep`。WebFlux 用 `WebTestClient`，不用 `MockMvc`；gRPC 用 in-process channel，不打开真实 socket。
:::

## 覆盖率门禁

`make coverage` 聚合各模块的 JaCoCo 数据，由 `dc3-coverage/pom.xml` 的门槛把关：

| 指标    | 当前门槛                                  |
|-------|---------------------------------------|
| 行覆盖率  | `coverage.line.minimum = 0.20`（20%）   |
| 分支覆盖率 | `coverage.branch.minimum = 0.15`（15%） |

门槛当前相对克制，便于测试体系扩展期持续推进；判定只看静态最低阈值——任一指标低于上表门槛即阻断改动，不做与历史基线的回退比较。提高门槛时，应同时提交能支撑新门槛的测试，而非只调数字。

## CI 工作流

PR 与推送在 GitHub Actions 上分三个工作流跑测试，与本地命令一一对应：

| Workflow   | 触发                                | 主要任务      |
|------------|-----------------------------------|-----------|
| `ci.yml`   | push / PR 到 develop、release、main  | 快速编译      |
| `test.yml` | push / PR 到 develop、release、main  | 单元、集成、覆盖率 |
| `e2e.yml`  | push 到 develop、release、main 或手动触发 | E2E       |

合并前应确认：单元与集成 job 通过；覆盖率不低于 `dc3-coverage/pom.xml` 门槛（低于即阻断）；行为变更在说明里写清已验证内容与未验证风险。

## 延伸阅读

- [开发概览与规范](./) — 二次开发的整体约定与命令入口
- [驱动开发](./driver-authoring) — 派生新驱动时如何补协议层与集成测试
- [环境变量](../quickstart/environment) — 本地 Java 运行所需的依赖主机与端口
