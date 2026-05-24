---
title: 指令与事件属性配置方案
---

# 指令与事件属性配置方案

本文用于补齐 `Command` / `Event` 在设备侧的协议映射配置能力，使 `Profile` 在已有 `Point` 能力之外，完整表达属性、指令、事件三类物模型能力。

> **设计结论**：`CommandAttribute` / `EventAttribute` 的定义由驱动在 `application.yml` 中声明，并在驱动启动时注册到
> Manager；具体配置值不放在驱动配置文件中，而是在设备编辑页面按 `device + command/event + attribute` 填写，落库到对应
> Config
> 表。

---

## 目标

- 新增 `dc3_command_attribute` / `dc3_event_attribute`，描述驱动支持的指令、事件协议映射属性。
- 新增 `dc3_command_attribute_config` / `dc3_event_attribute_config`，保存设备实例对指令、事件属性的具体配置值。
- 延续现有 `DriverAttribute` / `PointAttribute` 的注册模式，驱动通过 `application.yml` 声明 Attribute 定义。
- 在设备配置侧补齐 `PointConfig`、`CommandConfig`、`EventConfig` 三类能力，形成完整设备接入闭环。

## 非目标

- 不把 `CommandParam` / `EventParam` 与 `CommandAttribute` / `EventAttribute` 混为一类。
- 不把设备实例的具体配置值写入驱动 `application.yml`。
- 不在初版引入独立的 `ThingModel`、`Product`、`Property` 表替换现有 `Profile` / `Point` 概念。
- 不要求所有驱动一次性支持指令和事件属性；驱动可按协议能力逐步补齐。

## 概念边界

`CommandParam` / `EventParam` 是物模型业务语义，属于 `Profile` 中的指令和事件定义；`CommandAttribute` / `EventAttribute`
是驱动协议映射语义，描述驱动执行指令或识别事件时需要哪些配置项。

| 概念                 | 归属视角                    | 作用                | 示例                                                 |
|--------------------|-------------------------|-------------------|----------------------------------------------------|
| `CommandParam`     | `Command`               | 指令调用时传入/返回的业务参数   | `temperature`、`mode`、`resultCode`                  |
| `CommandAttribute` | Driver 注册，Command 配置时使用 | 指令下发所需的协议映射属性     | `functionCode`、`registerAddress`、`payloadTemplate` |
| `CommandConfig`    | `Device`                | 某个设备的某个指令对属性的实际取值 | `setTemperature.registerAddress = 40003`           |
| `EventParam`       | `Event`                 | 事件上报时携带的业务输出参数    | `faultCode`、`message`、`occurTime`                  |
| `EventAttribute`   | Driver 注册，Event 配置时使用   | 事件识别或解析所需的协议映射属性  | `topic`、`eventCodePath`、`payloadPath`              |
| `EventConfig`      | `Device`                | 某个设备的某个事件对属性的实际取值 | `deviceFault.topic = /device/001/event`            |

## Param 逻辑

Param 不属于驱动协议映射配置，而属于 `Profile` 中的业务能力定义。它描述指令调用需要哪些输入、输出，以及事件上报会携带哪些业务字段。

### CommandParam

`CommandParam` 体现指令的业务入参和出参，定义在 `Command` 下。

```text
Profile
  +-- Command: setTemperature
        +-- CommandParam: temperature    input, required
        +-- CommandParam: resultCode     output
        +-- CommandParam: message        output
```

它在以下位置发挥作用：

| 环节             | 作用                                                     |
|----------------|--------------------------------------------------------|
| Profile 编辑页    | 定义指令需要哪些输入参数、输出参数、参数类型、是否必填、默认值、枚举或范围                  |
| 指令调用 API       | 调用方提交 `deviceId + commandId/commandCode + paramValues` |
| Data Center 校验 | 校验参数是否属于该 Command、必填参数是否存在、类型是否匹配、默认值是否补齐              |
| Driver 执行      | Driver 接收经过校验的 `paramValues`，结合 `CommandConfig` 渲染协议报文 |
| CommandRecord  | 保存本次调用的输入参数、输出参数、执行状态和错误信息，便于审计和排障                     |

示例：

```text
Command:
  commandCode = setTemperature

CommandParam:
  temperature, input, required, DOUBLE

CommandAttribute:
  functionCode
  registerAddress
  payloadTemplate

CommandConfig:
  functionCode = WRITE_SINGLE_REGISTER
  registerAddress = 40003
  payloadTemplate = ${temperature}

CommandCall:
  paramValues.temperature = 26
```

最终 Driver 执行时看到的是：

```text
DriverConfig:
  host = 192.168.1.10
  port = 502

Command:
  setTemperature

CommandConfig:
  functionCode = WRITE_SINGLE_REGISTER
  registerAddress = 40003
  payloadTemplate = ${temperature}

ParamValues:
  temperature = 26
```

也就是说，`CommandParam` 负责定义业务参数，`CommandConfig` 负责说明这些业务参数如何被映射成驱动协议报文。

### EventParam

`EventParam` 体现事件上报时会携带哪些业务字段，定义在 `Event` 下。

```text
Profile
  +-- Event: deviceFault
        +-- EventParam: faultCode
        +-- EventParam: message
        +-- EventParam: occurTime
```

它在以下位置发挥作用：

| 环节              | 作用                                                     |
|-----------------|--------------------------------------------------------|
| Profile 编辑页     | 定义事件输出字段、字段类型、默认值、枚举、范围或 JSON Schema 片段                |
| Driver 事件解析     | Driver 根据 `EventConfig` 从协议报文中识别事件，并解析出参数值             |
| EventReport API | Driver 上报 `deviceId + eventId/eventCode + paramValues` |
| Data Center 校验  | 校验事件是否属于设备 Profile，参数是否属于该 Event，类型是否匹配                |
| EventRecord     | 保存事件参数快照，供查询、告警规则、审计和后续分析使用                            |

示例：

```text
Event:
  eventCode = deviceFault

EventParam:
  faultCode
  message

EventAttribute:
  sourceTopic
  eventCodePath
  payloadPath

EventConfig:
  sourceTopic = /device/001/event
  eventCodePath = $.eventCode
  payloadPath = $.payload

EventReport:
  paramValues.faultCode = E001
  paramValues.message = Motor overload
```

也就是说，`EventAttribute` / `EventConfig` 负责把协议报文解析成某个事件，`EventParam` 负责约束和说明这个事件最终上报出来的业务字段。

## 领域模型

业务视角上，`Profile` 仍然是一类设备的能力模型，`Device` 绑定一个 `Profile` 后就拥有该 Profile 定义的 `Point`、`Command`、
`Event`。配置视角上，`Device` 再结合自己的 `driverId`，对这些能力做协议映射配置。

```text
Driver
  +-- DriverAttribute
  +-- DriverConfig

Profile
  +-- Point
  |     +-- PointAttribute
  |
  +-- Command
  |     +-- CommandParam
  |     +-- CommandAttribute
  |
  +-- Event
        +-- EventParam
        +-- EventAttribute

Device
  +-- profileId
  +-- driverId
  +-- PointConfig    device + point + pointAttribute
  +-- CommandConfig  device + command + commandAttribute
  +-- EventConfig    device + event + eventAttribute
```

需要注意，这里的 `PointAttribute` / `CommandAttribute` / `EventAttribute` 表示用户在配置 `Point`、`Command`、`Event`
时看到的属性集合；从实现来源看，它们由具体 Driver 在启动时注册，因为不同协议需要的映射属性不同。

## Attribute 定义来源

驱动 `application.yml` 只声明 Attribute 定义，不声明具体设备配置值。当前系统已经支持：

```yaml
dc3:
  driver:
    driver-attribute:
      - attribute-name: Host
        attribute-code: host
        attribute-type-flag: STRING
        default-value: localhost

    point-attribute:
      - attribute-name: Function Code
        attribute-code: functionCode
        attribute-type-flag: INT
        default-value: 3
```

本方案在同一层级新增：

```yaml
dc3:
  driver:
    command-attribute:
      - attribute-name: Function Code
        attribute-code: functionCode
        attribute-type-flag: STRING
        default-value: WRITE_SINGLE_REGISTER
        remark: Command write function

      - attribute-name: Register Address
        attribute-code: registerAddress
        attribute-type-flag: STRING
        default-value: ''
        remark: Target register or command address

      - attribute-name: Payload Template
        attribute-code: payloadTemplate
        attribute-type-flag: STRING
        default-value: ''
        remark: Payload template rendered with command params

    event-attribute:
      - attribute-name: Source Topic
        attribute-code: sourceTopic
        attribute-type-flag: STRING
        default-value: ''
        remark: Topic or channel used to receive event payload

      - attribute-name: Event Code Path
        attribute-code: eventCodePath
        attribute-type-flag: STRING
        default-value: $.eventCode
        remark: JSON path used to resolve event code

      - attribute-name: Payload Path
        attribute-code: payloadPath
        attribute-type-flag: STRING
        default-value: $.payload
        remark: JSON path used to resolve event params
```

### 配置文件职责

| 放置位置                     | 内容                | 是否需要重启 | 说明                           |
|--------------------------|-------------------|--------|------------------------------|
| Driver `application.yml` | Attribute 定义      | 是      | 驱动开发者声明协议支持哪些配置项             |
| Manager DB               | Attribute 定义同步结果  | 否      | 驱动启动注册后持久化，供 Web 和运行时查询      |
| Device Edit 页面           | Config 具体配置值      | 否      | 用户按设备、指令、事件填写实际映射配置          |
| Config 表                 | Config 具体配置值持久化结果 | 否      | 运行时 Driver 从 Manager 拉取并组装使用 |

设备实例配置值不放入 `application.yml`，原因是它们属于租户、设备和业务模型的运行配置，需要在 Web
页面中可编辑、可审计、可热更新。放入驱动配置文件会导致修改设备配置必须重启驱动，也无法支持同一驱动服务管理大量不同设备。

## Attribute / Config 能力边界

`Attribute + Config` 可以解决 `Command` / `Event` 的协议映射问题，但不能单独替代 `Command` / `Event` / `Param` /
运行记录 / 驱动执行链路。

### 可以解决的问题

| 场景     | Attribute 定义什么              | Config 配置什么                                  |
|--------|-----------------------------|----------------------------------------------|
| 指令下发   | 驱动执行指令需要哪些协议字段              | 某个设备的某个指令使用哪些协议字段值                           |
| 指令报文模板 | 模板字段、地址字段、方法字段、主题字段等        | 模板内容、寄存器地址、HTTP URL、MQTT Topic 等             |
| 指令参数映射 | 哪些配置项支持引用 `CommandParam`    | `payloadTemplate = ${temperature}` 这类映射关系    |
| 事件来源识别 | 驱动监听事件需要哪些来源字段              | Topic、地址、通道、订阅表达式等具体值                        |
| 事件报文解析 | 事件编码路径、载荷路径、条件表达式等          | `eventCodePath`、`payloadPath`、`condition` 的值 |
| 不同协议差异 | 不同 Driver 声明各自需要的 Attribute | 同一 Profile 在不同 Driver 下可以配置不同映射值             |

例如 `setTemperature` 指令：

```text
CommandParam:
  temperature

CommandAttribute:
  functionCode
  registerAddress
  payloadTemplate

CommandConfig:
  functionCode = WRITE_SINGLE_REGISTER
  registerAddress = 40003
  payloadTemplate = ${temperature}
```

这里 `Attribute + Config` 已经能表达“业务参数 temperature 如何变成驱动协议报文”。

### 不能单独解决的问题

| 问题              | 需要的模型或链路                            |
|-----------------|-------------------------------------|
| 指令本身是什么         | `Command`                           |
| 指令需要哪些业务入参/出参   | `CommandParam`                      |
| 事件本身是什么         | `Event`                             |
| 事件上报哪些业务字段      | `EventParam`                        |
| 指令调用校验、投递、回执、超时 | Data Center 指令调用链路 + Driver SDK     |
| 事件上报、持久化、告警规则触发 | EventReport 链路 + EventRecord + 告警规则 |
| 复杂组合指令或多步骤流程    | 后续可扩展 `command_ext` 或流程编排能力         |
| 跨点位、跨设备聚合事件     | 后续由规则引擎或事件规则模型承载                    |

因此，初版结论是：`Attribute + Config` 足以补齐大多数驱动协议映射缺口；`Param` 负责业务参数，运行链路负责调用、解析、记录和规则触发。

## 新增表结构

### `dc3_command_attribute`

指令属性定义表，描述某个 Driver 支持哪些指令协议映射属性。

| 字段                    | 类型            | 说明                    |
|-----------------------|---------------|-----------------------|
| `id`                  | `BIGINT`      | 主键 ID                 |
| `attribute_name`      | `TEXT`        | 属性名称                  |
| `attribute_code`      | `TEXT`        | 属性编码                  |
| `attribute_type_flag` | `SMALLINT`    | 属性类型                  |
| `default_value`       | `TEXT`        | 默认值                   |
| `driver_id`           | `BIGINT`      | 驱动 ID                 |
| `attribute_ext`       | `JSON`        | 属性扩展信息，例如组件类型、选项、校验规则 |
| `enable_flag`         | `SMALLINT`    | 启停状态                  |
| `tenant_id`           | `BIGINT`      | 租户 ID                 |
| `remark`              | `TEXT`        | 说明                    |
| `signature`           | `TEXT`        | 签名                    |
| `version`             | `INTEGER`     | 版本                    |
| `creator_id`          | `BIGINT`      | 创建人 ID                |
| `creator_name`        | `TEXT`        | 创建人名称                 |
| `create_time`         | `TIMESTAMPTZ` | 创建时间                  |
| `operator_id`         | `BIGINT`      | 操作人 ID                |
| `operator_name`       | `TEXT`        | 操作人名称                 |
| `operate_time`        | `TIMESTAMPTZ` | 操作时间                  |
| `deleted`             | `SMALLINT`    | 逻辑删除标识                |

建议索引：

```sql
CREATE UNIQUE INDEX idx_command_attribute_active_unique
    ON dc3_command_attribute (tenant_id, driver_id, attribute_code) WHERE deleted = 0 AND attribute_code <> ''::TEXT;

CREATE INDEX idx_command_attribute_driver_id
    ON dc3_command_attribute (driver_id) WHERE deleted = 0;
```

### `dc3_event_attribute`

事件属性定义表，描述某个 Driver 支持哪些事件识别、解析、上报映射属性。

| 字段                    | 类型            | 说明                    |
|-----------------------|---------------|-----------------------|
| `id`                  | `BIGINT`      | 主键 ID                 |
| `attribute_name`      | `TEXT`        | 属性名称                  |
| `attribute_code`      | `TEXT`        | 属性编码                  |
| `attribute_type_flag` | `SMALLINT`    | 属性类型                  |
| `default_value`       | `TEXT`        | 默认值                   |
| `driver_id`           | `BIGINT`      | 驱动 ID                 |
| `attribute_ext`       | `JSON`        | 属性扩展信息，例如组件类型、选项、校验规则 |
| `enable_flag`         | `SMALLINT`    | 启停状态                  |
| `tenant_id`           | `BIGINT`      | 租户 ID                 |
| `remark`              | `TEXT`        | 说明                    |
| `signature`           | `TEXT`        | 签名                    |
| `version`             | `INTEGER`     | 版本                    |
| `creator_id`          | `BIGINT`      | 创建人 ID                |
| `creator_name`        | `TEXT`        | 创建人名称                 |
| `create_time`         | `TIMESTAMPTZ` | 创建时间                  |
| `operator_id`         | `BIGINT`      | 操作人 ID                |
| `operator_name`       | `TEXT`        | 操作人名称                 |
| `operate_time`        | `TIMESTAMPTZ` | 操作时间                  |
| `deleted`             | `SMALLINT`    | 逻辑删除标识                |

建议索引：

```sql
CREATE UNIQUE INDEX idx_event_attribute_active_unique
    ON dc3_event_attribute (tenant_id, driver_id, attribute_code) WHERE deleted = 0 AND attribute_code <> ''::TEXT;

CREATE INDEX idx_event_attribute_driver_id
    ON dc3_event_attribute (driver_id) WHERE deleted = 0;
```

### `dc3_command_attribute_config`

指令属性配置表，保存某个设备的某个指令对某个指令属性的实际配置值。

| 字段              | 类型            | 说明      |
|-----------------|---------------|---------|
| `id`            | `BIGINT`      | 主键 ID   |
| `attribute_id`  | `BIGINT`      | 指令属性 ID |
| `config_value`  | `TEXT`        | 指令属性配置值 |
| `device_id`     | `BIGINT`      | 设备 ID   |
| `command_id`    | `BIGINT`      | 指令 ID   |
| `config_ext`    | `JSON`        | 配置扩展信息  |
| `enable_flag`   | `SMALLINT`    | 启停状态    |
| `tenant_id`     | `BIGINT`      | 租户 ID   |
| `remark`        | `TEXT`        | 说明      |
| `signature`     | `TEXT`        | 签名      |
| `version`       | `INTEGER`     | 版本      |
| `creator_id`    | `BIGINT`      | 创建人 ID  |
| `creator_name`  | `TEXT`        | 创建人名称   |
| `create_time`   | `TIMESTAMPTZ` | 创建时间    |
| `operator_id`   | `BIGINT`      | 操作人 ID  |
| `operator_name` | `TEXT`        | 操作人名称   |
| `operate_time`  | `TIMESTAMPTZ` | 操作时间    |
| `deleted`       | `SMALLINT`    | 逻辑删除标识  |

建议索引：

```sql
CREATE UNIQUE INDEX idx_command_attribute_config_active_unique
    ON dc3_command_attribute_config (tenant_id, device_id, command_id, attribute_id) WHERE deleted = 0;

CREATE INDEX idx_command_attribute_config_attribute_id
    ON dc3_command_attribute_config (attribute_id) WHERE deleted = 0;

CREATE INDEX idx_command_attribute_config_device_command
    ON dc3_command_attribute_config (device_id, command_id) WHERE deleted = 0;
```

### `dc3_event_attribute_config`

事件属性配置表，保存某个设备的某个事件对某个事件属性的实际配置值。

| 字段              | 类型            | 说明      |
|-----------------|---------------|---------|
| `id`            | `BIGINT`      | 主键 ID   |
| `attribute_id`  | `BIGINT`      | 事件属性 ID |
| `config_value`  | `TEXT`        | 事件属性配置值 |
| `device_id`     | `BIGINT`      | 设备 ID   |
| `event_id`      | `BIGINT`      | 事件 ID   |
| `config_ext`    | `JSON`        | 配置扩展信息  |
| `enable_flag`   | `SMALLINT`    | 启停状态    |
| `tenant_id`     | `BIGINT`      | 租户 ID   |
| `remark`        | `TEXT`        | 说明      |
| `signature`     | `TEXT`        | 签名      |
| `version`       | `INTEGER`     | 版本      |
| `creator_id`    | `BIGINT`      | 创建人 ID  |
| `creator_name`  | `TEXT`        | 创建人名称   |
| `create_time`   | `TIMESTAMPTZ` | 创建时间    |
| `operator_id`   | `BIGINT`      | 操作人 ID  |
| `operator_name` | `TEXT`        | 操作人名称   |
| `operate_time`  | `TIMESTAMPTZ` | 操作时间    |
| `deleted`       | `SMALLINT`    | 逻辑删除标识  |

建议索引：

```sql
CREATE UNIQUE INDEX idx_event_attribute_config_active_unique
    ON dc3_event_attribute_config (tenant_id, device_id, event_id, attribute_id) WHERE deleted = 0;

CREATE INDEX idx_event_attribute_config_attribute_id
    ON dc3_event_attribute_config (attribute_id) WHERE deleted = 0;

CREATE INDEX idx_event_attribute_config_device_event
    ON dc3_event_attribute_config (device_id, event_id) WHERE deleted = 0;
```

## 运行链路

### 驱动启动注册

1. Driver 从 `application.yml` 读取 `driver-attribute`、`point-attribute`、`command-attribute`、`event-attribute`。
2. Driver 注册到 Manager 时携带四类 Attribute 定义。
3. Manager 以 `tenant_id + driver_id + attribute_code` 为唯一键写入或更新 Attribute 表。
4. Manager 返回注册后的 Attribute 列表，Driver SDK 缓存到 `DriverMetadata`。

### 设备配置

1. 用户在设备编辑页面选择 `driverId` 和 `profileId`。
2. 页面加载 Driver 提供的 `PointAttribute`、`CommandAttribute`、`EventAttribute`。
3. 页面加载 Profile 下的 `Point`、`Command`、`Event`。
4. 用户分别配置：
    - `PointConfig = device + point + pointAttribute`
    - `CommandConfig = device + command + commandAttribute`
    - `EventConfig = device + event + eventAttribute`
5. Manager 保存配置值到对应 Config 表。

### 指令下发

1. Data Center 收到 `deviceId + commandId/commandCode + paramValues`。
2. 校验 `device.profileId == command.profileId`。
3. 查询 `CommandConfig(deviceId, commandId)`，组装为 `Map<String, AttributeBO>`。
4. 投递 `CommandCallDTO` 到对应 Driver。
5. Driver 执行时同时使用：
    - `DriverConfig`
    - `Device`
    - `Command`
    - `CommandParam` 入参值
    - `CommandConfig`

### 事件上报

事件有两类来源，均使用同一套事件定义和记录表。

| 来源             | 说明                            |
|----------------|-------------------------------|
| Driver 主动上报    | 驱动解析协议报文后直接识别 `eventCode` 并上报 |
| Data Center 识别 | 后续可基于点位值或规则识别事件               |

Driver 主动上报场景：

1. Driver 根据 `EventConfig(deviceId, eventId)` 订阅、监听或解析事件来源。
2. Driver 根据 `eventCodePath`、`payloadPath` 等属性识别事件。
3. Driver 通过 `DriverSenderService.eventReportSender()` 上报 `EventReportDTO`。
4. Data Center 校验 `device.profileId == event.profileId`。
5. Data Center 保存 `EventRecord`，并继续触发告警规则链路。

## UI 设计要求

前端交互需要避免“选一个对象、配一堆字段、单独保存一次”的割裂体验。设备配置页应以 `driverId + profileId`
为上下文，一次性生成可编辑的配置矩阵，让用户能批量查看、批量编辑、批量保存。

### Profile 页面

`Profile` 负责定义能力，不负责填写设备协议配置值。

- `Related Points` 展示 Profile 下的 Point。
- `Related Commands` 展示 Profile 下的 Command。
- `Related Events` 展示 Profile 下的 Event。
- Profile 编辑步骤中包含 Point、Command、Event 的定义维护。

Profile 编辑建议采用以下步骤：

| 步骤 | 内容           | 说明                                 |
|----|--------------|------------------------------------|
| 1  | Profile Info | 维护模板基础信息                           |
| 2  | Points       | 维护 Point 基础定义                      |
| 3  | Commands     | 维护 Command 基础定义和 CommandParam      |
| 4  | Events       | 维护 Event 基础定义和 EventParam          |
| 5  | Review       | 汇总 Point / Command / Event 数量和关键字段 |

Command 编辑交互：

- 主表维护 `commandName`、`commandCode`、`commandTypeFlag`、`callTypeFlag`、`timeout`、`enableFlag`。
- 行展开或侧边抽屉维护 `CommandParam`，使用表格编辑 `paramName`、`paramCode`、`direction`、`type`、`required`、`defaultValue`。
- Param 表格支持新增、删除、排序和编码唯一性校验。
- 不在 Profile 编辑页填写 `CommandConfig` 具体值，因为此时不一定知道设备使用哪个 Driver。

Event 编辑交互：

- 主表维护 `eventName`、`eventCode`、`eventTypeFlag`、`eventLevelFlag`、`enableFlag`。
- 行展开或侧边抽屉维护 `EventParam`，使用表格编辑 `paramName`、`paramCode`、`type`、`defaultValue`。
- EventParam 表格支持新增、删除、排序和编码唯一性校验。
- 不在 Profile 编辑页填写 `EventConfig` 具体值。

### Device 页面

`Device` 负责把设备实例与驱动、Profile 和协议映射配置串起来。

- `Device Info` 显示当前 `driverId`、`profileId`。
- `Related Points` 展示设备从 Profile 继承的 Point，并提供 PointConfig 配置入口。
- `Related Commands` 展示设备从 Profile 继承的 Command，并提供 CommandConfig 配置入口。
- `Related Events` 展示设备从 Profile 继承的 Event，并提供 EventConfig 配置入口。

Device 编辑建议采用以下步骤：

| 步骤 | 内容             | 说明                                       |
|----|----------------|------------------------------------------|
| 1  | Device Info    | 选择 Driver、Profile，维护设备基础信息               |
| 2  | Driver Config  | 配置 `device + driverAttribute`            |
| 3  | Point Config   | 配置 `device + point + pointAttribute`     |
| 4  | Command Config | 配置 `device + command + commandAttribute` |
| 5  | Event Config   | 配置 `device + event + eventAttribute`     |
| 6  | Review         | 汇总缺失项、默认值、配置完整度和保存结果                     |

### 配置矩阵交互

Point / Command / Event Config 采用同一种矩阵式交互。

```text
左侧或首列：Profile 下的能力对象
  - Point
  - Command
  - Event

表格列：当前 Driver 声明的 Attribute
  - PointAttribute
  - CommandAttribute
  - EventAttribute

单元格：当前 Device 的 ConfigValue
```

Command Config 示例：

| Command          | functionCode            | registerAddress | payloadTemplate  | 状态 |
|------------------|-------------------------|-----------------|------------------|----|
| `restart`        | `WRITE_SINGLE_REGISTER` | `40001`         | `1`              | 完整 |
| `setMode`        | `WRITE_SINGLE_REGISTER` | `40002`         | `${mode}`        | 完整 |
| `setTemperature` | `WRITE_SINGLE_REGISTER` | `40003`         | `${temperature}` | 完整 |

Event Config 示例：

| Event         | sourceTopic         | eventCodePath | payloadPath | condition                  | 状态 |
|---------------|---------------------|---------------|-------------|----------------------------|----|
| `deviceFault` | `/device/001/event` | `$.eventCode` | `$.payload` | `faultCode != null`        | 完整 |
| `doorOpened`  | `/device/001/event` | `$.eventCode` | `$.payload` | `eventCode == 'DOOR_OPEN'` | 完整 |

交互要求：

- 支持表格内直接编辑 ConfigValue。
- 支持一键应用 Attribute 默认值。
- 支持从其他设备复制配置。
- 支持按缺失、已配置、已禁用筛选。
- 支持保存全部变更，减少逐个对象保存的割裂感。
- 支持单行保存或自动保存作为增强能力，但不作为初版必须项。
- Attribute 的 `attribute_ext` 可驱动不同输入控件，例如输入框、数字输入、下拉选择、模板编辑器、JSONPath 输入框。

### Command Config 交互细节

Command Config 页面需要把 `CommandParam` 作为模板变量提示，而不是作为设备配置项。

- 每行展示 Command 的基础信息和 Param 摘要。
- 编辑 `payloadTemplate`、`url`、`topic` 等字段时，提供可插入的 Param 变量列表，例如 `${temperature}`、`${mode}`。
- 支持“预览报文”：用户输入测试参数，前端渲染模板，展示最终 payload。
- 支持“测试调用”：使用测试参数调用当前设备的指令接口，生成一条测试 CommandRecord。

### Event Config 交互细节

Event Config 页面需要帮助用户验证事件识别和参数解析是否正确。

- 每行展示 Event 的基础信息和 EventParam 摘要。
- 编辑 `eventCodePath`、`payloadPath`、`condition` 等字段时，提供 JSONPath 或表达式输入控件。
- 支持“解析预览”：用户粘贴一段样例报文，前端根据当前 EventConfig 解析出 `eventCode` 和 `paramValues`。
- 支持“模拟上报”：将解析结果按 `EventReportDTO` 格式提交到 Data Center，生成一条测试 EventRecord。

### Driver 页面

Driver 页面可提供只读或半只读的 Attribute 查看能力，帮助用户理解当前驱动支持哪些配置项。

- `DriverAttribute`：连接配置项。
- `PointAttribute`：点位采集配置项。
- `CommandAttribute`：指令下发配置项。
- `EventAttribute`：事件识别和解析配置项。

驱动注册产生的 Attribute 默认不建议在 Web 中随意改编码；如需支持编辑，应限制为名称、描述、默认值、`attribute_ext` 等非破坏性字段。

## 实施阶段

| 阶段      | 内容                                                      |
|---------|---------------------------------------------------------|
| Phase 1 | 新增四张表和种子 SQL，补齐 DO/DTO/BO/Mapper/Service 基础层            |
| Phase 2 | 扩展 DriverProperties、注册 DTO、Manager 注册接口和 DriverMetadata |
| Phase 3 | 补齐 CommandConfig / EventConfig 的 Manager API            |
| Phase 4 | 扩展 Data Center 指令调用链路，给 Driver 传入 CommandConfig         |
| Phase 5 | 扩展 Driver 事件解析链路，支持基于 EventConfig 识别事件                  |
| Phase 6 | 补齐 Web 设备配置页面和 Profile / Device 详情页展示                   |

## 待确认问题

### `attribute_ext` 是否需要统一 UI 组件协议

需要。`attribute_type_flag` 只能表达值类型，例如字符串、数字、布尔、JSON；但前端配置页还需要知道用什么控件展示、是否必填、下拉选项有哪些、是否允许引用 Param 变量、是否需要脱敏展示。

如果不约定 `attribute_ext`，前端只能把所有 Attribute 渲染成普通输入框，体验会退化为“能填但不好用”。如果每个驱动都在前端硬编码一套表单，又会导致驱动和 Web 强耦合。

推荐把 `attribute_ext` 约定为轻量 UI 协议，初版只定义必要字段，复杂能力后续再扩展。

```json
{
  "ui": {
    "component": "template",
    "required": true,
    "placeholder": "Use ${paramCode} to reference command params",
    "group": "Protocol Mapping",
    "order": 30,
    "variables": "commandParam"
  },
  "validation": {
    "min": null,
    "max": null,
    "regex": ""
  },
  "security": {
    "secret": false
  }
}
```

建议初版字段：

| 字段              | 说明                                                         |
|-------------------|--------------------------------------------------------------|
| `ui.component`    | 控件类型，如 `input`、`number`、`select`、`switch`、`textarea`、`json`、`jsonpath`、`template`、`expression`、`password` |
| `ui.required`     | 是否必填                                                     |
| `ui.placeholder`  | 输入提示                                                     |
| `ui.options`      | 下拉、单选、多选的选项                                       |
| `ui.group`        | 表单分组或表格列分组                                         |
| `ui.order`        | 展示排序                                                     |
| `ui.variables`    | 是否允许引用变量，如 `commandParam`、`eventParam`、`none`       |
| `validation.min`  | 数值最小值或字符串最短长度                                   |
| `validation.max`  | 数值最大值或字符串最长长度                                   |
| `validation.regex`| 正则校验                                                     |
| `security.secret` | 是否敏感字段，敏感字段在列表、快照、日志中需要脱敏             |

前端渲染规则：

1. 优先读取 `attribute_ext.ui.component`。
2. 如果没有配置 `component`，按 `attribute_type_flag` 兜底渲染。
3. 保存时前端做体验校验，后端做最终校验。
4. `secret = true` 的字段不在日志和历史快照中保存明文。

### Command/Event Attribute 是否需要按类型过滤展示

需要支持，但建议作为 Attribute 的展示和校验规则存在，而不是新增硬编码列。原因是过滤维度不只 `command_type_flag` / `event_type_flag`，后续还可能按 `call_type_flag`、驱动协议能力、事件来源类型、是否支持模板变量等维度过滤。

推荐在 `attribute_ext` 中增加适用范围：

```json
{
  "appliesTo": {
    "commandTypeFlags": ["CUSTOM", "ACTION"],
    "callTypeFlags": ["SYNC", "ASYNC"],
    "eventTypeFlags": ["ALERT", "FAULT"],
    "eventSourceTypes": ["DRIVER"]
  }
}
```

展示规则：

| 场景                     | 处理方式                                                                 |
|--------------------------|--------------------------------------------------------------------------|
| `appliesTo` 为空          | 认为该 Attribute 适用于当前 Driver 下所有 Command 或 Event                |
| 当前 Command 类型匹配     | 在 Command Config 矩阵中展示该 Attribute 列                              |
| 当前 Command 类型不匹配   | 不展示该 Attribute 列，已有配置保存时也应被后端拒绝或标记为无效            |
| 当前 Event 类型匹配       | 在 Event Config 矩阵中展示该 Attribute 列                                |
| 当前 Event 类型不匹配     | 不展示该 Attribute 列，已有配置保存时也应被后端拒绝或标记为无效            |

示例：

```json
{
  "ui": {
    "component": "template",
    "variables": "commandParam"
  },
  "appliesTo": {
    "commandTypeFlags": ["CUSTOM", "ACTION"]
  }
}
```

这个配置表示 `payloadTemplate` 只在 `CUSTOM`、`ACTION` 类型指令中展示，配置型指令如果不需要报文模板，就不会在表格中出现这一列。

### EventConfig 初版覆盖 Driver 主动上报还是 PointValue 规则识别

初版建议只覆盖 Driver 主动上报。Data Center 基于 `PointValue` 的规则识别应该复用 `Event` / `EventParam` / `EventRecord`，但不应该放进 `EventConfig` 的初版范围。

原因是两者职责不同：

| 来源                  | 本质                             | 配置模型                                   |
|-----------------------|----------------------------------|--------------------------------------------|
| Driver 主动上报        | 驱动协议层从报文、Topic、通道中识别事件 | `EventAttribute` + `EventConfig`            |
| PointValue 规则识别    | 平台规则层根据采集值推导业务事件       | 后续独立的 EventRule / Rule Engine 配置     |

Driver 主动上报链路：

```text
Driver 原始报文
  -> EventConfig 解析 eventCode / paramValues
  -> DriverSenderService.eventReportSender()
  -> Data Center 校验
  -> EventRecord
  -> 告警规则
```

以 MQTT Driver 为例，事件入站消息先按 `sourceTopic` 匹配设备事件配置，命中后再用 `eventCodePath` 和 `payloadPath` 从 JSON 报文中抽取事件编码和事件参数：

```json
{
  "eventCode": "alarm",
  "payload": {
    "temperature": "92",
    "source": "mqtt"
  }
}
```

对应配置：

| Attribute Code  | 示例值             | 作用                                      |
|-----------------|--------------------|-------------------------------------------|
| `sourceTopic`   | `dc3/event/device-a` | 匹配 MQTT 入站 Topic，支持 `+` / `#` 通配符 |
| `eventCodePath` | `$.eventCode`        | 抽取事件编码，并与 Profile Event Code 校验 |
| `payloadPath`   | `$.payload`          | 抽取事件参数，写入 `param_values`          |

如果入站 Topic 未命中任何 EventConfig，MQTT Driver 会保留原有 PointValue 路径，继续尝试将消息解析为点位值。这样事件上报和点位上报共享 MQTT 入站通道，但通过配置边界清晰分流。

PointValue 规则识别链路：

```text
PointValue
  -> Rule Engine / EventRule
  -> 生成 eventCode / paramValues
  -> EventRecord
  -> 告警规则
```

如果初版把 PointValue 规则也塞进 `EventConfig`，会让 `EventConfig` 同时承担“驱动协议映射”和“平台规则表达”两种职责，后面会很难维护。例如 `sourceTopic`、`eventCodePath` 是驱动协议字段，而 `temperature > 80` 是平台规则字段，它们不是同一层模型。

建议后续单独设计：

```text
EventRule
  +-- profileId
  +-- eventId
  +-- sourceType = POINT_VALUE
  +-- conditionExpression
  +-- paramTemplate
  +-- enableFlag
```

这样 `EventConfig` 保持驱动映射职责，`EventRule` 负责平台推导事件，二者最终都落到同一个 `EventRecord`。

### 指令执行结果是否写入 CommandConfig 快照

需要写入，但要做脱敏和裁剪。业务上通常称为 `CommandRecord`，当前数据侧落库表是 `dc3_command_history`。

原因是指令配置会变化。如果历史记录只保存 `commandId` 和 `paramValues`，后续用户修改了 `CommandConfig`，再回看旧记录时就无法知道当时到底是写了哪个寄存器、发到了哪个 Topic、用了哪个 payload 模板。

建议在指令创建记录时保存配置快照：

```json
{
  "driverId": 123,
  "profileId": 456,
  "commandId": 789,
  "commandCode": "setTemperature",
  "commandConfig": {
    "functionCode": {
      "attributeId": 1,
      "attributeName": "Function Code",
      "configValue": "WRITE_SINGLE_REGISTER"
    },
    "registerAddress": {
      "attributeId": 2,
      "attributeName": "Register Address",
      "configValue": "40003"
    },
    "payloadTemplate": {
      "attributeId": 3,
      "attributeName": "Payload Template",
      "configValue": "${temperature}"
    }
  }
}
```

快照的作用：

| 作用             | 说明                                                   |
|------------------|--------------------------------------------------------|
| 审计             | 能确认某次指令执行时实际使用了哪些协议映射配置           |
| 排障             | 设备响应异常时可以回看当时的地址、Topic、模板、超时配置   |
| 防止历史漂移      | 后续修改 Config 不影响历史记录解释                       |
| 支持重放分析      | 可以用历史 ParamValues + ConfigSnapshot 复现当时报文      |

需要注意：

- `secret = true` 的 Attribute 不保存明文，只保存掩码或摘要。
- 快照保存 `attributeCode` / `attributeName` / `configValue`，不能只保存 `attributeId`，因为 Attribute 定义后续也可能改名或删除。
- 大字段要控制大小，例如大型模板、证书、密钥不应完整写入历史。
- 快照用于审计和排障，不应该作为重试时的唯一数据源；重试策略需要明确是使用历史快照还是使用当前配置。

落库方式建议二选一：

| 方案                          | 说明                                                     |
|-------------------------------|----------------------------------------------------------|
| 新增 `config_snapshot JSONB` 字段 | 语义最清晰，查询和展示直接围绕快照字段                     |
| 放入现有扩展字段               | 改表较少，但语义不如独立字段清楚；如果当前表无扩展字段仍需加字段 |

初版推荐新增 `config_snapshot JSONB`，并在 Command Detail 页面展示“执行时配置快照”折叠区。
