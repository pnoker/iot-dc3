# 设计：可插拔时序存储抽象（TSDB Port）

|                |                                                                                   |
|----------------|-----------------------------------------------------------------------------------|
| **状态**       | Proposed — 尚未实现                                                                |
| **日期**       | 2026-08-20                                                                        |
| **范围**       | 位值（点位遥测历史 + 分析读取）的时序存储                                          |
| **目标库**     | TimescaleDB（默认）、TDengine、InfluxDB、IoTDB —— 外加面向社区存储的 TCK          |
| **前身**       | [`storage-abstraction.md`](./storage-abstraction.md) §4 曾勾画过本轨；本文取代该节 |
| **姊妹篇**     | [`mq-abstraction.md`](./mq-abstraction.md) —— 同一套 port/adapter/TCK 方法论，已于 2026-08-19..20 落地 |
| **讨论**       | Phase 1 启动前开放评审                                                             |

## 1. 摘要

设备遥测历史目前存放在内嵌于主 PostgreSQL 实例的 TimescaleDB 超表（hypertable）
中，读写路径到处泄漏存储细节：幂等的 `ON CONFLICT` 插入、fencing-token 守卫的
最新值 upsert、port 签名里的 MyBatis-Plus `Page`、只有单窗口的聚合，以及**九条
直查超表的看板语句**（其中一条带 `dc3_history.` 跨 schema，另一条用
`time_bucket`）。存储今天并不真正可换——不是因为没有 port（`RepositoryService`
存在），而是因为 port 是 SQL 形状的、残缺的、而且被绕开。

本方案复刻已落地的 MQ 方法论：一个薄 **TSDB port**（`dc3-tsdb-core`）承载平台
真正用到的语义——序列追加（自然 upsert）、末 N 条历史、游标分页的范围历史、
单窗口与**分桶**聚合、范围计数——加上每库一个适配器，由 `dc3.tsdb.type` 选择，
外加一套库中立（broker-neutral）的 TCK 作为验收门槛。TimescaleDB 首先做行为
等价的抽取；随后是 TDengine（国内需求）、InfluxDB 3 和 IoTDB。

两个架构决策承担了大部分工作量：

1. **最新值投影留在关系库。** `dc3_point_latest`（按点位 fencing-token 元组守卫的
   "当前值"）是 OLTP 状态，不是时序数据——它喂给看板、流和告警，且要 join
   名称，而它的语义（元组比较守卫）无法在各 TSDB 间统一表达。所有适配器通过
   同一条关系路径更新它；TSDB 适配器**只存历史**。这一刀直接砍掉最难的跨库
   语义，而不是去协商它。
2. **幂等上移到摄入层；存储做自然 upsert。** 超表的 `message_id` 唯一索引是
   关系库的奢侈品。各 TSDB 按（序列, 时间戳）去重，重复策略因库而异；port
   按适配器声明策略，摄入层为 MQ 级重投保留一个短幂等窗口（至少一次契约
   不变）。

## 2. 现状 —— 已核实的盘点

### 2.1 写入路径（lease 守卫、事务性）

`PointValueReceiver`（MQ 批量）→ schema-v1 校验 → `PostgresRepositoryServiceImpl.
savePointValues` —— 单个 PostgreSQL 事务：

1. `insertHistoryBatch` —— `INSERT INTO dc3_point_value … ON CONFLICT (message_id,
   create_time, device_id) DO NOTHING RETURNING message_id`（幂等追加；
   接受集 = 返回的 id）。
2. `upsertLatestBatch` —— `INSERT INTO dc3_point_latest … ON CONFLICT (tenant_id,
   device_id, point_id) DO UPDATE … WHERE (EXCLUDED.fencing_token,
   EXCLUDED.create_time, EXCLUDED.sequence, EXCLUDED.message_id) > (旧元组)`
   —— fencing 守卫的最新值。
3. 批次预先按 `INGEST_ORDER` 排序，避免并发消费副本间死锁。

### 2.2 物理结构（`dc3/dependencies/postgres/initdb/05-iot-dc3-history.sql`）

- `dc3_point_value` 超表：`message_id, schema_version, driver_node, sequence,
  fencing_token, device_id, point_id, raw_value TEXT, cal_value TEXT,
  num_value DOUBLE NULL（cal_value 的数值投影）, driver_id, tenant_id,
  create_time TIMESTAMPTZ（设备采集时间）, operate_time`。按天分片
  + device_id 16 路 hash 维度；唯一索引 `(message_id, create_time, device_id)`；
  7 天后压缩（segmentby tenant/device/point）；**保留 180 天**。
- `dc3_point_latest`：每个（租户, 设备, 点位）一行，事务性最新值投影。

### 2.3 读取面

| 调用方 | 操作 | 现状 |
|--------|------|------|
| 告警规则窗口（`RepositoryWindowDataSource`） | `aggregateInWindow`（AVG/MIN/MAX/SUM/COUNT + sample_count）、`samplesInWindow` | 走 port |
| 历史分页 | `listPagePointValue` —— MyBatis-Plus `Page` + 相对窗口（`rangeHours`、`rangeKey`、`createTimeFrom`）、设备/点位名 + 启用过滤 | 走 port |
| 末 N 条 | `listHistoryPointValue(tenant, device, point, count)` | 走 port |
| 最新值 | `selectLatestPointValue` / `listLatestPointValues` | 走 port（读 `dc3_point_latest`） |
| **看板（data 侧）** | `countInRange`、`countTotal`、`timeseries`（`time_bucket` + `generate_series` 补空）、`top`、`latestStream`、`latencyHistogram`（`operate_time − create_time` 毫秒分箱）、`hourlyActivity`、`silentSources`、`coverageGapItems` —— **9 条语句直查超表 SQL** | **绕过 port** |
| **看板（manager 侧）** | 再加 1 条跨 schema 读 `dc3_history.dc3_point_value` | **绕过 port** |

### 2.4 哪里不合理（动机，直说）

1. **内嵌存储**：TimescaleDB 与 OLTP 共实例——遥测 I/O 与事务互相争抢；无法
   独立扩缩容、备份与生命周期管理。
2. **port 是 SQL 形状的**：MyBatis-Plus `Page` 与 SQL 聚合语义从
   `RepositoryService` 泄漏出来；外部存储无法忠实实现它。
3. **port 是残缺的**：没有分桶聚合（看板只能用 `time_bucket` 绕过）、没有游标
   分页、没有 count——旁路因此越长越大。
4. **port 被绕过**：10 条看板语句直读超表（一条跨 schema）——任何换库都会让
   看板静默崩掉。
5. **只有单窗口聚合** —— 图表/降采样在 port 里没有原语。
6. **默认了关系库奢侈品**：`message_id` 唯一索引、元组比较 upsert 守卫、
   基于 `RETURNING` 的幂等——全都不可移植。
7. **值模型一值两用**：每个样本带 `raw_value`/`cal_value` 字符串外加
   `num_value` 数值投影——必要（聚合要数值、历史要原文），但投影策略是隐式的。

## 3. 目标 / 非目标

**目标**

- `dc3-common-data`（以及看板服务）面向库中立的 TSDB API 编译，编译 classpath
  上零存储类。
- 每库一个适配器，由 `dc3.tsdb.type` 选择（默认 `timescale`）；同时只激活一个，
  与 `dc3.mq.type` 同构。
- Timescale 部署保持行为：内嵌时物理结构不变、读取结果不变——Phase 1 是抽取，
  不是重写。
- 看板停止绕过 port——所有位值读取都走 port。
- 社区适配器有机械化验收门槛（TCK），同 `dc3-mq-tck`。
- 外置存储部署：存储拥有自己的 compose 服务与 profile；主 PG 彻底卸掉
  Timescale 扩展（保留 `dc3_point_latest`）。

**非目标**

- 关系库方言可插拔（MySQL 等）——那是 storage-abstraction.md 的 Layer 1，
  不变。
- 向量存储（Layer 3 占位）。
- 跨存储查询联邦、存储侧与关系元数据的 JOIN（名称富化留在应用层）。
- 恰好一次（Exactly-once）。保持与全平台一致的至少一次 + 幂等写入。
- 流式分析 / 持续聚合作为 port 原语（能力门控的未来项；看板用分桶拉取查询）。

## 4. port 必须承载的语义

对照上文调用方逐一核实；这是全部清单——不多不少。

| # | 语义 | 现状形态 |
|---|------|---------|
| S1 | 序列标识 | `(tenantId, deviceId, pointId)`；只用数字 ID——名称在应用层富化 |
| S2 | 追加（批量、至少一次） | 每样本一行：完整信封含 `message_id`、`schema_version`、`driver_node`、`sequence`、`fencing_token`、`raw/cal/num`、`driver_id`、`create_time`（设备时间）、`operate_time`（服务端接收时间） |
| S3 | 重复策略 | 存储级（序列, 时间戳）upsert；MQ 级重投由摄入层短幂等窗口去重 |
| S4 | 末 N 条历史 | 某序列最新 `count` 条样本，新的在前 |
| S5 | 范围历史、分页 | 游标分页（按 `(create_time, message_id)` 降序）；相对窗口（`rangeHours`）由调用方在进 port 前解析 |
| S6 | 单窗口聚合 | 对 `num_value` 的 AVG/MIN/MAX/SUM/COUNT（跳过 NULL）+ sample_count |
| S7 | **分桶聚合**（port 新操作） | 窗口内按固定桶宽逐桶同样函数；空桶要么省略（能力 `gapFill=false`）要么补零；桶宽由调用方给定 |
| S8 | 范围计数 | 按序列或全租户，窗口有界 |
| S9 | 延迟来源 | 每样本 `operate_time − create_time`——port 必须**双时间戳都存**，延迟直方图才可计算 |
| S10 | 保留 | 按存储配置的时间过期（今天 180 天），能力声明 |
| S11 | 租户隔离 | 每次读取携带租户范围；TCK 做反向测试 |
| S12 | 时间戳精度 | port 层微秒 `Instant`；适配器可下调到存储原生精度（需文档化） |

**刻意不进 port**（及理由）：

- **最新值** —— 通过关系路径留在 `dc3_point_latest`（§1）。
  `selectLatestPointValue` / `listLatestPointValues` 从 TSDB port 移出，归入
  数据中心读投影的最新值服务。
- **`message_id` 唯一性** —— 摄入层幂等窗口取代唯一索引；该列仍作为字段随行
  存储以便追踪。
- **按范围删除** —— 能力门控（`deleteRange`）：租户注销需要它，但各库策略差异
  巨大（Timescale `DELETE`、TDengine 按子表、IoTDB 范围删除、Influx 只能删
  分区）。实现不了的适配器声明 `false`，注销退回存储原生工具。

## 5. 模块布局

复刻已落地的 `dc3-mq` 家族形态：

```
dc3-tsdb/                     # 顶层聚合器：存储选型家族
├── dc3-tsdb-core/            # Port：样本模型、TsdbStore SPI、能力、
│                             #      游标分页、聚合模型。零存储依赖。
├── dc3-tsdb-timescale/       # 适配器：今天的 SQL 原样抽取
├── dc3-tsdb-tdengine/        # 适配器：TDengine 3.x（超级表、JDBC）
├── dc3-tsdb-influxdb/        # 适配器：InfluxDB 3.x（SQL/Flight）
├── dc3-tsdb-iotdb/           # 适配器：Apache IoTDB（session API）
└── dc3-tsdb-tck/             # 契约套件（每库一个 Testcontainers）
```

`dc3-common-repository` 收缩为（或被并入）port：`RepositoryService` 退役；其
调用方（告警窗口、历史分页、末 N 条）在 Phase 1 迁到 `TsdbStore` + 最新值
服务。平台其他部分不再触碰位值存储。

## 6. 核心 API

```java
package io.github.pnoker.common.tsdb;

/** 序列标识——平台数字 ID；名称在应用层富化。 */
public record SeriesKey(long tenantId, long deviceId, long pointId) {}

/** 一条存储样本；时间戳为 epoch 微秒 Instant。 */
public record PointValueSample(
        SeriesKey series,
        Instant deviceTime,            // create_time：设备采集时间
        Instant receiveTime,           // operate_time：服务端接收时间（S9）
        String rawValue, String calValue,
        Double numericValue,           // calValue 的投影；非数值为 null
        String messageId, int schemaVersion,
        String driverNode, long sequence, long fencingToken, long driverId) {}

public enum AggregateFunction { AVG, MIN, MAX, SUM, COUNT }

public record TimeWindow(Instant from, Instant toExclusive) {}

public record Cursor(Instant deviceTime, String messageId) {}   // 降序分页锚点

public interface TsdbStore {
    String type();
    TsdbCapabilities capabilities();

    /** 批量追加；存储级（series, deviceTime）upsert。同一批次幂等。
        返回接受样本数（按存储尽力而为）。 */
    int append(List<PointValueSample> samples);

    /** S4：单序列最新 limit 条，新的在前。 */
    List<PointValueSample> last(SeriesKey series, int limit);

    /** S5：一页降序历史，从严格晚于 cursor 处开始（null = 从最新）。
        过滤：series 可选（缺省为全租户扫描，能力门控），窗口必填。 */
    CursorPage<PointValueSample> history(HistoryQuery query, Cursor cursor, int pageSize);

    /** S6：单窗口聚合，作用于 numericValue（跳过 NULL）+ 样本计数。 */
    WindowAggregate aggregate(SeriesKey series, AggregateFunction fn, TimeWindow window);

    /** S7：逐桶聚合，桶升序；能力 gapFill=true 时空桶补零，否则省略。
        桶宽由调用方给定。 */
    List<BucketAggregate> bucketedAggregate(SeriesKey series, AggregateFunction fn,
                                            TimeWindow window, Duration bucketWidth);

    /** S8：窗口内样本计数；series 可选表示全租户计数。 */
    long count(Long tenantId, SeriesKey seriesOrNull, TimeWindow window);

    /** S10/删除：能力门控的时间范围删除。 */
    void deleteRange(SeriesKey series, TimeWindow window);
}

public record TsdbCapabilities(
        boolean gapFill,                 // 空桶补零
        boolean tenantWideScan,          // 无 series 的 history/count
        boolean deleteRange,
        OrderingGuarantee ordering,      // NONE | PER_SERIES（每序列追加序）
        Precision precision,             // MICRO / MILLI / NANO
        boolean backfill                 // 接受乱序 / 迟到写入
) {}
```

**写入编排留在 `dc3-common-data`**（schema 校验、摄入幂等窗口、
`dc3_point_latest` 关系 upsert）——port 是存储边界，正如 MQ port 是 broker 边界。

## 7. 逐库映射

| 概念 | TimescaleDB | TDengine 3.x | InfluxDB 3 | IoTDB |
|------|-------------|--------------|------------|-------|
| 序列标识 | 行列（tenant, device, point） | 超级表 `point_value`；**tags** tenant/device/point；每点位一张子表 | measurement `point_value`；**tags** tenant/device/point/driver | 路径 `root.dc3.{tenant}.{device}.{point}` |
| 租户隔离 | WHERE tenant_id | tag 过滤（有索引） | tag 过滤 | 路径前缀过滤 |
| 样本字段 | 列含元数据 | 列：raw/cal NCHAR、num DOUBLE、消息元数据作列 | **fields**：raw(string)、cal(string)、num(float)；消息元数据作 field（绝不做 tag——基数无界） | measurements：raw、cal、num + 元数据作独立 measurement |
| 写入 | SQL 批量（今天的语句，去掉 `ON CONFLICT … RETURNING`——改自然 upsert） | STABLE 批量插入（schemaless 或 SQL） | SQL INSERT / 行协议 | session 批量插入 |
| （序列, 时间）重复策略 | 原地更新（主键语义） | 重复时间戳：后写胜（update 模式） | 同时间戳+tags：字段覆盖 | 同时间戳：覆盖 |
| 末 N 条 | `ORDER BY create_time DESC LIMIT n` | `ORDER BY ts DESC LIMIT n` | SQL `ORDER BY time DESC LIMIT n` | SQL `ORDER BY time DESC LIMIT n` |
| 单窗口聚合 | SQL | SQL | SQL | SQL |
| 分桶聚合 | `time_bucket` | `INTERVAL(width)` | `date_bin(width, time)` | `GROUP BY ([width], time)` |
| 空桶填充 | `generate_series` join | `INTERVAL(…) FILL(0/NULL)` | 应用层填充 | `FILL` 变体 / 应用层 |
| 保留 | drop-chunk 策略（180 天） | 每库 `KEEP` | 分区/桶生命周期 | 每存储组 TTL |
| 精度 | 微秒 | 微秒（`precision us`） | 纳秒（port 下调到微秒） | 按配置 毫秒/纳秒 |
| 乱序回填 | ✅ | ✅ | ✅（v3） | ✅ |
| 客户端 | 今天的 PG 数据源 | JDBC | Flight/HTTP（Java v3 客户端） | session SDK |

**InfluxDB 版本策略**（待决 §10.4，倾向已定）：目标 **InfluxDB 3**
（Core/Enterprise 的 SQL 接口）。OSS 2.x 处于维护态且 Flux 已废弃；2.x 适配器
等于绑死一门死掉的查询语言。能力矩阵中该适配器标注为 `influxdb (3.x)`。

**TDengine 注**：每点位一张子表意味着每个（device, point）对一张子表——通过
schemaless 插入懒创建，或在点位启用时预建。数字 ID 字符串化进表/tag 名
（映射关系文档化、稳定）。

**IoTDB 注**：路径中用数字 ID（`root.dc3.1001.2001.31`）；元数据作为
measurements 随行；按租户子树的 TTL 由存储组布局近似实现。

## 8. 能力矩阵（按适配器发布）

| 能力 | Timescale | TDengine | InfluxDB 3 | IoTDB |
|------|-----------|----------|------------|-------|
| 分桶聚合 | ✅ `time_bucket` | ✅ `INTERVAL` | ✅ `date_bin` | ✅ group-by-time |
| 空桶填充 | ✅ generate_series | ✅ `FILL` | ❌ 应用层 | ⚠️ 部分 |
| 全租户扫描 | ✅ | ⚠️ 超级表扫描 | ✅ | ⚠️ 路径模板 |
| 范围删除 | ✅（DELETE，慢） | ✅ 按子表 | ❌ 分区丢弃 | ✅ |
| 每序列追加序 | ✅ | ✅ | ⚠️（无严格顺序） | ✅ |
| 乱序回填 / 迟到写入 | ✅ | ✅ | ✅ | ✅ |
| 保留 | ✅ chunks | ✅ `KEEP` | ✅ 分区 | ✅ TTL |
| 精度 | 微秒 | 微秒 | 纳秒 | 毫秒/纳秒 |
| 字符串值 | ✅ TEXT | ✅ NCHAR | ✅ fields | ✅ TEXT |
| 内嵌 PG 模式 | ✅（默认） | ❌ | ❌ | ❌ |

启动协商日志汇总当前存储的一行能力，与 MQ port 一致。

## 9. 难点论证

### 9.1 最新值留在关系库（最大的简化）

`dc3_point_latest` 被看板按名称 join、推流到 web、并由 fencing-token 元组比较
守卫——全是 OLTP 关切。每个 TSDB 适配器都得**各自不同地**伪装这些（TDengine
`LAST_ROW`、Influx `last()`、IoTDB `last`），tie-break 规则互不相同，且与摄入
没有事务关联。投影留在 PG：

- 所有适配器经既有关系路径同样地更新它；
- fencing 守卫强度与今天分毫不差；
- TSDB 适配器变成纯历史存储——显著更薄、可测。

代价：外置存储时，历史追加与最新值 upsert 不再是同一事务。失败模式：最新值
更新了但历史丢了（顺序上不可能——历史先写）；或历史写了最新值丢了（批次重投；
历史自然 upsert，最新值幂等重跑；摄入幂等窗口吸收之）。至少一次契约下可接受，
而这正是 S3 把去重职责上移到摄入层的原因。

### 9.2 幂等：唯一索引 → 摄入窗口

`message_id` 唯一索引让写入在**存储内**恰好一次。TSDB 给的是（序列, 时间戳）
upsert。因此 port：(a) 保留 `messageId` 作为可追踪字段；(b) 按适配器声明重复
策略；(c) 摄入层保留有界幂等窗口（每个消费者近期 message id 的 Redis 或内存
LRU），窗口大小对齐 MQ 重投视野——与生产端 driver outbox 同款模式。

### 9.3 分桶聚合成为 port 一等操作（新增）

看板的 `time_bucket` 旁路之所以存在，是因为 port 缺分桶。S7 补上；timescale
适配器用今天的 SQL 原样实现，其余库用各自原生区间函数。空桶填充差异走能力。

### 9.4 看板读取重新表达（单项最大工作量）

十条旁路语句全部映射到 port 原语 + 应用层组装：

| 语句 | port 操作 |
|------|----------|
| countInRange / countTotal | `count` |
| timeseries | `bucketedAggregate`（`gapFill=false` 时应用层补空） |
| top | 逐序列 `count`，应用层排序（全租户扫描能力） |
| latestStream | 最新值服务（PG 投影）——不碰 TSDB |
| latencyHistogram | `history` 分页或对 `receiveTime−deviceTime` 逐桶 `count`——见 §10.2 |
| hourlyActivity | 逐小时 `bucketedAggregate` COUNT，应用层组装 |
| silentSources | 最新值投影（`dc3_point_latest`）时间戳——不碰 TSDB |
| coverageGapItems | `bucketedAggregate` COUNT + 应用层缺口检测 |
| manager 跨 schema 语句 | 改调数据中心 facade，不再读 `dc3_history` |

### 9.5 时间戳与值模型

port 层微秒精度 `Instant`；适配器下调到原生精度（纳秒库保持微秒；毫秒库取整
——文档化，TCK 容忍）。raw/cal/numeric 三元组原样保留——数值投影在摄入时计算
（代码不变），绝不交给存储算。

## 10. TCK —— 验收门槛

`dc3-tsdb-tck`，每库一个容器跑同一套中立套件（timescale、tdengine、iotdb；
influx 视许可）：

1. 追加 → 回读保真：`PointValueSample` 每个字段往返不丢（含 null
   `numericValue` 与双时间戳）
2. 末 N 条：新的在前、条数精确
3. 历史游标：降序分页稳定，游标精确续读（跨页不跳不重）
4. 单窗口聚合：已知夹具（含非数值样本——AVG/MIN/MAX/SUM 必须跳过、COUNT 必须
   计入）
5. 分桶聚合：桶边界对齐 epoch 锚定窗口；空桶按 `gapFill` 补零或省略
6. count：按序列与（能力门控）全租户
7. （序列, 时间戳）重复追加：恰好一条样本，后写胜
8. 乱序回填：早于最新值的样本被接受且可读
9. 租户隔离：跨租户序列读不到任何东西（反向测试）
10. 保留：过期窗口数据消失（时序容忍；能力门控）
11. 精度：微秒时间戳往返保真或按声明精度可预测取整
12. 突发：5k 样本批次完整落库

## 11. 迁移计划

- **Phase 1 —— 抽取 port，行为零变化。**
  建 `dc3-tsdb`（core + timescale 适配器）：超表 SQL 原样搬，改自然 upsert
  （去掉 `ON CONFLICT … RETURNING` 幂等，换摄入窗口），`RepositoryService`
  退役、三个调用方迁移，port 补 S7/S8/游标，并**把十条看板语句全部改表达到
  port 上**（§9.4）。最新值服务（`dc3_point_latest`）从存储接口移入数据中心。
  *门槛：既有单测全绿；E2E（`PostgresHypertableIT`、看板）不改而绿。*
- **Phase 2 —— TCK + TDengine 适配器。** 国内需求最高；超级表映射会最早拷打
  port 的序列模型，等于提前把 port 磨硬。
- **Phase 3 —— InfluxDB 3 + IoTDB 适配器**；发布能力矩阵；
  `dc3/dependencies/<store>/` 下的 `dc3.tsdb.type` compose profile。
- **全程**：`dc3_point_latest` 永不搬家；即使 PG 卸掉 Timescale 扩展也保留它。

## 12. 待决问题

1. **延迟直方图的归宿** —— 每样本存 `receiveTime`（S9，所有存储每样本多付一个
   时间戳）还是把接收延迟指标挪去可观测栈（Prometheus 摄入直方图）、port 砍掉
   S9？倾向：保留 S9（只是一个字段，且存储原生好过引入第二套系统）。
2. **幂等窗口的实现** —— 每数据中心副本内存 LRU（简单、重启即失——至少一次下
   可接受）还是 Redis 背书（重启不失、多一个依赖）？倾向 LRU，按 MQ 重投视野
   定容。
3. **TDengine/IoTDB 的全租户扫描** —— 超级表扫描与路径模板查询存在但有成本；
   `tenantWideScan=false` 时 `top`/`hourlyActivity` 是否降级为"应用层逐序列
   循环"？
4. **InfluxDB 版本** —— 3.x Core（免费、SQL）还是 Enterprise；Phase 3 先做
   v3 Java 客户端的批量与 SQL 覆盖度 spike 再定。
5. **点位禁用** —— 点位禁用/删除时，适配器是否删其子表/序列数据？（今天没人删
   历史。）倾向：不自动删；显式注销用 `deleteRange`。
6. **库间迁移工具** —— 一键双读双写模式，还是离线拷贝 CLI（`dc3-tsdb-copy`）？
   存量历史换库前必须解决。倾向离线 CLI，Phase 3。
7. **`dc3-common-repository` 的结局** —— 并入 `dc3-tsdb-core` 后删除，还是保留
   为应用侧 facade 委托给 port？倾向：并入并删除（不搞兼容别名，从家风）。

## 13. 与 storage-abstraction.md 的关系

本文取代 [`storage-abstraction.md`](./storage-abstraction.md) 的 §4
（Layer 2）：那边勾画时假设存储仍贴着 PG；MQ 的落地经验与本次分析把目标拓宽到
独立存储 + TCK 门槛。Layer 1（关系方言）与 Layer 3（向量占位）不受影响。
