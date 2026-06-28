---
title: 常见问题
---

# 常见问题（Q&A）

## 许可证与授权

### IoT DC3 使用什么开源协议？

IoT DC3 基于 [AGPL-3.0](https://github.com/pnoker/iot-dc3/blob/release/LICENSE-AGPL.txt) 协议发布。

AGPL-3.0 的核心要求：如果你修改了平台代码并**通过网络提供服务**（包括 SaaS、内部系统），你必须将修改后的完整源代码开源。如果只是内部使用、未分发、未通过网络提供服务，则无需开源。

### AGPL-3.0 对我们公司意味着什么？

| 场景 | 是否需要开源 |
|------|-------------|
| 内部部署、不改代码、仅自己用 | 否 |
| 内部部署、改了代码、仅自己用（未对外提供服务） | 否（但建议贡献回来） |
| 基于 DC3 做 SaaS 产品对外售卖 | **是**，必须开源全部修改 |
| 基于 DC3 做了二次开发并分发给客户部署 | **是**，必须开源全部修改 |
| 只是调用 DC3 的 API，未修改 DC3 本身 | 否 |

### 可以闭源二次开发吗？

如果你只是通过 API 调用 DC3、没有修改 DC3 源码本身，你的调用方代码可以闭源。一旦你修改了 DC3 源码并通过网络对外提供服务，AGPL-3.0 要求你将修改开源。

### 有商业授权吗？

目前没有独立的商业授权。如果你的使用场景与 AGPL-3.0 兼容，可以直接使用。如有特殊需求，可通过社区渠道联系维护者讨论。

---

## 收费与商业模式

### IoT DC3 本身收费吗？

**不收费。** IoT DC3 是完全开源免费的，你可以自由下载、使用、修改和分发（遵守 AGPL-3.0 条款）。

### 项目方如何盈利？

目前 IoT DC3 是维护者的个人开源项目，以社区驱动方式运作。未来可能的商业化方向包括：技术支持服务、企业定制开发、SaaS 托管服务等。核心平台本身将始终保持开源。

### 使用 IoT DC3 需要付费给谁吗？

不需要。你不需要向任何人付费即可使用 IoT DC3。但你需要自行承担部署所需的服务器、数据库等基础设施费用。

---

## 技术选型

### 为什么用 Java 而不是 Go/Node.js/Python？

IoT DC3 选择 Java + Spring 生态的核心原因：

1. **工业物联网场景**：工业领域大量现存系统是 Java 生态（SCADA、MES、ERP），Java 在工业集成中有天然优势
2. **Spring 生态成熟度**：Spring Boot/Cloud/Security/Data 提供开箱即用的分布式、安全、数据访问能力
3. **JVM 稳定性**：长时间运行的设备接入服务对 GC、内存管理要求高，JVM 经过数十年的生产验证
4. **AI 集成**：Spring AI 让平台能以统一范式接入多家大模型（OpenAI、Claude、本地模型等）
5. **团队技能**：维护者在 Java/Spring 生态有深厚积累

### 为什么用 PostgreSQL 而不是 MySQL？

1. **TimescaleDB 扩展**：IoT 时序数据场景，PostgreSQL 的 TimescaleDB 扩展提供原生的超表自动分区、压缩、数据保留策略
2. **Apache AGE**：图数据库扩展，用于设备关系、拓扑路径查询
3. **pgvector**：向量扩展，为 AI 语义检索提供基础设施
4. **更丰富的数据类型**：JSONB、数组、范围类型等
5. **更严格的 SQL 标准**：在复杂查询和事务场景下更可靠

IoT DC3 对 PostgreSQL 的依赖很深，这三个扩展（TimescaleDB + AGE + pgvector）是平台数据架构的核心。

### 支持哪些设备协议？应该怎么选择？

平台内置 **28 个驱动模块**，覆盖：

- **工业总线/PLC**：Modbus TCP/RTU、OPC UA/DA、S7 (Siemens)、MELSEC、FINS (Omron)、EtherNet/IP
- **SCADA/电力/计量**：BACnet/IP、IEC 104、DLMS、SL651、SNMP
- **IoT/无线**：MQTT、CoAP、LwM2M、HTTP、BLE、Zigbee、CAN
- **串口/通用网络**：Serial、TCP/UDP
- **数据库**：MySQL、PostgreSQL、Oracle、SQL Server

选择建议：先确定现场设备支持的协议，再看驱动能力矩阵（[驱动能力矩阵](../drivers/matrix)）确认所需读写/订阅能力是否满足。

---

## 部署与运维

### 最低硬件要求？

**开发环境**（仅依赖栈 PostgreSQL + RabbitMQ）：
- CPU: 2 核
- 内存: 4 GB
- 磁盘: 20 GB

**生产环境**（全栈：网关 + 4 个中心 + N 个驱动 + 依赖栈）：
- CPU: 8 核及以上
- 内存: 16 GB 及以上
- 磁盘: 100 GB SSD 及以上（时序数据持续增长，需规划扩容）

### 如何从开发环境迁移到生产？

1. **安全加固**：修改默认密钥/密码、启用 TLS、配置防火墙规则、关闭调试端点
2. **数据持久化**：确保 PostgreSQL 和 RabbitMQ 数据卷正确挂载和备份
3. **高可用**：根据需求配置 PostgreSQL 主从、RabbitMQ 集群
4. **监控告警**：部署 Prometheus + Grafana（docker-compose-optional.yml 已包含）
5. **日志收集**：接入 ELK（docker-compose-optional.yml 已包含）
6. **环境变量**：参考 [环境变量配置](../quickstart/environment)，将开发变量替换为生产值

详见 [安全策略](./security) 的生产基线清单。

### 数据怎么备份？

PostgreSQL 数据备份：

```bash
# 全量备份
podman exec dc3-postgres pg_dumpall -U dc3 > backup.sql

# 仅备份平台数据（不含 TimescaleDB 时序数据）
podman exec dc3-postgres pg_dump -U dc3 \
  --schema=dc3_auth --schema=dc3_manager --schema=dc3_data > backup_platform.sql
```

生产环境建议配置 pgBackRest 或 pg_dump 定时任务 + 异地存储。

---

## 驱动开发

### 怎么开发一个新驱动？

1. 阅读 [驱动开发指南](../development/driver-authoring)
2. 在 `dc3-driver/` 下复制最接近的驱动模块作为模板
3. 实现 Driver SDK 要求的 `read()`、`write()` 和（可选的）`subscribe()` 方法
4. 在 `dc3/docker-compose.yml` 中添加驱动服务配置
5. 写文档（参考已有驱动文档页的格式）

### 驱动一定要用 Java 吗？

Driver SDK 本身是 Java 的，但你也可以通过 **MQTT 桥接** 或 **HTTP 代理** 的方式用任意语言实现设备接入：非 Java 程序将数据发到 MQTT Topic → MQTT 驱动订阅 → 进入平台数据管道。不过这种方式会丢失 SDK 内置的状态管理、自动重连、健康上报等能力。

---

## AI 能力

### AI 能做什么？

IoT DC3 的 Agentic 中心（基于 Spring AI）让大模型具备以下能力：

- **设备查询**：自然语言查询设备状态、位号值、历史数据
- **命令下发**：通过对话让 AI 向设备写入参数
- **告警分析**：AI 分析告警历史，给出根因推断
- **数据洞察**：对时序数据做趋势分析和异常检测

AI 能力通过 MCP（Model Context Protocol）协议暴露，可被 Claude Desktop、VS Code、Cursor 等 AI 工具直接调用。详见 [AI 概览](../ai/)。

### 支持哪些大模型？

通过 Spring AI，理论上支持所有主流模型提供商：OpenAI、Anthropic Claude、Google Gemini、阿里通义千问、百度文心一言、本地 Ollama 模型等。具体配置见 [Agentic 中心](../ai/agentic)。

---

## 社区与贡献

### 遇到问题怎么求助？

1. 先查 [故障排查指南](../guide/troubleshooting)
2. 搜索 [GitHub Issues](https://github.com/pnoker/iot-dc3/issues) 看是否有人遇到过
3. 没找到？提新 Issue，附上：版本号、日志、复现步骤、环境信息

### 如何参与贡献？

见 [贡献指南](./contributing)。任何形式的贡献都欢迎：报告 bug、改进文档、提交代码、参与讨论。

### 有商业支持服务吗？

目前项目以社区形式运作，暂无官方商业支持。如有企业级支持需求，可通过社区渠道联系维护者沟通。
