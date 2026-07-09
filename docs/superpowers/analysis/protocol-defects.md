# 协议层代码缺陷分析（三分类 + 学术依据）

> **生成日期**：2026-06-30
>
> **底座来源**：复用 `iot-dc3-vs-iot-communication.md`（2026-05-26）的协议层结论，逐条回读当前源码核验；未重复造轮子。
>
> **核验范围**：
> - 6 个骨架驱动主类：`dc3-driver-{mqtt,lwm2m,iec104,dlms,ethernet-ip,can}` 的 `*DriverCustomServiceImpl.java`
> - 3 个可用驱动核心实现：`dc3-driver-{modbus-tcp,opc-ua,plcs7}`
> - 学术依据：`protocol-knowledge-entries.md` 已核条目
>
> **三分类定义**（避免书生气误判）：
> - 【真缺陷】违反最佳实践且有改进空间（静默吞异常、伪造成功、错误不退避、资源未关闭、线程安全）。
> - 【有意简化】工程取舍，记录不判错（如骨架驱动 `read()` 显式 throw NotImplemented 快速失败——诚实设计）。
> - 【技术债】已知欠账（CIP 组帧未补、CAN 字节切分未齐、测试覆盖不足、某驱动仅支持 16 位类型）。
>
> **核验方式**：每条 `文件:行` 均由独立 verify 子 agent 回读源码核对（见文末 verify 记录）。

---

## 一、既有 analysis 核验结果

历史报告（2026-05-26）6 条协议层结论，回读当前源码后的状态：

| # | 历史结论                                        | 当前状态     | 核验依据                                                                                                                                                                                                          |
|---|---------------------------------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | S7 基于 libnodave C 移植（Nodave.java ~391 行）    | **已变化**  | 全仓 `find -iname "*nodave*"` 返回 0；plcs7 main 仅 3 个 java 文件。已改为依赖第三方 `com.github.xingshuangs.iot.protocol.s7.S7PLC`（`PlcS7DriverCustomServiceImpl.java:20-21`）。libnodave 移植层已移除。                                |
| 2 | `@S7Variable`/`S7SerializerImpl` 注解序列化仅限 S7 | **已变化**  | `grep -rl "@S7Variable"` 仅命中本历史报告自身，无任何 .java 使用；`S7SerializerImpl.java` 全仓不存在。注解序列化体系随 libnodave 一并移除。                                                                                                       |
| 3 | 协议引擎与 Spring 强绑定                            | **部分成立** | 驱动服务类仍 `@Service`（`ModbusTcpDriverCustomServiceImpl.java:71`、`OpcUaDriverCustomServiceImpl.java:76`）。但仓库内已无独立"协议编解码类"可判定 POJO/Spring 归属——编解码下沉到外部依赖（modbus4j / Eclipse Milo / xingshuangs iot-communication）。 |
| 4 | 字节缓冲工具缺失（无 ByteReadBuff/ByteWriteBuff）      | **成立**   | `dc3-common` 下 `find -name "*Byte*.java"` 无此类（仅 `WindowSampleBuffer.java` 属告警采样）。                                                                                                                             |
| 5 | Modbus 模式边界（无 ASCII/Server 模块）              | **成立**   | `ls dc3-driver/` modbus 相关仅 `dc3-driver-modbus-rtu`、`dc3-driver-modbus-tcp`。                                                                                                                                  |
| 6 | 驱动测试薄弱（modbus-tcp 仅 1 测试文件）                 | **成立**   | modbus-tcp/plcs7/opc-ua 各 1 个测试文件。                                                                                                                                                                            |

**小结**：6 条中 3 条仍成立（4/5/6），1 条部分成立（3），2 条已变化（1/2）。历史报告最大的认知偏差：**plcs7 已不再"薄封装
libnodave"，而是直接依赖对比对象 iot-communication 本身**（`com.github.xingshuangs.iot` 即 iot-communication 的 Maven
坐标）——这把"S7 深度不足"的旧结论部分化解了，但也引入了"驱动编解码全靠外部库、仓库内无自有协议栈"的新观察。

---

## 二、骨架驱动完成度表（增量核查）

6 个标注 "work-in-progress skeleton" 的驱动主类，逐个实读 `read()/write()/health()`：

| 驱动          | 主类（行数）                                  | read() 行:状态                           | write() 行:状态                  | health() 行:状态                        | 编解码核心                                                  | 完成度评级       |
|-------------|-----------------------------------------|---------------------------------------|-------------------------------|--------------------------------------|--------------------------------------------------------|-------------|
| mqtt        | MqttDriverCustomServiceImpl (312)       | 172-184: return null（pub/sub 设计）      | 202-229: 完整实现（QoS 处理）         | 116-119: 桩（恒 online + TODO）          | N/A（payload 透传）                                        | 写可用 / 健康桩   |
| lwm2m       | Lwm2mDriverCustomServiceImpl (236)      | 133-157: 完整（委托 ServerManager）         | 159-177: 完整                   | 97-115: 完整（驱动+设备两级）                  | 委托 Leshan                                              | **最完整**     |
| iec104      | Iec104DriverCustomServiceImpl (176)     | 113-120: throw NotImplemented         | 122-129: throw NotImplemented | 未声明（用 SDK 默认）                        | 无（无 APDU/ASDU 代码）                                      | 纯骨架         |
| dlms        | DlmsDriverCustomServiceImpl (173)       | 132-140: throw NotImplemented         | 142-149: throw NotImplemented | 100-108: 查 clientMap 但从不填充→恒 offline | 无（import GXDLMSClient 未用）                              | 纯骨架         |
| ethernet-ip | EthernetIpDriverCustomServiceImpl (389) | 159-179: 部分实现（发 CIP Read Tag）         | 181-197: 部分实现                 | 122-131: 实现（socket 状态）               | 部分组帧：服务码齐全但封装头仅写 2/24 字节，缺 RegisterSession/ForwardOpen | 编解码最成形但仍不可用 |
| can         | CanDriverCustomServiceImpl (274)        | 149-173: 实现（ProcessBuilder 调 candump） | 175-192: 实现（cansend）          | 110-127: 实现（ip link show）            | 无原生组帧，纯 shell 调 can-utils                              | 功能可用但每调用起进程 |

---

## 三、缺陷清单（三分类）

字段：`编号 | 文件:行 | 协议/驱动 | 对照(书/标准/竞品) | 三分类 | 描述 | 建议`

### A. 【真缺陷】（4 条）

> ✅ **修复状态（2026-07-01）**：D1-D4 全部已在 `feature/protocol-defect-fixes` 分支修复并通过测试（dc3-driver-mqtt
> 16/16、dc3-driver-opc-ua 16/16，BUILD SUCCESS）。
>
> | 缺陷 | 修复 commit | 修复要点 |
> |------|------------|---------|
> | D1 OPC-UA 连接风暴 | `63fdf962d` | 镜像 modbus-tcp `failureMap`+`ConsecutiveFailure`（阈值3/60s）+ health invalidate 坏 client |
> | D2 OPC-UA 静默降级 | `2d78b55` | `certificateDegraded` 标记 + health description 暴露降级 |
> | D3 OPC-UA 证书空设 | `94feac5c6` | 提取 `buildIdentityProvider`，证书分支改用 `X509IdentityProvider`（Milo 签名 cert+PrivateKey） |
> | D4 MQTT health 撒谎 | `f69787eb8` | 事件驱动（`MqttSubscribedEvent`/`MqttConnectionFailedEvent`），health 基于真实连接 |
>
> 详见 `.superpowers/sdd/defect-fix-ledger.md`。B（有意简化）与 C（技术债）未在本轮处理。

| 编号 | 文件:行                                                                          | 协议/驱动  | 对照                                                                                                                                  | 三分类 | 描述                                                                                                                                                                                  | 建议                                                                                    |
|----|-------------------------------------------------------------------------------|--------|-------------------------------------------------------------------------------------------------------------------------------------|-----|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| D1 | `dc3-driver-opc-ua/.../OpcUaDriverCustomServiceImpl.java:135-138` + `187-226` | OPC UA | 同仓库 modbus-tcp 已有 failureMap+ConsecutiveFailure 退避（`ModbusTcpDriverCustomServiceImpl.java:100,275-308`）；OPC UA 规范 Part 4 要求会话生命周期管理 | 真缺陷 | `health()` catch 块仅 `log.debug` 后返回 offline，无失败计数/退避；`getConnector()` 用 `computeIfAbsent` 每周期为不可达主机创建新 `OpcUaClient`。不可达设备被反复 TCP+TLS 握手（连接风暴）。`log.debug` 级别也过低——连接失败是值得 warn 的事件。 | 镜像 modbus-tcp：每设备失败计数器 + 连续 3 次/60s 冷却退避；catch 块日志提级 warn。                            |
| D2 | `dc3-driver-opc-ua/.../OpcUaDriverCustomServiceImpl.java:112-118`             | OPC UA | KeyLoader 失败应 fail-fast 而非静默降级（安全配置缺失应显式暴露）                                                                                         | 真缺陷 | `initial()` 中 KeyLoader 加载失败被 catch 后 `keyLoader = null`，驱动以匿名身份继续启动。需证书的服务器会静默连上但读不到数据，运维只看到一个"正常连接"。配置错误被掩盖。                                                                      | 启动时 fail-fast，或在健康状态持续暴露"证书缺失"降级标记。                                                   |
| D3 | `dc3-driver-opc-ua/.../OpcUaDriverCustomServiceImpl.java:204-213`             | OPC UA | OPC UA 安全模式规范：配了证书就应配对应身份提供者（X509IdentityProvider）                                                                                  | 真缺陷 | `getConnector()` 中已 `setCertificate/setKeyPair` 的分支仍 `setIdentityProvider(new AnonymousProvider())`——证书用于 TLS 握手却不用于应用层身份认证，配置形同虚设（安全假象）。                                           | 配证书分支用 `x509IdentityProvider` 或至少在日志标注"证书仅用于 TLS，身份仍匿名"。                              |
| D4 | `dc3-driver-mqtt/.../MqttDriverCustomServiceImpl.java:116-119`                | MQTT   | 同仓库 modbus-tcp/plcs7 的 health() 都真实探测连接；MQTT 客户端管理器（Spring Integration）有现成 isConnected 接口                                           | 真缺陷 | `health()` 恒返回 `DriverHealthState.online()` + TODO 注释。MQTT broker 不可达时驱动仍自报健康，调度器会继续向其下发写命令并静默失败（write 路径有 try/catch 但上层看不到 broker 已挂）。                                             | 调用 `MqttSendService` 或 Spring Integration MQTT client manager 的 isConnected 实现真实健康检查。 |

> 注：modbus-tcp 与 plcs7 经核查**未发现真缺陷**——modbus-tcp 退避/invalidate/重抛均到位，plcs7 用 ReentrantLock 串行化 +
> close 异常吞掉属良性清理日志。这两个是各自类别的正面范例。

### B. 【有意简化】（5 条）

| 编号 | 文件:行                                                                        | 协议/驱动   | 对照                                                                      | 三分类  | 描述                                                                                                                                                        | 建议                                        |
|----|-----------------------------------------------------------------------------|---------|-------------------------------------------------------------------------|------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| S1 | `dc3-driver-iec104/.../Iec104DriverCustomServiceImpl.java:113-120, 122-129` | IEC 104 | 快速失败优于沉默返回假数据                                                           | 有意简化 | `read()/write()` 显式 `throw ReadPointException/WritePointException("...not implemented...")`。类 javadoc 标 "WORK IN PROGRESS"。这是诚实设计——调用方立即知道能力未实现，不会被假数据误导。 | 维持现状；补全协议 I/O 后移除 throw。                  |
| S2 | `dc3-driver-dlms/.../DlmsDriverCustomServiceImpl.java:132-140, 142-149`     | DLMS    | 同上                                                                      | 有意简化 | `read()/write()` 显式 throw NotImplemented，javadoc 标 transport I/O 待实现。                                                                                     | 维持。                                       |
| S3 | `dc3-driver-mqtt/.../MqttDriverCustomServiceImpl.java:172-184`              | MQTT    | MQTT 是 pub/sub 模型，read 语义本身不适用（数据经 `MqttReceiveHandler` 异步到达）——见学术依据 E1 | 有意简化 | `read()` 返回 null。这不是偷懒，而是协议模型决定的：MQTT 无"主动读"语义。注释有说明。                                                                                                     | 维持；文档中明确"MQTT 驱动 read() 恒为 null，数据走订阅回调"。 |
| S4 | `dc3-driver-iec104/.../Iec104DriverCustomServiceImpl.java`（无 health 声明）     | IEC 104 | —                                                                       | 有意简化 | 未重写 `health()`，继承 SDK 默认。骨架阶段合理（无可探测的连接）。                                                                                                                 | 实现 IEC 104 协议层后补 health。                  |
| S5 | `dc3-driver-dlms/.../DlmsDriverCustomServiceImpl.java:100-108`              | DLMS    | —                                                                       | 有意简化 | `health()` 查 `clientMap.containsKey`，但 `initial()` 中 clientMap 初始化后从不填充（无连接代码）→ 恒返回 offline。骨架阶段无设备能"在线"，调用方看到的是真实状态（offline），未伪造 online。                 | 维持；实现连接后填充 clientMap。                     |

### C. 【技术债】（7 条）

| 编号 | 文件:行                                                                                      | 协议/驱动       | 对照                                                                                           | 三分类 | 描述                                                                                                                                                                                             | 建议                                                 |
|----|-------------------------------------------------------------------------------------------|-------------|----------------------------------------------------------------------------------------------|-----|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------|
| T1 | `dc3-driver-ethernet-ip/.../EthernetIpDriverCustomServiceImpl.java:283-289`               | EtherNet/IP | CIP 规范（ODVA EtherNet/IP Volume 2）：24 字节封装头需完整填充 command/length/session/handle/status/options | 技术债 | `buildEncapsulationHeader` 分配 24 字节但仅 `putShort(commandLen)` 写 2 字节，其余 22 字节为 0。注释自标 "detailed CIP encapsulation framing is TODO"。                                                             | 补全封装头字段 + 实现 RegisterSession/ForwardOpen（见 T2）。    |
| T2 | `dc3-driver-ethernet-ip/.../EthernetIpDriverCustomServiceImpl.java:199-217`（TODO 在 208 行） | EtherNet/IP | CIP 连接建立必须先 RegisterSession 再 ForwardOpen                                                    | 技术债 | 连接建立仅 `new Socket(host,port)` + setSoTimeout，无 RegisterSession/ForwardOpen。read/write 能发 CIP 请求但服务器侧未建立会话，实际不可用。                                                                               | 补 CIP 会话注册与连接打开流程。                                 |
| T3 | `dc3-driver-can/.../CanDriverCustomServiceImpl.java:149-173, 229-244`（javadoc 57-59）      | CAN         | SocketCAN 原生 JNI/系统调用；can-utils 是调试工具非生产通路                                                   | 技术债 | `read()/write()` 通过 `ProcessBuilder` shell 调 `candump`/`cansend`；`readCanFrame` 按空格切 candump 输出末段解析（无多帧/扩展帧/error frame 处理）。每调用起一个进程，延迟与开销高。javadoc TODO 标 "Native SocketCAN JNI integration"。 | 评估 jCAN/jSocketCAN 或直接 JNI 封装 SocketCAN 系统调用。      |
| T4 | `dc3-driver-plcs7/.../PlcS7DriverCustomServiceImpl.java:20-21`                            | S7          | 自研协议栈（iot-communication 的 S7PLC.java 1455 行）vs 直接依赖该库                                        | 技术债 | plcs7 直接 `import com.github.xingshuangs.iot.protocol.s7.service.S7PLC`——即依赖对比对象 iot-communication 本身。驱动层无自有 S7 编解码，升级/patch 受制于上游。                                                             | 评估：长期是继续依赖上游，还是 fork/内化关键编解码以便定制（如 PDU 大小协商、型号适配）。 |
| T5 | 各驱动 `src/test/`（modbus-tcp/plcs7/opc-ua 各 1 文件；6 骨架驱动测试几乎为空）                              | 全协议         | iot-communication 133 个测试文件；工业驱动应 >70% 覆盖                                                    | 技术债 | 测试覆盖薄弱（既有 analysis 结论 6 仍成立）。骨架驱动无 fixture、无报文回归。                                                                                                                                              | 对可用驱动补报文 fixture 与集成测试（Testcontainers 模式已有先例）。     |
| T6 | `dc3-driver/`（无 modbus-ascii / modbus-server 独立模块）                                        | Modbus      | Modbus 协议规范定义 TCP/RTU/ASCII 三种传输 + Client/Server 角色                                          | 技术债 | 仅有 modbus-tcp / modbus-rtu，缺 ASCII 传输与 Server（从站模拟）模式（既有 analysis 结论 5 仍成立）。                                                                                                                   | 按实际需求评估 ASCII 模块；Server 模式可用于设备模拟。                 |
| T7 | `dc3-common/`（无 ByteReadBuff/ByteWriteBuff/BitWriteBuff）                                  | 通用          | iot-communication 的 ByteReadBuff 支持偏移追踪/多字节序；现场总线协议（Modbus/PLC）频繁需要字节级操作                     | 技术债 | dc3-common 无通用字节缓冲工具（既有 analysis 结论 4 仍成立）。各驱动各自处理字节序，重复且易错。                                                                                                                                   | 评估在 dc3-common 抽取通用字节缓冲 + BCD/CRC/LRC 工具。          |

---

## 四、学术依据补强（给关键缺陷的理论对照）

从 `protocol-knowledge-entries.md` 取已核条目（verify 全通过），给 2 条关键缺陷补"书籍怎么说"——这是既有竞品分析没有的维度。

### E1 — MQTT 驱动 `read()` 返回 null 不是缺陷，是协议模型使然（对应 S3）

> **书籍原句**（《物联网之魂：物联网协议与物联网操作系统》/ 孙昊 等 / 机工社·2019 / 1.13.2 发布和订阅模型 / p155，已核）：
>
> "MQTT 协议的一个关键特性是发布和订阅模型。与所有消息协议一样，它将数据的发布者与使用者分离。MQTT
>
协议在网络中定义了两种实体类型：消息代理和一些客户端。代理是一个服务器，它从客户端接收所有消息，然后将这些消息路由到相关的目标客户端。"

**对照结论**：MQTT 是代理转发的 pub/sub 模型，数据的"读"在协议层不存在——订阅者被动接收代理推送。因此 mqtt 驱动 `read()` 返回
null、数据经 `MqttReceiveHandler` 异步到达，是符合协议语义的设计，归为【有意简化】而非缺陷。

### E2 — Modbus 是主从协议，主站主动读写；这支撑"可用驱动应实现完整读写"的预期（佐证 modbus-tcp 为正面范例、iec104/dlms 骨架的 throw 是合理快速失败）

> **书籍原句**（同上书 / 1.14.2 基于现场总线的协议转换器 / p165，已核）：
>
> "通过引用 Modbus 协议代替原自定义串口协议，将通信任务按读、写进行归纳分类，再用 Modbus 协议定义的标准功能码简化通信流程……Modbus
> 协议是主从协议……协议转换器从 Modbus 接收缓冲区获取报文……依据功能码进行发送分析，决定采用单次还是分次发送方式。"

**对照结论**：主从协议下"读/写"是主站的核心通信任务（按功能码归纳）。modbus-tcp 完整实现读写 + 退避是符合主站职责的正面范例；iec104/dlms
骨架在未实现协议 I/O 时显式 throw NotImplemented（而非伪造读写成功），符合"主站要么正确完成读/写、要么明确失败"的语义。

### E3 — OPC UA 健康检查缺失退避的真缺陷，可类比现场总线"主站不应无节制轮询不可达从站"

> 书中现场总线章节强调协议转换器需"依据功能码决定单次/分次发送"（同 p165）——即主站对从站的访问应受控。

**对照结论**（间接类比，本书无 OPC 专门章节，见条目表诚实声明）：OPC UA 驱动 D1 中"不可达设备每周期被重新 TCP+TLS 握手"
是无节制轮询，违背主站受控访问的原则。同仓库 modbus-tcp 已用 failureMap 实现了受控退避，OPC UA 应对齐。

> **学术覆盖度诚实声明**（来自 `protocol-knowledge-entries.md`）：本书是 IoT 通论教材，**工业总线深度覆盖不足**——无 OPC UA /
> EtherNet/IP / DLMS / IEC 104 专门内容，无字节序 ABCD/CDAB 内容，无总线轮询机制内容。因此 E3 只能作间接类比；T1/T2（CIP
> 组帧）的理论对照需另寻 ODVA EtherNet/IP 规范书源，本批次书源无法支撑。

---

## 五、verify 记录（独立子 agent 行号核对）

独立 verify 子 agent 回读每条 `文件:行` 结果：**18/19 条准确，1 条修正**。

- 修正：T2 原 TODO 行号含 209，实读 209 行是日志参数行，TODO 在 208 行单行注释。已改为 208。
- 其余 18 条（D1-D4、S1-S5、T1/T3-T7、E1-E3 引用）行号与代码逐字一致。
- D2 描述"静默降级"经核为 `log.warn`（非完全静默），描述已调整为"以匿名身份继续启动、配置错误被掩盖"——效果层面成立。

---

## 六、总结

| 分类   | 条数 | 编号                                            |
|------|----|-----------------------------------------------|
| 真缺陷  | 4  | D1-D4（全在 OPC-UA + MQTT health 桩）              |
| 有意简化 | 5  | S1-S5（骨架快速失败 + pub/sub 语义）                    |
| 技术债  | 7  | T1-T7（CIP 组帧/CAN JNI/S7 依赖/测试/Modbus 模式/字节工具） |

**一句话**：仓库内**无严重真缺陷**（modbus-tcp/plcs7 是正面范例），真缺陷集中在 OPC-UA 驱动的异常处理（无退避、静默降级、证书未用于身份）和
MQTT 健康桩；6 个骨架驱动中 lwm2m 已完整、iec104/dlms 诚实快速失败、ethernet-ip 编解码最成形但封装头不完整、can 用 shell
调用可工作但开销高。既有 analysis 6 条结论中 3 条仍成立、1 条部分成立、2 条已变化（最大变化：plcs7 已直接依赖对比对象
iot-communication 本身）。
