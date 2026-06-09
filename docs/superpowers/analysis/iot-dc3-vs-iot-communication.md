# IoT DC3 vs iot-communication 对比分析报告

> 生成日期: 2026-05-26
>
> 分析目标: 以 iot-communication 为参考，查找 IoT DC3 需要借鉴和补齐的地方。
>
> 当前状态: 该文档是历史分析材料，已迁移到 `Superpowers`。截至当前仓库目录，IoT DC3 已包含 28 个接入驱动模块，其中包括
> `dc3-driver-melsec`、`dc3-driver-sl651`、`dc3-driver-modbus-rtu` 等后续补齐项。当前能力矩阵以
> [模块清单](../../modules/) 为准，本文保留协议实现质量、协议核心拆分和测试建设方面的参考价值。

---

## 一、项目定位对比

| 维度       | IoT DC3                                                               | iot-communication             |
|----------|-----------------------------------------------------------------------|-------------------------------|
| **定位**   | 分布式工业物联网平台                                                            | 物联网通信协议工具库                    |
| **目标用户** | 企业级部署与运维人员                                                            | Java 开发者（协议集成）                |
| **规模**   | ~1,582 Java 文件, 7 个顶层模块                                               | ~341 Java 文件, 单 jar           |
| **代码量**  | 大型项目 (微服务拆分明细)                                                        | 46,332 行                      |
| **技术栈**  | Java 21 / Spring Boot 4 / Spring Cloud / gRPC / RabbitMQ / PostgreSQL | Java 8 / 零框架 / 仅 Lombok+SLF4J |
| **部署形态** | Docker/Podman Compose, 多服务编排                                          | Maven 依赖引入, 嵌入式               |
| **许可**   | AGPL v3                                                               | MIT                           |

**结论**: 两者定位完全不同。iot-dc3 是**平台级产品**，iot-communication 是**协议级工具库**。本次对比重点在于: iot-dc3 可以从
iot-communication 借鉴哪些协议实现和工具设计，补齐自身的协议层短板。

---

## 二、协议支持对比

### 2.1 工业 PLC 协议

| 协议                    | IoT DC3                                         | iot-communication                         | 差距分析                                                                                                                                                     |
|-----------------------|-------------------------------------------------|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| **西门子 S7**            | ✅ dc3-driver-plcs7 (基于 libnodave C 端口移植)        | ✅ 自研纯 Java 实现 (1455行 S7PLC.java)          | **关键差距**: iot-dc3 的 S7 实现依赖 nodave C 口移植（仅 391 行 Nodave.java），是第三方库的薄封装。iot-communication 是完整的自研实现，支持 S1500/S1200/S400/S300/S200Smart/**828D 数控机床**，覆盖更广 |
| **Modbus TCP**        | ✅ dc3-driver-modbus-tcp (基于 Serotonin modbus4j) | ✅ 自研实现 (168行 ModbusTcp.java)              | IoT DC3 使用 modbus4j（完整的工业级库，支持 TCP/RTU/ASCII/UDP），iot-communication 自研实现更轻量但功能更精简                                                                        |
| **Modbus RTU/ASCII**  | ✅ 已有 `dc3-driver-modbus-rtu`；ASCII / Server 模式需按代码能力另行确认 | ✅ 支持 ModbusRtuOverTcp, ModbusAsciiOverTcp | 历史缺口已部分补齐；后续重点是明确 RTU、ASCII、Server 三类模式的边界与测试覆盖                                                                                                                |
| **Modbus TCP Server** | 当前模块清单未体现独立 Server 模式                    | ✅                                         | 可作为设备模拟/从站模式的后续增强                                                                                                                                       |
| **三菱 Melsec (MC)**    | ✅ 已有 `dc3-driver-melsec`                         | ✅ 自研实现 (iQ-R, Q/L, QnA, A 系列)             | 历史缺口已补齐；后续应补充协议能力矩阵和测试覆盖                                                                                                                   |

### 2.2 视频/流媒体协议

| 协议             | IoT DC3 | iot-communication                | 差距分析                     |
|----------------|---------|----------------------------------|--------------------------|
| **RTSP**       | ❌       | ✅                                | **完整缺失**: 视频监控是工业物联网核心场景 |
| **RTCP / RTP** | ❌       | ✅                                | 同上                       |
| **H264 解析**    | ❌       | ✅ (NAL unit, FU-A, MTAP24)       | 同上                       |
| **MP4/FMP4**   | ❌       | ✅ (FMP4 + WebSocket + MSE + WEB) | 同上                       |

### 2.3 行业专用协议

| 协议                    | IoT DC3             | iot-communication | 差距分析                   |
|-----------------------|---------------------|-------------------|------------------------|
| **SL651-2014** (水文监测) | ✅ `dc3-driver-sl651` | ✅                 | 历史缺口已补齐；后续应补充采集场景、报文样例和集成测试 |
| **OPC UA**            | ✅ dc3-driver-opc-ua | ❌                 | iot-dc3 已领先            |
| **OPC DA**            | ✅ dc3-driver-opc-da | ❌                 | iot-dc3 已领先            |
| **MQTT**              | ✅ dc3-driver-mqtt   | ❌                 | iot-dc3 已领先            |
| **CoAP**              | ✅ dc3-driver-coap   | ❌                 | iot-dc3 已领先            |

### 2.4 协议覆盖雷达图（定性）

```
iot-dc3 有而 iot-communication 无:  OPC UA/DA, MQTT, CoAP, LwM2M, HTTP, BLE, Zigbee, database drivers
iot-communication 有而 iot-dc3 当前模块清单未体现: RTSP/RTP/RTCP/H264/MP4, Modbus Server, Modbus ASCII
两者皆有但实现方式不同:              S7, Modbus TCP, Modbus RTU, Melsec MC, SL651
```

---

## 三、协议实现质量对比（以 S7 为例）

### iot-communication 的 S7 实现优势

1. **纯 Java 自研**: 完整的 S7 协议栈 (1455 行核心 + 完整的 model/enum/service/algorithm 分层)
2. **西门子型号覆盖面广**: S1500/S1200/S400/S300/S200Smart/828D 数控机床
3. **地址解析工具**: `AddressUtil` 自动解析 DB/M/I/Q 等不同存储区地址格式
4. **多区域读写支持**: DataItem, RequestItem, RequestNckItem, S7Data 完整数据模型
5. **连接管理**: PLCNetwork 基类提供 TCP 连接池和生命周期管理
6. **序列化框架**: 通过 `ByteArraySerializer` 实现声明式注解驱动的数据结构与 PLC 内存映射

### iot-dc3 的 S7 实现

1. **依赖 libnodave C 端口**: nodave/Nodave.java 仅 391 行，是 C 库 libnodave 的 Java 翻译
2. **覆盖型号有限**: 文档未列出具体支持的 PLC 型号
3. **有自研的注解驱动序列化**: `@S7Variable`, `@Datablock`, `@Array` 注解 + `S7SerializerImpl` 是其亮点
4. **集成在 Driver 框架**: 天然具备设备注册/点位管理/数据上报/命令下发能力（这是 iot-communication 不具备的平台层能力）

### 建议

> **iot-dc3 的注解驱动序列化 (`@S7Variable`) + iot-communication 的完整协议栈实现 = 最优方案。**
>
> 具体而言，iot-dc3 应将 iot-communication 的 S7 协议实现作为底层引擎替换当前的 nodave 方案，同时保留现有的注解序列化和
> Driver 框架集成。

---

## 四、工具类/基础能力对比

### 4.1 字节缓冲区工具

| 工具    | IoT DC3    | iot-communication                |
|-------|------------|----------------------------------|
| 字节读缓冲 | 基础 ByteBuf | `ByteReadBuff` — 支持偏移追踪、多种字节序    |
| 字节写缓冲 | 基础 ByteBuf | `ByteWriteBuff` — 支持链式写入         |
| 位写缓冲  | 无          | `BitWriteBuff` — 按位写入            |
| 字节序格式 | 基础         | `EByteBuffFormat` — 4/8 字节编码格式枚举 |

**建议**: iot-dc3 参考 iot-communication 的 `ByteReadBuff`/`ByteWriteBuff` 设计，在 `dc3-common` 中构建一套更易用的字节缓冲区工具。

### 4.2 数据类型转换工具

iot-communication 提供了丰富的工具类，iot-dc3 也有自己的转换逻辑但部分可以借鉴:

| 工具            | iot-communication | 建议                |
|---------------|-------------------|-------------------|
| `BCDUtil`     | BCD 码编解码          | 在 PLC 通信中常用，建议纳入  |
| `CRCUtil`     | 多种 CRC 校验         | 已存在部分，可增强         |
| `LRCUtil`     | LRC 纵向冗余校验        | Modbus ASCII 场景需要 |
| `FloatUtil`   | 浮点数与字节互转          | 已存在类似能力           |
| `BooleanUtil` | 布尔值与字节互转          | 按位读写点位的场景需要       |
| `HexUtil`     | 十六进制字符串转换         | 基础工具，建议检查是否有缺失    |
| `TimesUtil`   | S7/Melsec 时间格式    | 工业协议时间格式转换专用      |

### 4.3 注解驱动的序列化框架

iot-communication 的 `ByteArraySerializer` 提供了一套强大的注解驱动序列化框架:

```java
// iot-communication 模式示例
@ByteArrayVariable(byteOffset = 0, bitOffset = 0, count = 1, type = EDataType.BOOL)
boolean running;
@ByteArrayVariable(byteOffset = 2, count = 1, type = EDataType.UINT16)
int speed;
```

iot-dc3 的 `@S7Variable` 注解有类似思路但仅限于 S7 驱动。**建议将字节序列化框架从 S7
驱动中分离出来，提升为通用的 `dc3-common` 工具层**，供所有协议驱动复用。

---

## 五、架构差异与互鉴

### 5.1 iot-dc3 的平台层优势（iot-communication 不具备）

| 能力         | 说明                        |
|------------|---------------------------|
| 多租户体系      | 租户隔离、RBAC 权限、资源管理         |
| 设备/点位/模板管理 | 完整的元数据管理和 CRUD            |
| 数据采集与存储    | 时序数据入库、查询、聚合              |
| 命令下发       | 设备命令的创建、路由、执行、追踪          |
| 驱动注册与调度    | 驱动的自动注册、健康检查、生命周期管理       |
| 事件告警       | 设备告警、状态变更事件               |
| gRPC 通信    | 高性能的跨服务通信                 |
| 网关层        | Spring Cloud Gateway 统一入口 |
| AI Agentic | AI 辅助运维                   |

### 5.2 iot-communication 的设计模式值得借鉴的地方

#### a) 协议引擎独立于框架

iot-communication 将协议栈实现为**零框架依赖的纯 Java 库**。这使得它可以嵌入任何 Java 应用中。iot-dc3 的协议实现目前与
Spring Boot/Driver 框架强绑定，无法独立复用。

**建议**: 将每个协议的编解码核心抽取为独立模块（如 `dc3-protocol-s7-core`, `dc3-protocol-modbus-core`），只依赖
SLF4J，使协议栈可独立发布和复用。

#### b) 协议模型的层次化设计

iot-communication 的代码组织清晰规范:

```
protocol/
├── s7/
│   ├── model/      ← 数据模型 (PDU, DataItem, S7Data...)
│   ├── enums/      ← 枚举定义 (EPlcType, EMessageType...)
│   ├── service/    ← 服务层 (S7PLC, PLCNetwork...)
│   ├── utils/      ← 工具 (AddressUtil)
│   └── serializer/ ← 序列化器
├── modbus/
│   ├── model/
│   ├── enums/
│   └── service/
└── ...
```

**建议**: iot-dc3 的驱动代码内部可参照此层次化组织，区分协议模型层和服务适配层。

#### c) 完整的测试覆盖

iot-communication 虽然项目较小但有 133 个测试文件，测试覆盖率高。iot-dc3 的驱动模块测试相对薄弱（Modbus TCP 驱动仅 1
个测试文件）。

**建议**: 对新引入的协议模块要求达到 >70% 单元测试覆盖率。

---

## 六、当前建议清单（按优先级）

### P0 — 仍需明确或增强的关键能力

| 序号 | 补齐项                 | 说明                                       | 建议方案                                                 |
|----|---------------------|------------------------------------------|------------------------------------------------------|
| 1  | **S7 协议栈评估**        | 当前分析认为 S7 实现深度仍需专项评估 | 对 `dc3-driver-plcs7` 建立协议能力矩阵、兼容型号列表和回归测试 |
| 2  | **视频流媒体能力**         | 当前模块清单未体现 RTSP/RTP/H264 能力 | 评估新增 `dc3-driver-rtsp` 或独立 `dc3-media` 模块 |
| 3  | **Modbus 模式边界** | 已有 TCP 与 RTU 驱动，但 ASCII / Server 模式需要明确 | 补充 Modbus 能力矩阵、测试报文和文档 |

### P1 — 已补齐项的工程完善

| 序号 | 补齐项                  | 说明                                    | 建议方案                           |
|----|----------------------|---------------------------------------|--------------------------------|
| 4  | **MELSEC 驱动文档和测试** | `dc3-driver-melsec` 已存在，需要确认型号覆盖、地址格式和错误处理 | 补充驱动 README、报文 fixture 和集成测试 |
| 5  | **SL651 驱动文档和测试** | `dc3-driver-sl651` 已存在，需要沉淀水文场景样例 | 补充报文样例、解析说明和回归测试 |
| 6  | **字节缓冲区工具**          | 协议驱动仍需要通用 ByteReadBuff / ByteWriteBuff 级别工具 | 在 `dc3-common` 中评估通用字节处理工具       |
| 7  | **通用序列化框架**          | 注解驱动的字节序列化可以从单协议能力扩展为通用能力 | 评估抽取 `dc3-common-serializer` 或协议核心模块 |

### P2 — 长期规划（生态建设）

| 序号 | 补齐项                       | 说明                       | 建议方案                                                           |
|----|---------------------------|--------------------------|----------------------------------------------------------------|
| 8  | **协议引擎独立化**               | 协议栈与 Spring Driver 框架强绑定，不利于独立复用 | 拆分协议核心模块为独立 jar |
| 9  | **FMP4 + WebSocket 视频播放** | Web 端视频监控 | 参考 RTSP + H264 + FMP4 + WebSocket + MSE 方案 |
| 10 | **更多 PLC / CNC 协议** | 松下、基恩士、Fanuc、Heidenhain 等场景需要按实际需求评估 | 先做需求验证，再进入驱动路线图 |

---

## 七、总结

| 维度         | IoT DC3 优势                      | iot-communication 优势 | 行动项                 |
|------------|---------------------------------|----------------------|---------------------|
| **平台能力**   | 平台化能力完整，覆盖微服务、多租户、RBAC、数据管理和 AI | 不适用 | 继续深化平台能力 |
| **S7 协议**  | 已有驱动，协议实现深度和型号覆盖需要专项验证 | 有完整自研协议栈 | 建立能力矩阵和测试基线 |
| **Modbus** | 已有 TCP 与 RTU 驱动 | 多模式实现可参考 | 明确 ASCII / Server 边界 |
| **三菱 PLC** | 已有 `dc3-driver-melsec` | 有 MC 协议实现经验 | 补文档、样例和测试 |
| **视频流媒体** | 当前模块清单未体现 | 有 RTSP/RTP/H264/MP4 参考实现 | 单独评估媒体模块 |
| **行业协议** | 已有 `dc3-driver-sl651` | 有 SL651-2014 参考实现 | 补场景样例和测试 |
| **工具库** | 基础能力可用 | 字节缓冲和序列化工具更集中 | 抽取通用协议工具 |
| **测试** | 驱动测试仍需加强 | 测试文件较多 | 提升协议 fixture 和回归测试 |
| **架构** | 平台化/驱动框架清晰 | 协议与框架解耦 | 评估协议核心模块拆分 |

**一句话**: 这份历史分析的主要价值已经从“补齐缺失驱动”转为“提升协议实现深度、文档清晰度、测试覆盖和协议核心可复用性”。
