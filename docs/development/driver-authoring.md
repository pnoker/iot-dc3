# 驱动开发

本指南说明如何基于 `dc3-driver-virtual` 创建一个新的协议驱动。驱动是 IoT DC3 的南向 I/O 层，负责通过 Modbus、OPC、MQTT、S7、BACnet 等协议连接物理设备或外部数据源。

如果只是使用已有驱动，请先阅读 [操作手册](../operation/) 和 [快速开始](../quickstart/)。

除非特别说明，命令都在仓库根目录执行。

## 驱动职责

一个驱动本质上是 Spring Boot 服务，通常负责五件事：

1. 启动时向 `dc3-center-manager` 注册驱动名称、驱动编码、设备属性和点位属性。
2. 按配置周期读取设备点位值，并把数据发送到 `dc3-center-data`。
3. 接收 Data Center 通过 RabbitMQ 下发的读写命令，并写回设备。
4. 按配置周期上报设备状态，例如 online、offline、fault、maintain。
5. 响应 Manager Center 推送的元数据变更事件，例如设备或点位新增、修改、删除。

这些能力通过 `DriverCustomService` SPI 暴露。`dc3-common-driver` 负责注册、调度、RabbitMQ、gRPC 和运行时编排，驱动实现只需要关注协议逻辑。

## 前置条件

- Java 21 和 Maven 3.9+。
- PostgreSQL、RabbitMQ、Auth Center、Manager Center、Data Center 已启动。
- 本地源码运行已加载环境变量：

```bash
source dc3/env/dev.env.sh
```

- 熟悉目标设备协议和现场连接参数。

## 1. 创建模块

驱动模块命名使用 `dc3-driver-<protocol>`，协议名使用 kebab-case。

```bash
cp -r dc3-driver/dc3-driver-virtual dc3-driver/dc3-driver-bacnet
```

然后重命名 Java 包、启动类和自定义服务实现类。`dc3-driver-virtual` 模板中最关键的类是：

| 类 | 说明 |
|----|------|
| `VirtualDriverApplication` | Spring Boot 启动类 |
| `VirtualDriverCustomServiceImpl` | 协议逻辑实现入口 |

新驱动应使用协议专用命名，例如 `BacnetDriverApplication` 和 `BacnetDriverCustomServiceImpl`，避免多个驱动出现重复类名。

## 2. 接入父 POM

在 `dc3-driver/pom.xml` 的 `<modules>` 中增加新模块：

```xml
<modules>
  <module>dc3-driver-bacnet</module>
  <!-- existing modules -->
</modules>
```

新模块自己的 `pom.xml` 通常只需要继承驱动父模块，并添加协议库依赖：

```xml
<parent>
  <groupId>io.github.pnoker</groupId>
  <artifactId>dc3-driver</artifactId>
  <version>2026.5.22</version>
</parent>

<artifactId>dc3-driver-bacnet</artifactId>
<packaging>jar</packaging>

<dependencies>
  <!-- protocol-specific libraries, for example bacnet4j -->
</dependencies>
```

`dc3-driver` 父模块已经引入 `dc3-common-driver` 和 Spring Boot Maven Plugin。

## 3. 启动类

启动类与协议实现类放在同一个包或父包下，确保默认组件扫描可以找到 `DriverCustomService` 实现。

```java
@SpringBootApplication
public class BacnetDriverApplication {
    public static void main(String[] args) {
        SpringApplication.run(BacnetDriverApplication.class, args);
    }
}
```

## 4. 配置 `application.yml`

`dc3.driver` 是驱动最重要的用户可见配置。SDK 启动时会读取这些配置并注册到 Manager Center，管理侧据此渲染设备和点位配置表单。

```yaml
dc3:
  driver:
    tenant: default
    name: BACnet Driver
    code: BacnetDriver
    type: DRIVER_CLIENT
    remark: @project.description@

    schedule:
      read:
        enable: true
        cron: '0/30 * * * * ?'
      custom:
        enable: true
        cron: '0/5 * * * * ?'

    driver-attribute:
      - attribute-name: Host
        attribute-code: host
        attribute-type-flag: STRING
        default-value: localhost
        remark: BACnet host
      - attribute-name: Port
        attribute-code: port
        attribute-type-flag: INT
        default-value: 47808
        remark: BACnet port

    point-attribute:
      - attribute-name: Object Type
        attribute-code: objectType
        attribute-type-flag: STRING
        default-value: analog-input
        remark: BACnet object type
      - attribute-name: Instance
        attribute-code: instance
        attribute-type-flag: INT
        default-value: 0
        remark: Object instance number

spring:
  application:
    name: @project.artifactId@
  profiles:
    active:
      - ${NODE_ENV:dev}

logging:
  file:
    name: dc3/logs/driver/bacnet/${spring.application.name}.log
```

### 属性命名

| 字段 | 说明 |
|------|------|
| `attribute-name` | 用户界面显示名，驱动元数据要求使用英文 |
| `attribute-code` | 协议实现读取的稳定 key，例如 `host`、`port` |
| `attribute-type-flag` | 属性类型，例如 `STRING`、`INT`、`DOUBLE`、`BOOLEAN` |
| `default-value` | 默认值 |
| `remark` | 说明文字，建议使用英文 |

驱动 metadata 中的 `name`、`attribute-name`、`remark` 属于用户可见元数据，应保持英文，便于多语言 UI 和现场项目复用。

## 5. 实现 `DriverCustomService`

核心协议逻辑放在 `DriverCustomService` 实现中：

```java
@Slf4j
@Service
public class BacnetDriverCustomServiceImpl implements DriverCustomService {

    @Resource
    private DriverMetadata driverMetadata;

    @Resource
    private DriverSenderService driverSenderService;

    @Override
    public void initial() {
        // 初始化协议栈、连接池、订阅关系等资源
    }

    @Override
    public void schedule() {
        driverMetadata.getDeviceIds().forEach(id ->
                driverSenderService.deviceStatusSender(id, DeviceStatusEnum.ONLINE, 25, TimeUnit.SECONDS));
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        // 响应设备、点位、模板等元数据变更
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig,
                               Map<String, AttributeBO> pointConfig,
                               DeviceBO device,
                               PointBO point) {
        String host = driverConfig.get("host").getValue(String.class);
        Integer port = driverConfig.get("port").getValue(Integer.class);
        String objectType = pointConfig.get("objectType").getValue(String.class);

        // 执行协议读取，返回原始字符串值
        return new ReadPointValue(device, point, "0");
    }

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig,
                         Map<String, AttributeBO> pointConfig,
                         DeviceBO device,
                         PointBO point,
                         WritePointValue writePointValue) {
        // 执行协议写入
        return true;
    }
}
```

## 6. 发送数据和状态

驱动不需要自己编写 RabbitMQ 或 gRPC 管道。通过 `DriverSenderService` 发送数据、状态和事件：

| 方法 | 用途 |
|------|------|
| `pointValueSender(PointValue)` | 发送单条点位值 |
| `pointValueSender(List<PointValue>)` | 批量发送点位值 |
| `deviceStatusSender(deviceId, status)` | 上报设备状态 |
| `deviceStatusSender(deviceId, status, ttl, unit)` | 上报带 TTL 的设备状态 |
| `driverEventSender(DriverEventDTO)` | 上报驱动事件 |
| `deviceEventSender(DeviceEventDTO)` | 上报设备事件 |
| `driverAlarmSender(String)` | 快速上报驱动告警 |
| `deviceAlarmSender(deviceId, String)` | 快速上报设备告警 |

状态 TTL 应大于状态上报周期。否则设备可能在两次心跳之间被判定为离线。例如 `cron: '0/5 * * * * ?'` 每 5 秒执行一次，TTL 可设置为 25 秒。

## 7. 命名和路由

驱动路由涉及三个标识：

| 标识 | 来源 | 用途 |
|------|------|------|
| `dc3.driver.code` | `application.yml` | 驱动类型唯一编码，Manager 用它识别驱动类型 |
| `dc3.driver.service` | 自动派生或显式覆盖 | 驱动实例路由标识，用于 RabbitMQ 命令队列和 routing key |
| `spring.application.name` | Maven artifactId | 日志文件、Actuator 元数据等 |

`dc3.driver.code` 一旦投入使用就不要随意修改。变更驱动编码会影响 Manager 元数据和 RabbitMQ 路由，需要迁移方案。

## 8. 构建和运行

构建新驱动及其依赖：

```bash
source dc3/env/dev.env.sh
mvn -s .mvn/settings.xml clean package -pl dc3-driver/dc3-driver-bacnet -am
```

运行驱动：

```bash
java -jar dc3-driver/dc3-driver-bacnet/target/dc3-driver-bacnet.jar
```

开发环境中驱动会自动向 Manager Center 注册。可查看 Manager 和驱动日志确认是否出现类似 `Driver registered: BacnetDriver` 的事件。

## 9. 冒烟验证

1. 在管理侧创建设备，填写驱动属性，例如 `host` 和 `port`。
2. 绑定点位，填写点位属性，例如 `objectType` 和 `instance`。
3. 等待一个采集周期。
4. 在数据页面、Data Center API 或数据库中查看点位值。
5. 触发写入命令，确认 `write` 方法执行并返回正确结果。

## 常见问题

| 问题 | 处理方式 |
|------|----------|
| 驱动编码冲突 | 修改 `dc3.driver.code`，保持唯一且稳定 |
| `DriverCustomService` 未加载 | 确认类有 `@Service`，且位于启动类扫描范围内 |
| `read` 返回空或异常 | 不要静默吞异常，让日志暴露协议错误；单点失败不应拖垮整个驱动 |
| 设备频繁离线 | TTL 小于心跳或状态上报周期，增加 TTL 或缩短调度周期 |
| 点位有读取但无数据 | 检查 RabbitMQ、Data Center 日志和租户上下文 |
| 元数据变更不生效 | 在 `event(...)` 中更新本地协议客户端、订阅或缓存 |
| 协议依赖很重 | 只放在具体驱动模块的 `pom.xml`，不要放到 `dc3-common-driver` |

## 参考入口

- Driver SDK：`dc3-common/dc3-common-driver`
- 虚拟驱动：`dc3-driver/dc3-driver-virtual`
- Modbus TCP 示例：`dc3-driver/dc3-driver-modbus-tcp`
- MQTT 推送示例：`dc3-driver/dc3-driver-mqtt`
- 监听式推送示例：`dc3-driver/dc3-driver-listening-virtual`
- [模块与依赖](../architecture/modules.md)
- [故障排查](../guide/troubleshooting.md)
