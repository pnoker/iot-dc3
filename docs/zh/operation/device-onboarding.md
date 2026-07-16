---
title: 设备接入
---

<script setup>
import DeviceOnboardingSelectDiagram from '../../.vitepress/theme/components/DeviceOnboardingSelectDiagram.vue'
import DeviceOnboardingFlowDiagram from '../../.vitepress/theme/components/DeviceOnboardingFlowDiagram.vue'
</script>


# 设备接入

把一台现场设备接入 IoT DC3，本质是五步：按协议选一个驱动 → 建模板与位号 → 建设备并绑定模板和驱动 →
为设备填位号属性的具体值 → 启用后确认设备在线、数据可查。读完这页，你能用内置的 `dc3-driver-virtual`
跑通一次完整接入，并把同样的步骤套到真实协议上。

> 你在这里：已了解 [核心概念](../introduction/concepts)（驱动/模板/设备/位号、三层配置），现在动手接入第一台设备。更快的"
> 复制粘贴"上手版见 [第一个设备](../quickstart/first-device)。

## 先决定：用哪个驱动

接入的第一个决策是**按设备说的协议挑驱动**。驱动（Driver / `dc3-driver-*`）是协议适配实例——它知道怎么和某一类设备通信，并把"
这类设备/位号需要哪些配置项"注册到管理中心。选错协议，后面的模板和位号都对不上。

平台内置 28 个驱动，覆盖工业现场总线、IoT 无线、数据库桥接和基础通信。下面这张图按协议把常见选择收敛到一个驱动模块：

<DeviceOnboardingSelectDiagram lang="zh" />

::: tip 选不准时的两条经验

- **先跑虚拟驱动**：`dc3-driver-virtual` 会按配置生成合成值，不依赖任何真实设备，是验证"模板→设备→位号→数据可查"
  整条链路最快的方式，也是写新驱动的模板工程。
- **数据方向决定模式**：平台主动去"读"设备（轮询采集）用 `dc3-driver-virtual` 这类常规驱动；外部系统主动往平台"推"数据，用反向监听的
  `dc3-driver-listening-virtual`（TCP `6270` / UDP `6271`）。

:::

完整的 28 个驱动清单（工业/IoT/数据库/计量/仿真分类）见 [驱动开发](../development/driver-authoring) 与模块地图。下文用
`dc3-driver-virtual` 走一遍真实接入。

## 接入的数据走向

理解一台设备"接入成功"意味着什么，要先看它产生的值怎么流到可查询的地方。设备侧的原始值由驱动按协议读出、归一成 `PointValue`
，经 RabbitMQ 进数据中心（Data Center / `dc3-center-data`）落库，最终通过网关（Gateway / `dc3-gateway`，对外唯一入口，端口
`8000`）对外可查。

<DeviceOnboardingFlowDiagram lang="zh" />

所以"接入成功"的判据不是"驱动启动了"，而是**这条链路打通、能在数据中心查到这台设备最新的位号值**。这条数据平面的交换机、队列、TTL
等细节见 [数据与命令](./data-commands)。

## 第 0 步：起栈与驱动注册

接入前先确保依赖与中心服务就绪，并启动目标驱动：

- 已启动 PostgreSQL、RabbitMQ 与核心中心服务（鉴权/管理/数据中心）。
- 本地源码运行时，先加载环境变量：`source dc3/env/dev.env.sh`（让本地 Java 进程指向 Compose 暴露在 `localhost`
  的服务，详见 [环境变量](../quickstart/environment)）。
- 启动至少一个驱动，例如虚拟驱动。

```bash
java -jar dc3-driver/dc3-driver-virtual/target/dc3-driver-virtual.jar
```

驱动启动时，`DriverInitRunner` 会编排 **注册 → `initial()` → `schedule()`** 三步：通过 gRPC 向管理中心提交 `RegisterBO`
，带上驱动编码、名称、服务信息、租户，以及它声明的全部属性定义（attribute）。注册失败会按指数退避自动重试（2–30 秒，最多 30 次）。

::: danger `dc3.driver.code` 是稳定路由标识

驱动编码 `dc3.driver.code` 是消息路由与设备归属的稳定标识，**注册后不可随意更改**（改了等于换了一个驱动，已绑定的设备会失联）。每个驱动实例的
code 必须唯一且稳定。

:::

如果驱动迟迟没出现在驱动列表，按这张表排查注册失败的常见原因：

| 现象                         | 处理方式                                 |
|----------------------------|--------------------------------------|
| 管理中心未启动                    | 先启动 Manager Center，再重启驱动             |
| `CENTER_MANAGER_HOST` 指向错误 | 检查 `dc3/env/dev.env(.sh)` 或 IDE 环境变量 |
| 驱动编码重复                     | 保持 `dc3.driver.code` 唯一且稳定           |
| RabbitMQ 未就绪               | 等健康检查通过后重启驱动                         |

## 第 1–4 步：黄金路径接入

下面用网关 HTTP 接口走一遍。所有写接口都经网关 `:8000` 转发，受保护接口需带鉴权头 `X-Auth-Tenant` / `X-Auth-Login` /
`X-Auth-Token`（先 `POST /api/v3/auth/token/salt` 取盐，再 `POST /api/v3/auth/token/generate` 取 token，有效期 12
小时，详见黄金路径登录流程）。以下 `$TOKEN` 即登录拿到的访问令牌；示例里的 ID、名称为示例值。

### 1. 建模板（Profile）

为同类设备建一个能力模板。模板沉淀"这类设备有哪些位号、命令、事件"，设备复用它即可。

```bash
curl -X POST http://localhost:8000/api/v3/manager/profile/add \
  -H "X-Auth-Tenant: default" -H "X-Auth-Login: dc3" -H "X-Auth-Token: $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"profileName":"virtual-motor","profileShareFlag":"TENANT","enableFlag":true}'
# 响应：R.ok(SuccessCode.ADD)，即 "Added successfully"；add 不回传新建实体 id
# 后续步骤需要 profileId 时，调 POST /api/v3/manager/profile/list 按 profileName 查回
```

`profileShareFlag` 取 `ProfileShareTypeEnum`（`TENANT` / `DRIVER` / `USER`），决定模板的共享范围。

### 2. 在模板下建位号（Point）

位号（Point）是一个数据项。关键字段是数据类型 `pointTypeFlag` 与读写方向 `rwFlag`——某个位号能不能写**由它自己的 `rwFlag` 决定
**，不是命令表决定。可选的 `baseValue` / `multiple` 把原始值线性换算成工程值，`unit` 标单位。

```bash
curl -X POST http://localhost:8000/api/v3/manager/point/add \
  -H "X-Auth-Tenant: default" -H "X-Auth-Login: dc3" -H "X-Auth-Token: $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"pointName":"temperature","pointTypeFlag":"DOUBLE","rwFlag":"READ_WRITE",
       "profileId":"<上一步的 profileId>","valueDecimal":2,"unit":"celsius","enableFlag":true}'
# 响应：R.ok(SuccessCode.ADD)；add 不回传 id，需要 pointId 时调 /api/v3/manager/point/list 按 pointName 查回
```

`pointTypeFlag` 取 `PointTypeEnum`（`STRING` / `BYTE` / `SHORT` / `INT` / `LONG` / `FLOAT` / `DOUBLE` / `BOOLEAN`，共 8
种）；`rwFlag` 取 `RwTypeEnum`（`READ_ONLY` / `WRITE_ONLY` / `READ_WRITE`）。

### 3. 建设备（Device）并绑定模板与驱动

设备（Device）是现场一台具体设备的平台镜像，它**绑定一个模板**（决定有哪些位号）**和一个驱动**（决定怎么通信）。

```bash
curl -X POST http://localhost:8000/api/v3/manager/device/add \
  -H "X-Auth-Tenant: default" -H "X-Auth-Login: dc3" -H "X-Auth-Token: $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"deviceName":"motor-01","driverId":"<virtual 驱动 id>",
       "profileId":"<profileId>","enableFlag":true}'
# 响应：R.ok(SuccessCode.ADD)；add 不回传 id，需要 deviceId 时调 /api/v3/manager/device/list 按 deviceName 查回
```

设备创建后，驱动通过元数据事件（`DriverMetadataListener.event(...)` 收 ADD/UPDATE/DELETE）感知变更并刷新缓存，多数情况无需重启驱动。

### 4. 为设备配置位号属性（Attribute 的 Config 值）

这一步最容易混淆，必须区分两个概念：

::: info Attribute 是驱动注册的"有哪些"，Config 是设备实例填的"具体值"

- **属性 Attribute**（`PointAttribute` / `DriverAttribute` 等）：**驱动启动时**从自己的 `application.yml`
  注册的协议层配置项——它声明"这个驱动的位号**需要**哪些配置项"（如 Modbus 的寄存器地址、virtual 的取值范围）。你不创建
  attribute，它随驱动注册而来。
- **配置 Config**（`PointAttributeConfigDO` 等）：**这台设备**给上述每个 attribute 填的**具体值**——如"motor-01 的
  temperature 位号，寄存器地址填 40001"。这一步就是在填 Config。

三层配置（业务层 Param / 协议层 Attribute / 实例层 Config）的完整说明见 [核心概念](../introduction/concepts)。

:::

为设备上某个位号的某个属性写入实例值，调用 `POST /api/v3/manager/point_attribute_config/add`：

```bash
curl -X POST http://localhost:8000/api/v3/manager/point_attribute_config/add \
  -H "X-Auth-Tenant: default" -H "X-Auth-Login: dc3" -H "X-Auth-Token: $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"attributeId":"<驱动注册的位号属性 id>","deviceId":"<deviceId>",
       "pointId":"<pointId>","configValue":"40001","enableFlag":true}'
# 响应：R.ok(SuccessCode.ADD)；add 统一返回成功码，不回传新建记录 id
```

`attributeId` 来自驱动注册的属性列表；`configValue` 是这台设备实例填的值。每个驱动声明的属性集不同——virtual
驱动声明的是取值范围之类的合成参数，Modbus 驱动声明的是寄存器/地址之类的协议参数。

::: tip 用真实现场参数校准位号

对工业协议，重点核对：寄存器/地址/对象 ID/topic 是否正确、数据类型与字节序、倍率与单位是否匹配现场、读写方向是否与设备能力一致、采集周期是否与设备性能匹配。这些都落在
`configValue` 上。

:::

## 第 5 步：启用后确认设备在线、数据可查

接入的终点是确认链路打通。启用设备后等待一个采集周期，按"状态 → 数据 → 日志"的顺序确认。

**先看数据**——能查到最新位号值，就说明整条链路通了。经网关查这台设备的最新值：

::: code-group

```bash [curl]
curl -X POST http://localhost:8000/api/v3/data/point_value/latest \
  -H "X-Auth-Tenant: default" -H "X-Auth-Login: dc3" -H "X-Auth-Token: $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"deviceId":"<deviceId>","current":1,"size":20}'
```

```json [响应形态 (示例值)]
{
  "data": {
    "current": 1, "size": 20, "total": 1,
    "records": [
      {
        "deviceId": "...", "pointId": "...", "driverId": "...", "tenantId": "...",
        "rawValue": "23.71", "calValue": "23.71", "numValue": 23.71,
        "hasLatestValue": true,
        "createTime": "2026-06-22T08:30:00", "operateTime": "2026-06-22T08:30:00"
      }
    ]
  }
}
```

:::

`POST /api/v3/data/point_value/latest` 返回 `Page<PointValueVO>`（每条含 `deviceId` / `pointId` / `driverId` /
`tenantId` / `rawValue` 原始值 / `calValue` 工程值 / `numValue` 数值投影（可空）/ `hasLatestValue` / `createTime` /
`operateTime`，时间为本地日期时间）；要按时间窗翻历史值用 `POST /api/v3/data/point_value/list`
。完整的读写命令链路见 [数据与命令](./data-commands)。

**没数据时，按这条链路反向排查**，每一跳都对应上面那张数据走向图：

1. **驱动状态**：驱动是否在线？设备健康状态是否 `ONLINE`？注意驱动上报的状态 TTL **必须大于读取周期**（如 30 秒 cron，TTL 至少
   25 秒），否则设备会反复掉线。
2. **驱动日志**：有没有协议连接错误（连不上 host/port、寄存器越界、认证失败）。
3. **RabbitMQ**：队列是否有积压或绑定异常，说明驱动发出去了但数据中心没消费。
4. **数据中心**：是否收到该设备的位号值消息。
5. **租户一致性**：设备、模板、位号、属性配置的 `tenantId` 是否一致；跨租户访问会返回 404 而非数据。
6. **属性配置**：`configValue` 是否缺失或格式不符合驱动期望（如地址填了非法字符串）。

::: warning 设备一直显示离线？先查状态 TTL

最常见的"启用了却查不到值"是状态 TTL 配小了：驱动按读取周期上报心跳，TTL 短于周期就会在两次上报之间过期，设备被判离线。把
TTL 配成略大于读取周期即可。

:::

跑通虚拟驱动这一遍后，把同样的五步套到真实协议上：唯一变化的是第 0 步选的驱动模块、以及第 4 步每个驱动声明的属性集不同。

## 延伸阅读

- [第一个设备](../quickstart/first-device) — 更短的复制粘贴上手版，先跑通再回来细读
- [数据与命令](./data-commands) — 接入后如何查历史值、下发读写命令与回执
- [核心概念](../introduction/concepts) — 驱动/模板/设备/位号与三层配置（Param/Attribute/Config）的心智模型
- [驱动开发](../development/driver-authoring) — 28 个驱动的清单、SPI 契约，以及从 `dc3-driver-virtual` 写一个新协议驱动
