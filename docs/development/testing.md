# 测试

本页是 IoT DC3 后端仓库的测试参考。它补充根目录 `AGENTS.md` 的工程规则，以及 [日志规范](../guide/logging.md) 和 [环境变量](../quickstart/environment.md)。

## 测试金字塔

| 层级 | 目标 | 技术方式 | 说明 |
|------|------|----------|------|
| 单元测试 | 快速验证孤立业务逻辑 | JUnit 5、Mockito、AssertJ | 不启动完整 Spring 上下文 |
| Slice 测试 | 验证 Controller、JSON、持久化切片 | `@WebFluxTest`、`@JsonTest`、MyBatis 相关测试 | 聚焦框架边界 |
| 集成测试 | 验证真实基础设施和跨模块协作 | Testcontainers、gRPC InProcess、RabbitMQ harness | 使用 PostgreSQL、RabbitMQ、MQTT 等容器 |
| E2E 测试 | 验证端到端业务链路 | `dc3-e2e`、Testcontainers、rest-assured | 由 `DC3_E2E` 控制启用 |

## 覆盖率门槛

聚合覆盖率门槛由 `dc3-coverage/pom.xml` 控制：

| 指标 | 当前门槛 |
|------|----------|
| 行覆盖率 | 20% |
| 分支覆盖率 | 15% |

当前门槛相对克制，便于在测试体系扩展过程中持续推进。提高门槛时，应同时提交能够支撑新门槛的测试。

## 目录和命名

建议测试目录结构：

```text
<module>/src/test/
├── java/<package>/
│   ├── unit/           # *Test.java; Surefire 执行
│   ├── slice/          # *SliceTest.java; WebFlux / JSON / DAL 切片
│   ├── integration/    # *IT.java; Failsafe 执行
│   ├── contract/       # *ContractTest.java; 复用 dc3-common-test 基类
│   └── support/        # 模块专用 fixture 和 test double
└── resources/
    ├── application-test.yml
    ├── fixtures/
    └── logback-test.xml
```

命名规则：

- 单元测试：`*Test.java`。
- 集成测试：`*IT.java`。
- 测试方法建议使用 `should_<expected>_when_<condition>` 或 `given_<state>__when_<action>__then_<outcome>`。
- 断言使用 AssertJ，不引入 JUnit 4 断言。
- Mock 使用 Mockito 5；禁止引入 PowerMock。
- fixture 放在 `src/test/resources/fixtures/`，文件名体现功能和场景。

## 何时必须补测试

| 变更类型 | 要求 |
|----------|------|
| Bug 修复 | 增加能复现问题的回归测试 |
| 新功能或行为变化 | 增加单元测试，并按风险补 slice / integration 测试 |
| 重构 | 保持现有覆盖，隐式契约应补 contract 测试 |
| DAL / SQL 变更 | 增加 Testcontainers 测试，并运行 `make test-it` |
| gRPC proto 变更 | 同步更新服务端、客户端和契约测试 |
| 纯文档或格式变更 | 不需要 Java 测试，但需要文档构建或格式检查 |

## 时间、随机数和 IO

- 注入 `java.time.Clock`，不要直接调用 `LocalDateTime.now()`。
- 需要固定时间时使用 `FixedClockConfig`。
- 随机逻辑使用可注入或固定 seed 的 `RandomGenerator`。
- 静态工具可用 `Mockito.mockStatic(...)`，放在 `try-with-resources` 中。
- WebFlux 使用 `WebTestClient`；不要使用 `MockMvc`。
- gRPC 测试使用 `grpc-testing` 和 `InProcessChannelBuilder`，不打开真实 socket。
- 异步等待使用 Awaitility，禁止在测试中裸用 `Thread.sleep`。

## Testcontainers 约定

共享容器包装器位于：

```text
dc3-common/dc3-common-test/src/main/java/io/github/pnoker/test/containers/
```

常用容器：

| 容器 | 镜像 | 用途 |
|------|------|------|
| PostgreSQL / TimescaleDB | `timescale/timescaledb-ha:pg18` | 数据库和时序表测试 |
| RabbitMQ | `rabbitmq:3.13-management` | 消息发布、确认和消费测试 |
| MQTT | `eclipse-mosquitto:2.0` | MQTT 驱动测试 |

本地复用容器是可选能力，可在支持的环境中创建：

```bash
testcontainers.reuse.enable=true
```

## 测试数据

- 小型固定载荷放在 `src/test/resources/fixtures/`，便于 review diff。
- 大量对象生成使用 Instancio，并固定 seed。
- 数据库初始状态通过测试资源中的 SQL 或 Testcontainers 初始化脚本准备。
- fixture 中的敏感字段使用 `should-not-leak` 等显式占位值，避免误泄漏。

## 契约测试

`dc3-common-test` 提供可复用契约测试基类：

| 基类 | 用途 |
|------|------|
| `EnumContractTest<E>` | 验证枚举 `getIndex()` 唯一、`ofIndex()` 往返和常量稳定性 |
| `SecretFieldContractTest` | 验证 `apiKey`、`password`、`secret`、`token` 等字段不从 `toString()` 或序列化中泄漏 |

新增通用契约测试时，应放在能合理访问目标类型的模块中，避免反向依赖。

## 本地命令

```bash
make test                 # 单元测试
make test-it              # 集成测试，需要 Docker/Podman 可用
make coverage             # 聚合 JaCoCo 报告
make test-e2e             # E2E 测试，设置 DC3_E2E=true
mvn -B -pl <module> test  # 指定模块单元测试
```

`make coverage` 生成报告：

```text
dc3-coverage/target/site/jacoco-aggregate/
```

## CI 工作流

| Workflow | 触发 | 主要任务 |
|----------|------|----------|
| `ci.yml` | push / PR 到 develop、release、main | 快速编译 |
| `test.yml` | push / PR 到 develop、release、main | unit、integration、coverage |
| `e2e.yml` | push 到 develop、release、main 或手动触发 | E2E |

PR 合并前应关注：

- unit 和 integration job 通过。
- coverage job 上传 `jacoco-aggregate` artifact。
- 覆盖率低于 `dc3-coverage/pom.xml` 门槛会阻断。
- 行为变更应说明已验证内容和未验证风险。
