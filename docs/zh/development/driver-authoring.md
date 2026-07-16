---
title: 驱动开发
---

<script setup>
import DriverAuthoringStateDiagram from '../../.vitepress/theme/components/DriverAuthoringStateDiagram.vue'
import DriverAuthoringFlow1Diagram from '../../.vitepress/theme/components/DriverAuthoringFlow1Diagram.vue'
import DriverAuthoringFlow2Diagram from '../../.vitepress/theme/components/DriverAuthoringFlow2Diagram.vue'
import DriverAuthoringSeqDiagram from '../../.vitepress/theme/components/DriverAuthoringSeqDiagram.vue'
import DriverAuthoringFlow3Diagram from '../../.vitepress/theme/components/DriverAuthoringFlow3Diagram.vue'
</script>


# 驱动开发

驱动是 IoT DC3 的南向 I/O 层：它把 Modbus、OPC UA、MQTT、S7、BACnet 等异构协议设备，统一接入到平台的数据平面和命令平面。本页带你从
`dc3-driver-virtual` 模板派生一个新协议驱动，并讲清驱动的生命周期、读/写调度与那条"不能随手改"
的路由约束——读完你能写出一个能注册、能采集、能接受命令的驱动。

> 你在这里：想为一种现成驱动还不支持的协议接入设备。只想使用已有驱动，请先看 [操作手册](../operation/)
> 和 [快速开始](../quickstart/)。下一步可看 [命令平面](../architecture/command-plane) 理解读写命令如何流回设备。

除非特别说明，命令都在 `iot-dc3` 仓库根目录执行。

## 驱动是什么：一个聚合了 7 个 SPI 的 Spring Boot 服务

一个驱动本质上是一个独立的 Spring Boot 服务（`dc3-driver-<protocol>`）。它不直接和管理中心、数据中心打交道，而是继承
`dc3-common-driver` 这个 SDK——SDK 负责注册、调度、RabbitMQ 收发、gRPC 调用和租户上下文，**你只需要实现协议逻辑**。

协议逻辑通过一个入口接口暴露：`DriverCustomService`。它本身不声明方法，而是聚合了 7 个职责单一的 SPI 子接口，一个驱动实现这一个接口，就等于把这
7 件事都接管了：

| SPI 子接口                  | 你要回答的问题                                           |
|--------------------------|---------------------------------------------------|
| `DriverLifecycle`        | 进程启动时初始化什么（`initial()`）？每个自定义周期做什么（`schedule()`）？ |
| `DriverProtocol`         | 怎么从设备读一个位号（`read(...)`）？怎么写一个位号（`write(...)`）？    |
| `DriverCommand`          | 怎么执行模板里定义的自定义命令（`execute(...)`）？                  |
| `DriverMetadataListener` | 设备/位号元数据变更时（`event(...)`）如何刷新本地缓存？                |
| `DriverHealth`           | 驱动整体在线态是 ONLINE / OFFLINE / FAULT / MAINTAIN？     |
| `DeviceHealth`           | 单台设备的在线态如何判断？                                     |
| `DriverValidator`        | 驱动/位号配置是否合法（`validate*`）？能否生成仿真值？                 |

源码入口：`dc3-common/dc3-common-driver/.../service/DriverCustomService.java`（一行 `extends` 把 7 个接口拼起来）。
`dc3-driver-virtual` 模板把这 7 个方法都给了可运行的示例实现，是新驱动最好的起点。

::: tip 术语对齐
**属性（Attribute）** 来自驱动 `application.yml` 的 `dc3.driver.*-attribute`，定义"这个驱动有哪些配置项"；**配置（Config）**
是某台设备为这些属性填的**具体值**，存在管理中心。驱动启动时注册属性，运行时通过 `Map<String, AttributeBO>` 拿到某台设备的配置值。
:::

## 生命周期：注册（带重试）→ initial → schedule

驱动进程启动后，`DriverInitRunner`（`ApplicationRunner`）执行一段固定的引导序列：先向管理中心注册自己和全部属性定义，注册成功后调用你的
`initial()` 做一次性初始化，最后由 SDK 装配定时任务（读调度、自定义调度、设备健康检查）。

注册走 gRPC，而管理中心在驱动启动时未必就绪（滚动重启、Pod 重新调度）。所以注册不是"一锤子买卖"：
`DriverInitRunner.registerWithRetry()` 用**带上限的指数退避**重试——初始 2 秒，每次翻倍，封顶 30 秒，最多 30
次；全部失败才抛异常退出。没有它，管理中心的一次短暂抖动就会把驱动拖进 CrashLoopBackOff。

<DriverAuthoringStateDiagram lang="zh" />

源码：`dc3-common/dc3-common-driver/.../init/DriverInitRunner.java`（`REGISTER_MAX_ATTEMPTS=30`、
`REGISTER_INITIAL_BACKOFF=2s`、`REGISTER_MAX_BACKOFF=30s`）。`initial()` 只在启动时跑一次，适合建连接池、订阅关系；
`schedule()` 由 `dc3.driver.schedule.custom` 的 cron 周期触发。

## 从模板到新驱动：四步

新驱动的工作量集中在四处：拷贝模板、改 `pom.xml`、改 `application.yml`、实现 `DriverCustomService`。下图是整体路径，随后逐步展开。

<DriverAuthoringFlow1Diagram lang="zh" />

### 第 1 步：拷贝模板并重命名

驱动模块命名用 `dc3-driver-<protocol>`，协议名用 kebab-case：

```bash
cp -r dc3-driver/dc3-driver-virtual dc3-driver/dc3-driver-knx
```

然后重命名 Java 包、启动类和自定义服务实现类。模板里两个关键类是：

| 类                                | 说明                                         |
|----------------------------------|--------------------------------------------|
| `VirtualDriverApplication`       | Spring Boot 启动类                            |
| `VirtualDriverCustomServiceImpl` | 协议逻辑实现入口（`implements DriverCustomService`） |

新驱动应使用协议专用命名，例如 `KnxDriverApplication`、`KnxDriverCustomServiceImpl`，避免多个驱动出现重复类名。启动类与实现类放在同一父包下，确保组件扫描能找到带
`@Service` 的 `DriverCustomService` 实现：

```java
@SpringBootApplication
public class KnxDriverApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnxDriverApplication.class, args);
    }
}
```

### 第 2 步：接入父 POM

在 `dc3-driver/pom.xml` 的 `<modules>` 中登记新模块：

```xml
<modules>
  <module>dc3-driver-knx</module>
  <!-- existing modules -->
</modules>
```

新模块自己的 `pom.xml` 通常只继承驱动父模块，并添加协议库依赖：

```xml
<parent>
  <groupId>io.github.pnoker</groupId>
  <artifactId>dc3-driver</artifactId>
  <version>2026.5.22</version>
</parent>

<artifactId>dc3-driver-knx</artifactId>
<packaging>jar</packaging>

<dependencies>
  <!-- 协议库，例如 calimero-core (KNX)。重协议依赖只放这里，不要塞进 dc3-common-driver -->
</dependencies>
```

`dc3-driver` 父模块已引入 `dc3-common-driver`（SDK）和 Spring Boot Maven Plugin，你无需重复声明。

### 第 3 步：配置 `application.yml`

`dc3.driver` 是驱动最重要的用户可见配置。SDK 启动时读取它并注册到管理中心，管理侧据此渲染设备和位号的配置表单。下面以
`dc3-driver-virtual` 的真实结构为蓝本（把 virtual 的值换成 KNX 语义）：

```yaml
dc3:
  driver:
    tenant: default
    name: KNX Driver
    code: KnxDriver               # 稳定路由标识，详见下文约束
    type: DRIVER_CLIENT
    remark: @project.description@

    schedule:
      read:                       # 读调度：周期采集位号值
        enabled: true
        cron: '0/30 * * * * ?'    # 每 30 秒一轮
      custom:                     # 自定义调度：驱动 schedule() 回调
        enabled: true
        cron: '0/5 * * * * ?'
    health:
      device:                     # 设备健康上报
        enabled: true
        cron: '0/15 * * * * ?'
        timeout: 45               # 设备状态租约 TTL（秒）
        timeout-unit: SECONDS

    driver-attribute:             # 驱动级属性：每个设备实例填一份
      - attribute-name: Host
        attribute-code: host
        attribute-type-flag: STRING
        default-value: localhost
        remark: KNX/IP gateway host
      - attribute-name: Port
        attribute-code: port
        attribute-type-flag: INT
        default-value: 3671
        remark: KNX/IP gateway port

    point-attribute:              # 位号级属性：每个位号填一份
      - attribute-name: Group Address
        attribute-code: groupAddress
        attribute-type-flag: STRING
        default-value: 1/0/1
        remark: KNX group address

spring:
  application:
    name: @project.artifactId@
  profiles:
    active:
      - ${NODE_ENV:dev}

logging:
  file:
    name: dc3/logs/driver/knx/${spring.application.name}.log
```

属性字段的含义（前面散文已建立心智模型，下表作速查）：

| 字段                    | 说明                                                                                                           |
|-----------------------|--------------------------------------------------------------------------------------------------------------|
| `attribute-name`      | UI 显示名，驱动元数据约定用英文                                                                                            |
| `attribute-code`      | 协议实现读取的稳定 key，例如 `host`、`port`、`objectType`                                                                  |
| `attribute-type-flag` | 属性类型，`AttributeTypeEnum` 共 8 值：`STRING` / `BYTE` / `SHORT` / `INT` / `LONG` / `FLOAT` / `DOUBLE` / `BOOLEAN` |
| `default-value`       | 默认值                                                                                                          |
| `remark`              | 说明文字，建议英文                                                                                                    |

::: warning 开关字段名是 enabled
调度开关字段名为 `enabled`：`DriverScheduleServiceImpl` 读取 `getRead().getEnabled()` / `getCustom().getEnabled()` /
`device.getEnabled()`，绑定到 `DriverProperties` 内的 `private Boolean enabled`。Spring 宽松绑定不会把 `enable` 映射到
`enabled`（属于不同属性名）。`dc3-driver-virtual` 模板里写的是 `enable`，该字段实际不会生效——新驱动请用 `enabled`。注意
device health 的 `enabled` 默认为 `false`，需显式置 `true` 才启用。
:::

属性注册的链路是：`application.yml` 的 `dc3.driver` → SDK 解析为 `RegisterBO` → 经 gRPC 提交到管理中心。下图给出这条注册流的实体关系：

<DriverAuthoringFlow2Diagram lang="zh" />

### 第 4 步：实现 `DriverCustomService`

核心协议逻辑放在 `DriverCustomService` 实现里。`read(...)` 返回一条 `ReadPointValue`，`write(...)` 返回 `Boolean`
——这是协议契约的全部对外约定（源码 `DriverProtocol.java`）：

```java
@Slf4j
@Service
public class KnxDriverCustomServiceImpl implements DriverCustomService {

    @Resource
    private DriverMetadata driverMetadata;

    @Resource
    private DriverSenderService driverSenderService;

    @Override
    public void initial() {
        // 一次性初始化：建立协议栈、连接池、订阅关系
    }

    @Override
    public void schedule() {
        // 自定义周期任务，例如周期上报设备状态（带 TTL）
        driverMetadata.getDeviceIds().forEach(deviceId ->
                driverSenderService.deviceStatusSender(
                        deviceId, EntityStatusEnum.ONLINE, 45, TimeUnit.SECONDS));
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        // 响应设备/位号元数据变更（ADD/UPDATE/DELETE），刷新本地缓存或订阅
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig,
                               Map<String, AttributeBO> pointConfig,
                               DeviceBO device,
                               PointBO point) {
        String host = driverConfig.get("host").getValue(String.class);
        Integer port = driverConfig.get("port").getValue(Integer.class);
        String groupAddress = pointConfig.get("groupAddress").getValue(String.class);

        // 执行协议读取，返回原始字符串值（示例值 "0"）
        return new ReadPointValue(device, point, "0");
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig,
                         Map<String, AttributeBO> pointConfig,
                         DeviceBO device,
                         PointBO point,
                         WritePointValue writePointValue) {
        // 执行协议写入；仅当设备确认写成功才返回 true
        return true;
    }
}
```

::: danger 读/写失败不要静默吞异常
`read()` / `write()` 抛异常是 SDK 约定的失败信号——SDK 会记录日志并对 RabbitMQ 上的命令做 ack/nack。写命令失败时结果不会回显写入值（
`responseValue=null`），这是为了避免"假成功"。单个位号读取失败也不应拖垮整轮采集。
:::

## 读/写调度：数据怎么出去、命令怎么进来

驱动有两条方向相反的数据流，都由 SDK 编排，你只填协议实现。

**读（出站）**：Quartz 的 `DriverReadScheduleJob` 按 `dc3.driver.schedule.read` 的 cron 触发，从 `DriverMetadata`
缓存遍历本驱动的设备，为每台设备提交读任务（线程池），调用你的 `read()` 拿到 `ReadPointValue`，再由 SDK 经 RabbitMQ 发往数据中心。你
**不需要**自己写 RabbitMQ 或 gRPC 管道。

**写（入站）**：数据中心把读/写命令经 RabbitMQ 下发到本驱动的命令队列；`PointCommandReceiver` 做去重、按设备加锁后，反向调用你的
`read()` 或 `write()`，结果再发回数据中心。

<DriverAuthoringSeqDiagram lang="zh" />

入站写命令在驱动侧的处理不是裸调用 `write()`，而是一条带校验、去重、加锁的流水线。下图展开 `PointCommandReceiver`
的处理管线（含错误路径）：

<DriverAuthoringFlow3Diagram lang="zh" />

发送侧统一走 `DriverSenderService`（源码 `DriverSenderService.java`），常用方法：

| 方法                                                                    | 用途             |
|-----------------------------------------------------------------------|----------------|
| `pointValueSender(PointValue)` / `pointValueSender(List<PointValue>)` | 发送单条 / 批量位号值   |
| `deviceStatusSender(deviceId, status)`                                | 上报设备状态（默认 TTL） |
| `deviceStatusSender(deviceId, status, timeout, unit)`                 | 上报带 TTL 的设备状态  |
| `driverAlarmSender(String)`                                           | 上报驱动级告警        |
| `deviceAlarmSender(deviceId, String)`                                 | 上报设备级告警        |
| `eventReportSender(EventReportDTO)`                                   | 上报设备事件         |
| `pointCommandResultSender(...)` / `commandResultSender(...)`          | 回执命令结果         |

其中 `status` 取值见 `EntityStatusEnum`：`ONLINE(0)` / `OFFLINE(1)` / `MAINTAIN(2)` / `FAULT(3)`。

::: warning 设备状态上报 TTL 必须大于读周期
设备状态以"租约"形式上报：到期未续约就判离线。TTL 必须**大于**状态上报/读取周期，否则设备会在两次心跳之间被判离线、反复掉线（flap）。例如读
cron 为 `0/30 * * * * ?`（每 30 秒），TTL 应 ≥ 25 秒；模板默认设备健康 `timeout: 45` 秒，留足了余量。
:::

## 命名与路由：哪个标识不能改

驱动路由涉及三个标识，区分清楚能避免投产后改不动的坑：

| 标识                        | 来源                | 用途                                     |
|---------------------------|-------------------|----------------------------------------|
| `dc3.driver.code`         | `application.yml` | 驱动类型唯一编码，管理中心据此识别驱动类型                  |
| `dc3.driver.service`      | 自动派生或显式覆盖         | 驱动实例路由标识，用于 RabbitMQ 命令队列和 routing key |
| `spring.application.name` | Maven artifactId  | 日志文件名、Actuator 元数据等                    |

::: danger dc3.driver.code 是稳定标识，变更需迁移
`dc3.driver.code` 一旦投产就不能随手改。它作为 driverCode
注册到管理中心、绑定该驱动类型的全部元数据——改了等于换了一个驱动类型，已接入的设备会全部失联，必须配套数据迁移方案。（RabbitMQ
命令队列与 routing key 由 `dc3.driver.service` 构建，不是 `code`，见下表。）
:::

## 构建、运行与冒烟验证

本地运行先加载环境变量（让本地 Java 进程指向 Compose 发布到 localhost 的依赖端口）：

```bash
source dc3/env/dev.env.sh
```

构建新驱动及其依赖，然后运行：

::: code-group

```bash [构建]
mvn -s .mvn/settings.xml clean package -pl dc3-driver/dc3-driver-knx -am
```

```bash [运行]
java -jar dc3-driver/dc3-driver-knx/target/dc3-driver-knx.jar
```

:::

开发环境下驱动会自动向管理中心注册。查看驱动日志确认出现类似 `Driver register succeeded` 的事件即表示注册成功（注册重试时会打印
`Driver register failed on attempt n/30, retrying...`）。

走通黄金路径做一次端到端冒烟（HTTP 路径与字段来自网关合约，示例值标注为示例）：

1. 管理侧建驱动、模板、位号、设备，并为驱动属性 `host`/`port`、位号属性 `groupAddress` 填配置值。
2. 等待一个读周期（默认 30 秒）。
3. 取最新位号值，确认 `read()` 的采集已落库：

```bash
# 示例：deviceId/pointId 为示例值
curl -X POST http://localhost:8000/api/v3/data/point_value/latest \
  -H 'X-Auth-Tenant: default' \
  -H 'X-Auth-Login: dc3' \
  -H 'X-Auth-Token: <token>' \
  -H 'Content-Type: application/json' \
  -d '{"deviceId": 1, "pointId": 1, "page": {"current": 1, "size": 10}}'
```

4. 对可写位号下发写命令，确认 `write()` 被调用并回执：

```bash
curl -X POST http://localhost:8000/api/v3/data/point_command/write \
  -H 'X-Auth-Tenant: default' \
  -H 'X-Auth-Login: dc3' \
  -H 'X-Auth-Token: <token>' \
  -H 'Content-Type: application/json' \
  -d '{"deviceId": 1, "pointId": 1, "value": "42"}'
```

接口返回 `commandId`，再用它查询命令历史看执行状态（`PointCommandHistoryVO` 的 `status` 取 `SUCCESS`/`FAILED` 等，写成功时
`responseValue` 回显写入值）：

```bash
curl -X GET 'http://localhost:8000/api/v3/data/point_command_history/get_by_command_id?commandId=<commandId>' \
  -H 'X-Auth-Tenant: default' \
  -H 'X-Auth-Login: dc3' \
  -H 'X-Auth-Token: <token>'
```

完整的命令生命周期与回执语义见 [命令平面](../architecture/command-plane)。

::: info 鉴权头怎么来
所有受保护接口都需要 `X-Auth-Tenant` / `X-Auth-Login` / `X-Auth-Token`。token 通过 `POST /api/v3/auth/token/salt` 取盐、
`POST /api/v3/auth/token/generate` 换取（有效期 12 小时）。详见 [API 文档](./api-documentation)。
:::

## 常见问题

| 问题                        | 根因与处理                                                                                             |
|---------------------------|---------------------------------------------------------------------------------------------------|
| 驱动编码冲突                    | `dc3.driver.code` 重复——保持全局唯一且稳定，不要改已投产的 code                                                      |
| `DriverCustomService` 未加载 | 实现类缺 `@Service`，或不在启动类组件扫描范围内                                                                     |
| 注册一直重试不成功                 | 管理中心未就绪或 gRPC 不通——看 `Driver register failed on attempt n/30` 日志，确认 `CENTER_MANAGER_HOST` 和管理中心健康态 |
| `read` 返回空或异常             | 不要静默吞异常，让日志暴露协议错误；单点失败不应拖垮整轮采集                                                                    |
| 设备频繁离线（flap）              | 状态 TTL 小于读/上报周期——增大 TTL 或缩短调度周期                                                                   |
| 有读取但数据页无值                 | 检查 RabbitMQ 连通性、数据中心日志和租户上下文                                                                      |
| 元数据变更不生效                  | 在 `event(...)` 中更新本地协议客户端、订阅或缓存                                                                   |
| 协议依赖很重                    | 只放具体驱动模块的 `pom.xml`，不要放进 `dc3-common-driver`                                                      |

## 延伸阅读

- [命令平面](../architecture/command-plane) — 读/写命令如何下发、去重、加锁、回执，与本页的 `read()`/`write()` 对接
- [模块地图](../architecture/modules) — 28 个驱动模块的全貌与 `dc3-common-driver` SDK 在依赖树中的位置
- [领域模型](../architecture/domain-model) — Profile / Point / Device 与 Param/Attribute/Config 三层的字段与边界
- [API 文档](./api-documentation) — 鉴权流程、网关合约与 OpenAPI
- [故障排查](../guide/troubleshooting) — 启动依赖、端口与环境变量类问题
