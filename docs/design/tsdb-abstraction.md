# 设计：可插拔时序存储抽象（TSDB Port）

|            |                                                                                                                                           |
|------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| **状态**   | Implemented — Phase 1-3 已落地（timescale/tdengine/influxdb/iotdb 四适配器 TCK 认证；能力矩阵发布于 [tsdb-stores.md](../tsdb-stores.md)） |
| **日期**   | 2026-08-20                                                                                                                                |
| **范围**   | 位值（点位遥测历史 + 分析读取）的时序存储                                                                                                 |
| **目标库** | TimescaleDB（默认）、TDengine、InfluxDB、IoTDB —— 外加面向社区存储的 TCK                                                                  |
| **前身**   | [`storage-abstraction.md`](./storage-abstraction.md) §4 曾勾画过本轨；本文取代该节                                                        |
| **姊妹篇** | [`mq-abstraction.md`](./mq-abstraction.md) —— 同一套 port/adapter/TCK 方法论，已于 2026-08-19..20 落地                                    |
| **讨论**   | Phase 1 启动前开放评审                                                                                                                    |

## 1. 摘要

设备遥测历史目前存放在内嵌于主 PostgreSQL 实例的 TimescaleDB 超表（hypertable） 中，读写路径到处泄漏存储细节：幂等的
`ON CONFLICT` 插入、fencing-token 守卫的 最新值 upsert、port 签名里的 MyBatis-Plus `Page`、只有单窗口的聚合，以及 **九条
直查超表的看板语句**（其中一条带 `dc3_history.` 跨 schema，另一条用
`time_bucket`）。存储今天并不真正可换——不是因为没有 port（`RepositoryService`
存在），而是因为 port 是 SQL 形状的、残缺的、而且被绕开。

本方案复刻已落地的 MQ 方法论：一个薄 **TSDB port**（`dc3-tsdb-core`）承载平台 真正用到的语义——序列追加（自然 upsert）、末 N
条历史、游标分页的范围历史、 单窗口与 **分桶**聚合、范围计数——加上每库一个适配器，由 `dc3.tsdb.type` 选择，
外加一套库中立（broker-neutral）的 TCK 作为验收门槛。TimescaleDB 首先做行为 等价的抽取；随后是 TDengine（国内需求）、InfluxDB 3
和 IoTDB。

两个架构决策承担了大部分工作量（另外一组 **跃迁能力** S14–S18 见 §3/§9.6—— 它们默认关闭、显式启用，不参与 Phase 1 行为等价门槛）：

1. **最新值投影留在关系库。** `dc3_point_latest`（按点位 fencing-token 元组守卫的
   "当前值"）是 OLTP 状态，不是时序数据——它喂给看板、流和告警，且要 join 名称，而它的语义（元组比较守卫）无法在各 TSDB
   间统一表达。所有适配器通过 同一条关系路径更新它；TSDB 适配器 **只存历史**。这一刀直接砍掉最难的跨库 语义，而不是去协商它。
2. **幂等上移到摄入层；存储做自然 upsert。** 超表的 `message_id` 唯一索引是 关系库的奢侈品。各 TSDB 按（序列,
   时间戳）去重，重复策略因库而异；port 按适配器声明策略，摄入层为 MQ 级重投保留一个短幂等窗口（至少一次契约 不变）。

## 2. 现状 —— 已核实的盘点

### 2.1 写入路径（lease 守卫、事务性）

`PointValueReceiver`（MQ 批量）→ schema-v1 校验 → `PostgresRepositoryServiceImpl.
savePointValues` —— 单个 PostgreSQL 事务：

1. `insertHistoryBatch` —— `INSERT INTO dc3_point_value … ON CONFLICT (message_id,
   create_time, device_id) DO NOTHING RETURNING message_id`（幂等追加； 接受集 = 返回的 id）。
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
    + device_id 16 路 hash 维度；唯一索引 `(message_id, create_time, device_id)`； 7 天后压缩（segmentby
      tenant/device/point）； **保留 180 天**。
- `dc3_point_latest`：每个（租户, 设备, 点位）一行，事务性最新值投影。

### 2.3 读取面

| 调用方                                       | 操作                                                                                                                                                                                                                                                                                                                                                                                  | 现状                             |
|----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|
| 告警规则窗口（`RepositoryWindowDataSource`） | `aggregateInWindow`（AVG/MIN/MAX/SUM/COUNT + sample_count）、`samplesInWindow`                                                                                                                                                                                                                                                                                                        | 走 port                          |
| 历史分页                                     | `listPagePointValue` —— MyBatis-Plus `Page` + 相对窗口（`rangeHours`、`rangeKey`、`createTimeFrom`）、设备/点位名 + 启用过滤                                                                                                                                                                                                                                                          | 走 port                          |
| 末 N 条                                      | `listHistoryPointValue(tenant, device, point, count)`                                                                                                                                                                                                                                                                                                                                 | 走 port                          |
| 最新值                                       | `selectLatestPointValue` / `listLatestPointValues`                                                                                                                                                                                                                                                                                                                                    | 走 port（读 `dc3_point_latest`） |
| **看板（data 侧）**                          | `countInRange`、`countTotal`、`timeseries`（`time_bucket` + `generate_series` 补空）、`top`（按 device/point/driver 维度分组）、`latestStream`、`latencyHistogram`（`operate_time − create_time` 毫秒分箱）、`hourlyActivity`、`silentSources`（逐序列 lastSeen + HAVING）、`coverageGapItems`（与 `dc3_manager.dc3_point` 跨库 join）—— **9 条语句直查超表 SQL，且全部为租户作用域** | **绕过 port**                    |
| **看板（manager 侧）**                       | 再加 1 条跨 schema 读 `dc3_history.dc3_point_value`                                                                                                                                                                                                                                                                                                                                   | **绕过 port**                    |

### 2.4 哪里不合理（动机，直说）

1. **内嵌存储**：TimescaleDB 与 OLTP 共实例——遥测 I/O 与事务互相争抢；无法 独立扩缩容、备份与生命周期管理。
2. **port 是 SQL 形状的**：MyBatis-Plus `Page` 与 SQL 聚合语义从
   `RepositoryService` 泄漏出来；外部存储无法忠实实现它。
3. **port 是残缺的**：没有分桶聚合（看板只能用 `time_bucket` 绕过）、没有游标 分页、没有 count——旁路因此越长越大。
4. **port 被绕过**：10 条看板语句直读超表（一条跨 schema）——任何换库都会让 看板静默崩掉。
5. **只有单窗口聚合** —— 图表/降采样在 port 里没有原语。
6. **默认了关系库奢侈品**：`message_id` 唯一索引、元组比较 upsert 守卫、 基于 `RETURNING` 的幂等——全都不可移植。
7. **值模型一值两用**：每个样本带 `raw_value`/`cal_value` 字符串外加
   `num_value` 数值投影——必要（聚合要数值、历史要原文），但投影策略是隐式的。

## 3. 目标 / 非目标

**目标**

- `dc3-common-data`（以及看板服务）面向库中立的 TSDB API 编译，编译 classpath 上零存储类。
- 每库一个适配器，由 `dc3.tsdb.type` 选择（默认 `timescale`）；同时只激活一个， 与 `dc3.mq.type` 同构。
- Timescale 部署保持行为：内嵌时物理结构不变、读取结果不变——Phase 1 是抽取， 不是重写。
- 看板停止绕过 port——所有位值读取都走 port。
- 社区适配器有机械化验收门槛（TCK），同 `dc3-mq-tck`。
- **不被现状锁死**：借抽取的机会补齐专业 IoT 时序层应有而现状没有的能力（S14–S18： 多序列读取、图表级聚合、多级保留+rollup、质量位、运维面），全部设计为
  "默认行为不变、显式启用才生效"，不破坏 Phase 1 的行为等价门槛。
- 外置存储部署：存储拥有自己的 compose 服务与 profile；主 PG 彻底卸掉 Timescale 扩展（保留 `dc3_point_latest`）。

**非目标**

- 关系库方言可插拔（MySQL 等）——那是 storage-abstraction.md 的 Layer 1， 不变。
- 向量存储（Layer 3 占位）。
- 跨存储查询联邦、存储侧与关系元数据的 JOIN（名称富化留在应用层）。
- 恰好一次（Exactly-once）。保持与全平台一致的至少一次 + 幂等写入。
- 流式分析 / 持续聚合作为 port 原语（能力门控的未来项；看板用分桶拉取查询）。

## 4. port 必须承载的语义

对照上文调用方逐一核实；这是全部清单——不多不少。

| #   | 语义                                                                         | 现状形态                                                                                                                                                                                                                                                                                                                                                                                                  |
|-----|------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| S1  | 序列标识                                                                     | `(tenantId, deviceId, pointId)`；只用数字 ID——名称在应用层富化                                                                                                                                                                                                                                                                                                                                            |
| S2  | 追加（批量、至少一次）                                                       | 每样本一行：完整信封含 `message_id`、`schema_version`、`driver_node`、`sequence`、`fencing_token`、`raw/cal/num`、`driver_id`、`create_time`（设备时间）、`operate_time`（服务端接收时间）                                                                                                                                                                                                                |
| S3  | 重复策略                                                                     | 存储级（序列, 时间戳）upsert；MQ 级重投由摄入层短幂等窗口去重                                                                                                                                                                                                                                                                                                                                             |
| S4  | 末 N 条历史                                                                  | 某序列最新 `count` 条样本，新的在前                                                                                                                                                                                                                                                                                                                                                                       |
| S5  | 范围历史、分页                                                               | 游标分页（按 `(create_time, message_id)` 降序）；相对窗口（`rangeHours`）由调用方在进 port 前解析                                                                                                                                                                                                                                                                                                         |
| S6  | 单窗口聚合                                                                   | 对 `num_value` 的 AVG/MIN/MAX/SUM/COUNT（跳过 NULL）+ sample_count                                                                                                                                                                                                                                                                                                                                        |
| S7  | **分桶聚合**（port 新操作）                                                  | 窗口内按固定桶宽逐桶同样函数；空桶要么省略（能力 `gapFill=false`）要么补零；桶宽由调用方给定                                                                                                                                                                                                                                                                                                              |
| S8  | 范围计数                                                                     | 按序列或全租户，窗口有界                                                                                                                                                                                                                                                                                                                                                                                  |
| S9  | 延迟来源                                                                     | 每样本 `operate_time − create_time`——port 必须**双时间戳都存**，延迟直方图才可计算                                                                                                                                                                                                                                                                                                                        |
| S10 | 保留                                                                         | 按存储配置的时间过期（今天 180 天），能力声明                                                                                                                                                                                                                                                                                                                                                             |
| S11 | 租户隔离                                                                     | 每次读取携带租户范围；TCK 做反向测试                                                                                                                                                                                                                                                                                                                                                                      |
| S12 | 时间戳精度                                                                   | port 层微秒 `Instant`；适配器可下调到存储原生精度（需文档化）                                                                                                                                                                                                                                                                                                                                             |
| S13 | **租户级分析面**（看板实测口径：全部为租户作用域，非单序列——已逐条核对 SQL） | ① 时间桶计数（timeseries/hourlyActivity）；② 按维度分组计数取 TopN，维度 ∈ {DEVICE, POINT, DRIVER}（top）；③ 逐序列最近样本时间 + 阈值筛（silentSources、coverageGapItems 的子查询）；④ 接收延迟毫秒直方图，对派生表达式 `receiveTime−deviceTime` 分箱（latencyHistogram）；⑤ 逐序列计数（Phase 1 实施时从 manager 拓扑语句补入：同一 point_id 挂多设备时按 (device, point) 分组，②的单维度分组无法还原） |
| S14 | **多序列读取**（跃迁新增）                                                   | 一张图表常画同设备的多个点位曲线；逐序列循环调用太啰嗦。`history`/`last`/`aggregate`/`bucketedAggregate` 统一接受序列集合（单序列是退化解），存储侧一次下发                                                                                                                                                                                                                                               |
| S15 | **图表级聚合函数**（跃迁新增）                                               | `FIRST`/`LAST` 进枚举——配合既有 MIN/MAX 构成 **M4 降采样**（每桶 首/末/最小/最大），百万点渲染成 800px 曲线不再拉原始样本；`PERCENTILE(p)` 能力门控（SLA/P95 风格统计）                                                                                                                                                                                                                                   |
| S16 | **多级保留 + 降采样rollup**（跃迁新增）                                      | 原始 30 天 → 1 分钟粒度 1 年 → 1 小时粒度永久（默认三级，可配）。读操作**对分级透明**：`bucketedAggregate` 桶宽 ≥ 某级粒度时由存储自动从该级 rollup 供数；能力 `rollupSupport: NATIVE / MANUAL / NONE`（Timescale 连续聚合、TDengine 流计算为 NATIVE；Influx 任务为 MANUAL；IoTDB NONE→原始扫描，正确但慢）                                                                                               |
| S17 | **数据质量位**（跃迁新增）                                                   | 样本携带 `quality` 整型码（OPC UA 风格，0=GOOD 默认），驱动可标注 BAD/UNCERTAIN；随样本透传存储，历史查询可按质量过滤（应用层）。现在加是一个 int，将来加是全库迁移——趁 port 未落地先占位                                                                                                                                                                                                                 |
| S18 | **序列发现与运维面**（跃迁新增）                                             | `listSeries(tenant)` 枚举窗口内有数据的序列（迁移 CLI、覆盖审计、缓存预热都要用）；读操作统一携带 deadline（防失控扫描）；适配器声明 `maxAppendBatch`（port 自动分块）                                                                                                                                                                                                                                    |
| S19 | **AI 分析查询面**（跃迁新增，§9.7）                                          | 面向 Agentic/MCP 的粗粒度工具集（latest/history/stats/compare/rank/trend/threshold/correlate/quality 九个），由数据中心 `DataAnalyticsFacade` 组合 port 原语 + 关系元数据实现；port 仅新增能力门控原语 `correlation`；租户上下文由门面从鉴权注入，AI 无法跨租户                                                                                                                                           |

> S1–S13 为"现状实测 + 看板核对"出来的存量语义；S14–S18 是 **借改造机会补的专业能力**
> ——都设计为"默认行为不变、显式启用才生效"，不破坏行为等价的 Phase 1 门槛。

**刻意不进 port**（及理由）：

- **最新值** —— 通过关系路径留在 `dc3_point_latest`（§1）。
  `selectLatestPointValue` / `listLatestPointValues` 从 TSDB port 移出，归入 数据中心读投影的最新值服务。
- **`message_id` 唯一性** —— 摄入层幂等窗口取代唯一索引；该列仍作为字段随行 存储以便追踪。
- **按范围删除** —— 能力门控（`deleteRange`）：租户注销需要它，但各库策略差异 巨大（Timescale `DELETE`、TDengine 按子表、IoTDB
  范围删除、Influx 只能删 分区）。实现不了的适配器声明 `false`，注销退回存储原生工具。

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

`dc3-common-repository` 收缩为（或被并入）port：`RepositoryService` 退役；其 调用方（告警窗口、历史分页、末 N 条）在 Phase 1 迁到
`TsdbStore` + 最新值 服务。平台其他部分不再触碰位值存储。

## 6. 核心 API

读取面统一用一个过滤器形状——单序列、序列集合、全租户是同一形状的三个特例， S13 的租户分析面与 S14 的多序列由此 **合并为同一套
API**，不再有两套读原语：

```java
package io.github.pnoker.common.tsdb;

/** 序列标识——平台数字 ID；名称在应用层富化。 */
public record SeriesKey(long tenantId, long deviceId, long pointId) {}

/** 统一读取过滤器：series 非空=按集合（含单元素）；series 空=全租户扫描
    （tenantWideScan 能力门控）。tenantId 恒在——租户隔离是硬约束（S11）。 */
public record SeriesFilter(long tenantId, List<SeriesKey> series) {

    public static SeriesFilter of(SeriesKey single) {
        return new SeriesFilter(single.tenantId(), List.of(single));
    }

    public static SeriesFilter tenantWide(long tenantId) {
        return new SeriesFilter(tenantId, List.of());
    }
}

/** 一条存储样本；时间戳为 epoch 微秒 Instant。 */
public record PointValueSample(
        SeriesKey series,
        Instant deviceTime,            // create_time：设备采集时间
        Instant receiveTime,           // operate_time：服务端接收时间（S9）
        String rawValue, String calValue,
        Double numericValue,           // calValue 的投影；非数值为 null
        int quality,                   // S17：质量码，0=GOOD（OPC UA 风格）
        String messageId, int schemaVersion,
        String driverNode, long sequence, long fencingToken, long driverId) {}

/** AVG/MIN/MAX/SUM/COUNT 通用；FIRST/LAST 构成图表 M4 降采样（S15）；
    PERCENTILE 能力门控（p 存于查询）。 */
public enum AggregateFunction { AVG, MIN, MAX, SUM, COUNT, FIRST, LAST, PERCENTILE }

public record TimeWindow(Instant from, Instant toExclusive) {}

public record Cursor(Instant deviceTime, String messageId) {}   // 降序分页锚点

/** 读操作统一 deadline（S18）：超时抛 TsdbQueryTimeout，防失控扫描。 */
public record TsdbDeadline(Duration maxWait) {}

public interface TsdbStore {
    String type();
    TsdbCapabilities capabilities();

    // ===== 写入 =====

    /** 批量追加；存储级（series, deviceTime）upsert；同一批次幂等；整批成功或
        整批抛错（无部分接受）。超过 maxAppendBatch 由 port 自动分块。 */
    int append(List<PointValueSample> samples);

    // ===== 读取（统一过滤器：单序列 / 多序列 / 全租户） =====

    /** S4/S14：每个序列各取最新 limit 条，新的在前。 */
    Map<SeriesKey, List<PointValueSample>> last(SeriesFilter filter, int limit, TsdbDeadline deadline);

    /** S5/S14：降序游标分页历史；cursor=null 从最新开始。 */
    CursorPage<PointValueSample> history(SeriesFilter filter, TimeWindow window,
                                         Cursor cursor, int pageSize, TsdbDeadline deadline);

    /** S6/S15：单窗口聚合（按序列分组返回；全租户过滤器=逐序列聚合一次下发）。 */
    Map<SeriesKey, WindowAggregate> aggregate(SeriesFilter filter, AggregateFunction fn,
                                              TimeWindow window, TsdbDeadline deadline);

    /** S7/S15/S16：逐桶聚合，桶升序；空桶按 gapFill 补零或省略。
        对分级保留透明——桶宽达到某 rollup 级粒度时由存储自动从该级供数。 */
    Map<SeriesKey, List<BucketAggregate>> bucketedAggregate(SeriesFilter filter,
            AggregateFunction fn, TimeWindow window, Duration bucketWidth,
            Double percentile, TsdbDeadline deadline);

    /** S8：窗口内样本计数（过滤器三个特例皆可）。 */
    long count(SeriesFilter filter, TimeWindow window, TsdbDeadline deadline);

    // ===== S13：租户级分析面（tenantWideAnalytics 能力门控） =====

    /** S13-①：与 bucketedAggregate 的区别在于面向看板活动图：恒 COUNT、
        不分序列、结果合并为单序列桶。 */
    List<BucketAggregate> bucketedCount(Long tenantId, TimeWindow window,
                                        Duration bucketWidth, TsdbDeadline deadline);

    /** S13-②：按维度分组计数取 TopN，维度 ∈ {DEVICE, POINT, DRIVER}。 */
    List<DimensionCount> countByDimension(Long tenantId, TimeWindow window,
                                          GroupDimension dimension, int limit, TsdbDeadline deadline);

    /** S13-⑤：逐序列计数，按 (tenant, device, point) 全序列标识分组。 */
    List<SeriesCount> seriesCounts(long tenantId, TimeWindow window, TsdbDeadline deadline);

    /** S13-③：窗口内每序列最近样本时间（阈值筛与排序在应用层）。 */
    List<SeriesLastSeen> lastSeenPerSeries(Long tenantId, TimeWindow window, TsdbDeadline deadline);

    /** S13-④：接收延迟直方图——对 receiveTime−deviceTime 毫秒按箱沿计数；
        需要存储侧行级表达式，latencyHistogram 能力门控。 */
    List<LatencyBin> latencyHistogram(Long tenantId, TimeWindow window,
                                      List<Long> binEdgesMs, TsdbDeadline deadline);

    // ===== 运维 =====

    /** S18：窗口内有数据的序列清单（迁移 CLI / 覆盖审计 / 缓存预热）。 */
    List<SeriesKey> listSeries(long tenantId, TimeWindow window, TsdbDeadline deadline);

    /** S10：能力门控的时间范围删除（租户注销）。 */
    void deleteRange(SeriesKey series, TimeWindow window);

    /** S19：两序列按桶对齐的皮尔逊相关系数 + 对齐桶数（AI correlate 工具的
        存储端捷径；correlation=false 时门面拉桶化序列自算）。 */
    CorrelationResult correlation(SeriesKey a, SeriesKey b, TimeWindow window,
                                  Duration alignBucket, TsdbDeadline deadline);
}

/** 降序游标页：items + 下一页锚点（null 表示到底）。 */
record CursorPage<T>(List<T> items, Cursor nextCursor) {}

enum GroupDimension { DEVICE, POINT, DRIVER }

record WindowAggregate(Double value, long sampleCount) {}
record BucketAggregate(Instant bucketStart, Double value, long sampleCount) {}
record DimensionCount(GroupDimension dimension, long entityId, long count) {}
record SeriesLastSeen(SeriesKey series, Instant lastSeen) {}
record LatencyBin(long fromMsInclusive, long toMsExclusive, long count) {}
record CorrelationResult(double pearson, long alignedBuckets) {}

public record TsdbCapabilities(
        boolean gapFill,                 // 空桶补零
        boolean tenantWideScan,          // series 为空的 history/aggregate/count
        boolean tenantWideAnalytics,     // S13 租户级分析面
        boolean latencyHistogram,        // S13-④ 存储侧延迟直方图
        boolean percentile,              // S15 PERCENTILE
        RollupSupport rollupSupport,     // S16：NATIVE（存储原生）/ MANUAL（适配器后台）/ NONE
        int maxAppendBatch,              // S18：port 侧自动分块阈值
        boolean deleteRange,
        OrderingGuarantee ordering,      // NONE | PER_SERIES（每序列追加序）
        Precision precision,             // MICRO / MILLI / NANO
        boolean backfill,                // 接受乱序 / 迟到写入
        boolean correlation              // S19 存储端相关系数
) {}

enum RollupSupport { NATIVE, MANUAL, NONE }
```

### 6.1 配置与运行面

- **选型**：`dc3.tsdb.type`（默认 `timescale`），与 `dc3.mq.type` 同款机制；
  `@ConditionalOnProperty` 激活唯一适配器，启动打印能力协商行。
- **连接配置**（每适配器独立命名空间）：
  `dc3.tsdb.timescale.*`（内嵌模式复用主数据源；外置模式独立数据源）、
  `dc3.tsdb.tdengine.url`（JDBC）、`dc3.tsdb.influxdb.*`（HTTP/Flight）、
  `dc3.tsdb.iotdb.*`（session）。容器化部署经 `DC3_TSDB_TYPE` +
  `DC3_TSDB_*` 环境变量透传，复刻 MQ 的 compose 锚点模式。
- **结构引导（schema bootstrap）**：适配器在启动时幂等地确保自身结构存在—— timescale 内嵌模式沿用种子 SQL（超表/压缩/保留策略）；外置
  timescale 建库建 超表；TDengine 建库 + 超级表；IoTDB 建存储组与 TTL；InfluxDB 3 建库。与 MQ 适配器声明拓扑同一哲学：结构声明是适配器的私事。
- **append 失败语义**：批量要么整批成功、要么整批抛错由摄入层整体重试—— 自然 upsert 保证整批重试安全；不存在部分接受（MQ
  port 同款约定）。
- **健康检查**：每适配器暴露 Spring Boot `HealthIndicator`（连通性 + 版本）， 供 compose healthcheck 与 K8s 探针使用。
- **观测**：port 层统一埋点（append 延迟、批量大小、游标翻页深度），适配器不 各自为政。

### 6.2 与现有调用方的映射（迁移对照）

| 现有调用                                           | 去处                                                     |
|----------------------------------------------------|----------------------------------------------------------|
| `savePointValues`                                  | 数据中心编排（幂等窗口 + `append` + 最新值 upsert）      |
| `listHistoryPointValue(count)`                     | `last`                                                   |
| `samplesInWindow`（告警窗口）                      | `history`（窗口 + 大页）                                 |
| `listPagePointValue` 的名称/启用过滤               | 应用层经关系元数据解析为序列键集合，再进 `history`（S5） |
| `listPagePointValue` 的 `Page`                     | `CursorPage`（游标降序）                                 |
| `selectLatestPointValue` / `listLatestPointValues` | 最新值服务（`dc3_point_latest`，脱离 TSDB port）         |

**迁移风险单列一条**：现有 BO 全线使用 `LocalDateTime`（隐式系统时区），port 统一 `Instant`——Phase 1
必须完成换算并锁定固定时区，否则跨库往返会出现时区漂移。 这是行为等价门槛的一部分（E2E 兜底）。

**Phase 1 实施修订（与原文的偏差，实事求是的记录）**：

1. **时区锁定为平台规范时区而非 UTC。** 原文写"锁 UTC"，但仓库的规范时区是
   `TimeConstant.DEFAULT_ZONEID`（Asia/Shanghai）——latest 投影的 MyBatis
   `TimestamptzLocalDateTimeTypeHandler` 一直按它写库。BO↔`Instant` 换算锁 同一个常量，历史（port 路径）与 latest（mapper
   路径）对同一封信封落同一 绝对时刻；若锁 UTC 反而会让两条投影漂移 8 小时。平台将来若整体迁 UTC， 只需改一个常量。
2. **lease 守卫上移为应用层检查。** 原 `insertHistoryBatch` 的跨 schema
   `dc3_manager.dc3_device_lease` join（stale-owner 拒写）无法进 port，改由 摄入编排按批次内不同设备经
   `DeviceFacade.getActiveOwner`（既有 local+gRPC 全链路）比对 (driverId, driverNode, fencingToken) 信封。与 SQL join 的
   `FOR KEY SHARE` 不同，failover 与 append 竞态时可能漏过个别陈旧历史行 ——latest 投影的 fencing 元组守卫不受影响。
3. **S13-⑤ `seriesCounts` 补入**（见 §4/§9.4）：manager 拓扑语句按 (device, point) 分组计数，单维度 `countByDimension`
   在共享点位场景算不出 正确归属，port 依实补了逐序列计数原语。
4. **latestStream 数据源从超表改为 `dc3_point_latest`**（§9.4 原定）：流语义
   由"最近的原始样本"变为"各序列当前值按时间倒序"，符合设计意图。

**Phase 2 实施记录（2026-08-21，TDengine 适配器 `dc3-tsdb-tdengine`）**： 镜像 `tdengine/tdengine:3.3.6.13` + 驱动
`taos-jdbcdriver:3.9.0`（REST，
`dc3.tsdb.tdengine.*` 独立数据源）。超级表 `point_value` + tags (tenant,device,point)、 确定性子表 `pv_<t>_<d>_<p>`（首写
USING TAGS 自动建表）、库级 `PRECISION 'us'`。 TCK 23 例 0 败 0 错、2 例按声明能力跳过（latencyHistogram/correlation=false）。
拷打出来的教训，全已修并锁进适配器：

1. **时间戳绝不走字符串形态**——REST 驱动按客户端 JVM 时区序列化 `Timestamp`、 服务端按 UTC 解析，任何部署时区都会平移写入（游标分页还会每页漂移、永不
   终止）。根治：写入/窗口/游标一律 **epoch 微秒整数字面量**，读取一律
   `CAST(ts AS BIGINT)` 数字往返（对镜像实证对称）。
2. `AS value` 撞保留字 → 改 `agg_value`；`INTERVAL(60000ms)` 不认 `ms` 单位 → 裸数字按库精度（µs）解释。
3. `PERCENTILE` 只支持单表查询 → 单序列走确定性子表直查，tenantWide 直接拒绝。
4. REST 就绪探测必须 `POST /rest/sql` + Basic Auth——GET 路径式 SQL 一律 404， 容器健康也 404（曾因此误判启动超时烧掉两轮十分钟）。

**Phase 2 第二片（2026-08-21，S19 AI 分析门面落地）**：`DataAnalyticsService`
（dc3-common-data biz/analytics）+ `AnalyticsController` 九个端点 （`/analytics/query_latest|query_history|compute_stats|compare_periods|
rank_entities|trend_analysis|threshold_report|correlate|data_quality_report`）。 与 §9.7 原图的一处对齐说明：MCP 网关的
tools/call 本就按"工具目录 → HTTP 转发到 REST 控制器"工作（目录从 OpenAPI 快照合成），因此"九工具"的落地形态是九个带
`x-dc3-ai` 元数据的端点而非九个 Java 工具类——部署侧 `make openapi` 导出快照 +
`refreshToolCatalog` 后即出现在 MCP 工具列表，inputSchema 由 springdoc 从
`@Schema`（含枚举 allowableValues 与范围说明）生成。关键实现事实：

- 租户注入：控制器一律从鉴权 principal 取 tenantId，请求体无租户字段—— AI 无法跨租户（门面再硬校验一次，S11 双门）。
- 扫描量上限：每次调用 ≤20 序列、每序列 ≤5000 样本、窗口 ≤90 天；超限结构化 拒绝并给收窄建议，截断时 `degradation` 字段如实标注。
- 名称解析：deviceName/pointName 必须唯一命中，歧义时错误里带候选列表 （下一轮对话即可消歧）；percentile/stdDev/斜率/皮尔逊/区间合全在门面算。
- correlate 双路径已验证：timescale 走 STORE 原语；TDengine（能力 false）走 门面桶化自算（单测构造完全正相关夹具，r=1.0 精确）。
- `DataAnnotationGateTest` 守门要求每个请求字段带 `@Schema` 描述——正是 inputSchema 质量的机械化保障，本次全部满足。

**Phase 2 第三片（2026-08-21，S16 多级保留 + rollup 首发验证，Phase 2 至此完成）**： 生命周期落地为 **raw 30 天（seed 05）→ 1
分钟层 1 年 → 1 小时层永久**（`dc3.tsdb.
timescale.rollup.minute-keep-days` 可配）。实施中的关键决策与教训：

1. **与 observability 管线合并而非并存**：seed 07 早已为 Grafana 建了
   `cagg_point_value_1m/1h`（real-time、含 driver 维度与 cal_first/cal_last）—— 适配器最初另建一对 cagg 会造成双份物化开销。收敛为
   **单一结构**：适配器 以 `IF NOT EXISTS` + 与 seed 完全一致的列集引导同名 cagg，嵌入式/独立部署 两条路径汇合；Grafana 与
   port 读同一份物化。
2. **FIRST/LAST 留在原始路径**：共享 cagg 只有 cal_value 的首末（文本），数值 首末列在既有部署的 cagg 里不存在——从层上供
   FIRST/LAST 会造成新旧部署 行为分叉；M4 典型窗口（≤30 天）本就落在原始保留期内，原始扫描即可。
3. **real-time cagg 是"读即刻正确"的关键**：TS 2.13+ 新建 cagg 默认
   `materialized_only=TRUE`（物化前读到 0 行），显式 `materialized_only=FALSE`
   后 TCK 在 **未等任何刷新**的情况下断言分级 COUNT/AVG 与原始扫描逐位一致； 刷新策略只把聚合工作挪去后台。
4. **AVG 从层重组必须 SUM (sum)/SUM (count)**（对每桶平均值再平均是错的）； COUNT=SUM (sample_count)。cagg 列集为此带
   num_sum/num_count。
5. SQL 教训两则：层级 cagg 的 `GROUP BY` 不能用与源列同名的别名（绑到源列， seed 07 早已注释过同款坑——GROUP BY
   显式表达式或序数）；带参数的 time_bucket 在 SELECT 与 GROUP BY 里是两个不同表达式， **GROUP BY 用序数**
   才引用同一输出列。
6. TDengine 案例二十暴露 `bucketedAggregate PERCENTILE` 超级表限制——补了 逐序列子表分支（与 aggregate () 同款策略），TCK
   断言分桶百分位不再报
   "percentile is only supported in single table query"。 门：timescale
   24/24（含新案例"分级读与原始扫描一致"：COUNT/AVG/LAST/ bucketedCount/分桶 P50 五路验证）+ TDengine 24/24（2 例按声明能力跳过，
   NONE 档诚实原始降级同样全过）。deleteRange 后对两层补 refresh_continuous_aggregate，real-time 部分即刻正确、物化部分随后收敛。

## 7. 逐库映射

| 概念                   | TimescaleDB                                                           | TDengine 3.x                                                       | InfluxDB 3                                                                                   | IoTDB                                                  |
|------------------------|-----------------------------------------------------------------------|--------------------------------------------------------------------|----------------------------------------------------------------------------------------------|--------------------------------------------------------|
| 序列标识               | 行列（tenant, device, point）                                         | 超级表 `point_value`；**tags** tenant/device/point；每点位一张子表 | measurement `point_value`；**tags** tenant/device/point/driver                               | 路径 `root.dc3.{tenant}.{device}.{point}`              |
| 租户隔离               | WHERE tenant_id                                                       | tag 过滤（有索引）                                                 | tag 过滤                                                                                     | 路径前缀过滤                                           |
| 样本字段               | 列含元数据                                                            | 列：raw/cal NCHAR、num DOUBLE、消息元数据作列                      | **fields**：raw(string)、cal(string)、num(float)；消息元数据作 field（绝不做 tag——基数无界） | measurements：raw、cal、num + 元数据作独立 measurement |
| 写入                   | SQL 批量（今天的语句，去掉 `ON CONFLICT … RETURNING`——改自然 upsert） | STABLE 批量插入（schemaless 或 SQL）                               | SQL INSERT / 行协议                                                                          | session 批量插入                                       |
| （序列, 时间）重复策略 | 原地更新（主键语义）                                                  | 重复时间戳：后写胜（update 模式）                                  | 同时间戳+tags：字段覆盖                                                                      | 同时间戳：覆盖                                         |
| 末 N 条                | `ORDER BY create_time DESC LIMIT n`                                   | `ORDER BY ts DESC LIMIT n`                                         | SQL `ORDER BY time DESC LIMIT n`                                                             | SQL `ORDER BY time DESC LIMIT n`                       |
| 单窗口聚合             | SQL                                                                   | SQL                                                                | SQL                                                                                          | SQL                                                    |
| 分桶聚合               | `time_bucket`                                                         | `INTERVAL(width)`                                                  | `date_bin(width, time)`                                                                      | `GROUP BY ([width], time)`                             |
| 空桶填充               | `generate_series` join                                                | `INTERVAL(…) FILL(0/NULL)`                                         | 应用层填充                                                                                   | `FILL` 变体 / 应用层                                   |
| 保留                   | drop-chunk 策略（多级：§9.6）                                         | 每库 `KEEP`                                                        | 分区/桶生命周期                                                                              | 每存储组 TTL                                           |
| rollup 物化            | 连续聚合（cagg）NATIVE                                                | 流计算 NATIVE                                                      | 任务 MANUAL                                                                                  | ❌ NONE                                                |
| 精度                   | 微秒                                                                  | 微秒（`precision us`）                                             | 纳秒（port 下调到微秒）                                                                      | 按配置 毫秒/纳秒                                       |
| 乱序回填               | ✅                                                                    | ✅                                                                 | ✅（v3）                                                                                     | ✅                                                     |
| 客户端                 | 今天的 PG 数据源                                                      | JDBC                                                               | Flight/HTTP（Java v3 客户端）                                                                | session SDK                                            |

**InfluxDB 版本策略**（待决 §12.4，倾向已定）：目标 **InfluxDB 3**
（Core/Enterprise 的 SQL 接口）。OSS 2.x 处于维护态且 Flux 已废弃；2.x 适配器 等于绑死一门死掉的查询语言。能力矩阵中该适配器标注为
`influxdb (3.x)`。

**TDengine 注**：每点位一张子表意味着每个（device, point）对一张子表——通过 schemaless 插入懒创建，或在点位启用时预建。数字 ID
字符串化进表/tag 名 （映射关系文档化、稳定）。

**IoTDB 注**：路径中用数字 ID（`root.dc3.1001.2001.31`）；元数据作为 measurements 随行；按租户子树的 TTL 由存储组布局近似实现。

## 8. 能力矩阵（按适配器发布）

| 能力                    | Timescale          | TDengine                                                             | InfluxDB 3       | IoTDB                |
|-------------------------|--------------------|----------------------------------------------------------------------|------------------|----------------------|
| 分桶聚合                | ✅ `time_bucket`   | ✅ `INTERVAL`                                                        | ✅ `date_bin`    | ✅ group-by-time     |
| 空桶填充                | ✅ generate_series | ✅ `FILL`                                                            | ❌ 应用层        | ⚠️ 部分              |
| 全租户扫描              | ✅                 | ⚠️ 超级表扫描                                                        | ✅               | ⚠️ 路径模板          |
| 范围删除                | ✅（DELETE，慢）   | ✅ 按子表                                                            | ❌ 分区丢弃      | ✅                   |
| 每序列追加序            | ✅                 | ✅                                                                   | ⚠️（无严格顺序） | ✅                   |
| 乱序回填 / 迟到写入     | ✅                 | ✅                                                                   | ✅               | ✅                   |
| 保留                    | ✅ chunks          | ✅ `KEEP`                                                            | ✅ 分区          | ✅ TTL               |
| 精度                    | 微秒               | 微秒                                                                 | 纳秒             | 毫秒/纳秒            |
| 字符串值                | ✅ TEXT            | ✅ NCHAR                                                             | ✅ fields        | ✅ TEXT              |
| 租户级分析面（S13-①②③） | ✅ SQL             | ✅ 超级表聚合                                                        | ✅ SQL           | ⚠️ 路径模板/按层聚合 |
| 延迟直方图（S13-④）     | ✅ 表达式分箱      | ❌ 适配器如实声明 false，面板零桶降级（3.3.6 实测无可靠表达式分箱）  | ✅ SQL CASE      | ❌ UDF 或降级        |
| 多序列读取（S14）       | ✅ IN 列表         | ✅ 超级表 tbname 集合                                                | ✅ SQL IN        | ✅ 多路径            |
| FIRST/LAST（S15）       | ✅                 | ✅ FIRST/LAST                                                        | ✅               | ✅                   |
| PERCENTILE（S15）       | ✅ percentile_cont | ✅ PERCENTILE/APERCENTILE                                            | ✅               | ⚠️ 近似/拒绝         |
| rollup 分级（S16）      | ✅ NATIVE 连续聚合 | NONE（S16 落地时切流计算，Phase 2 适配器先扫原始）                   | ⚠️ MANUAL 任务   | ❌ NONE→原始扫描     |
| 质量位存储（S17）       | ✅ 列              | ✅ 列                                                                | ✅ field         | ✅ measurement       |
| 相关系数（S19）         | ✅ SQL JOIN 聚合   | ❌ 门面桶化自算（适配器声明 false，3.3.6 SQL 端无可靠 Pearson 表达） | ✅ SQL           | ❌ 门面桶化自算      |
| 内嵌 PG 模式            | ✅（默认）         | ❌                                                                   | ❌               | ❌                   |

启动协商日志汇总当前存储的一行能力，与 MQ port 一致。

## 9. 难点论证

### 9.1 最新值留在关系库（最大的简化）

`dc3_point_latest` 被看板按名称 join、推流到 web、并由 fencing-token 元组比较 守卫——全是 OLTP 关切。每个 TSDB 适配器都得
**各自不同地**伪装这些（TDengine
`LAST_ROW`、Influx `last()`、IoTDB `last`），tie-break 规则互不相同，且与摄入 没有事务关联。投影留在 PG：

- 所有适配器经既有关系路径同样地更新它；
- fencing 守卫强度与今天分毫不差；
- TSDB 适配器变成纯历史存储——显著更薄、可测。

代价：外置存储时，历史追加与最新值 upsert 不再是同一事务。失败模式：最新值 更新了但历史丢了（顺序上不可能——历史先写）；或历史写了最新值丢了（批次重投；
历史自然 upsert，最新值幂等重跑；摄入幂等窗口吸收之）。至少一次契约下可接受， 而这正是 S3 把去重职责上移到摄入层的原因。

### 9.2 幂等：唯一索引 → 摄入窗口

`message_id` 唯一索引让写入在 **存储内**恰好一次。TSDB 给的是（序列, 时间戳） upsert。因此 port： (a) 保留 `messageId`
作为可追踪字段； (b) 按适配器声明重复 策略； (c) 摄入层保留有界幂等窗口（每个消费者近期 message id 的 Redis 或内存
LRU），窗口大小对齐 MQ 重投视野——与生产端 driver outbox 同款模式。

### 9.3 分桶聚合成为 port 一等操作（新增）

看板的 `time_bucket` 旁路之所以存在，是因为 port 缺分桶。S7 补上；timescale 适配器用今天的 SQL 原样实现，其余库用各自原生区间函数。空桶填充差异走能力。

### 9.4 看板读取重新表达（单项最大工作量）

**已逐条核对 SQL：看板对超表的查询全部是租户作用域（无单序列过滤），`top` 按 device/point/driver 三种维度分组，
`silentSources` 是逐序列 lastSeen + HAVING，
`latencyHistogram` 是对派生表达式的分箱。** 这正是 S13 分析面存在的原因——单 序列原语覆盖不了它们。十条旁路语句的映射：

| 语句（已核口径）                                                         | port 操作                                                                                                                                  |
|--------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| countInRange / countTotal（租户级计数）                                  | `count`（series 缺省）                                                                                                                     |
| timeseries（租户级按桶 COUNT）                                           | `bucketedCount`（`gapFill=false` 时应用层补空）                                                                                            |
| top（按维度分组 TopN：device/point/driver）                              | `countByDimension`                                                                                                                         |
| latestStream                                                             | 最新值服务（PG 投影）——不碰 TSDB                                                                                                           |
| latencyHistogram（对 operate−create 毫秒分箱）                           | `latencyHistogram`（能力门控；不支持时面板降级——去留另见 §12.1）                                                                           |
| hourlyActivity（DOW×小时网格）                                           | `bucketedCount`（1h 桶）+ 应用层折叠成网格                                                                                                 |
| silentSources（逐序列 lastSeen + 阈值 HAVING）                           | `lastSeenPerSeries` + 应用层阈值筛与排序                                                                                                   |
| coverageGapItems（dc3_manager.dc3_point 与超表子查询跨库 join）          | 点位清单走 manager facade + `lastSeenPerSeries`/`count`，应用层组装——**跨库 join 彻底消失**                                                |
| manager 跨 schema 语句（topologyPointVolumes，按 device+point 分组计数） | 改调数据中心 facade（`PointValueFacade.pointVolumes`，新增 `ListSeriesVolumes` RPC），port 侧即 S13-⑤ `seriesCounts`——不再读 `dc3_history` |

### 9.5 时间戳与值模型

port 层微秒精度 `Instant`；适配器下调到原生精度（纳秒库保持微秒；毫秒库取整 ——文档化，TCK 容忍）。raw/cal/numeric
三元组原样保留——数值投影在摄入时计算 （代码不变），绝不交给存储算。

### 9.6 多级保留与降采样（S16，本次跃迁最大的架构增量）

现状是"原始数据单一保留 180 天"——看板拉一年活动图等于全量扫描原始样本，这在 数据量上来后必然崩。专业时序平台的标准生命周期是
**多级保留**：

```
原始样本（30 天） ──rollup──▶ 1 分钟粒度（1 年） ──rollup──▶ 1 小时粒度（永久）
```

（级数、粒度、各级保留期全部可配：`dc3.tsdb.retention.tiers`，默认如上。）

**关键设计决策：读侧对分级透明。** `bucketedAggregate` 的适配器实现按桶宽自动 选级——桶宽 ≥ 1 分钟且请求函数在该级已物化时从
1 分钟 rollup 供数，≥ 1 小时同理； 调用方完全无感，API 零新增。代价是物化函数集须与枚举对齐（每级默认物化
AVG/MIN/MAX/SUM/COUNT/FIRST/LAST；PERCENTILE 不物化，永远扫原始或退化拒绝—— 能力矩阵标注）。

`rollupSupport` 三档：

- **NATIVE**：存储原生物化——TimescaleDB 连续聚合（cagg）、TDengine 流计算。 适配器负责建 cagg/流并保持与保留策略联动。
- **MANUAL**：存储有任务机制但非自动——InfluxDB 3（任务/管道）。适配器启动时 注册任务并轮询健康。
- **NONE**：无机制——IoTDB。适配器如实声明；读操作退化为原始扫描（结果正确、 性能差），保留策略按原始窗口执行。
  **诚实降级，绝不给错误数据。**

写入侧 rollup 由存储/任务异步完成——最终一致（分钟级滞后），看板场景可接受； 告警窗口（S6）永远走原始数据，不受 rollup
滞后影响（其窗口典型为分钟~小时级， 落在原始保留期内）。

### 9.7 AI 分析查询面（S19，面向 Agentic Center / MCP 工具）

**需求来源（已核现状）**：Agentic Center 已落地（`dc3-common-agentic`：session/ message/action/model_provider），MCP 网关经
`tools/call` 让 AI 调工具；今天的 数据 API 只有 `PointValueController`（latest/list/分页历史）与
`point_value.proto`（GetLastValue/ListHistoryValues/读写命令）—— **给人看的 CRUD 面，不是给 AI 的分析面**。AI Agent
的查询模式完全不同：探索式、组合式、 一次提问要跨点位/跨设备/跨时间对比，且必须以 **少量粗粒度工具 + 结构化入参**
呈现（LLM 工具表太长会劣化选择质量），返回必须是 **自包含的统计结论**而非 原始样本页。

#### 三层架构：AI 工具层 / 分析门面 / 存储原语

```
MCP tools/call（AI Agent）
   │  少量粗粒度工具 + JSON Schema 入参（MCP 网关授权，走 §Agentic 鉴权）
   ▼
DataAnalyticsFacade（新，数据中心内）—— AI 查询门面
   │  ① 元数据解析：设备/点位名 → SeriesKey（含按类型/标签筛选 → 序列集）
   │  ② 组合 port 原语成分析结论；名称富化；租户强校验
   │  ③ 结果封包：结论 + 样本量 + 置信说明（AI 拿到即用，不再二次取数）
   ▼
TsdbStore port（S1-S18 原语层，不膨胀）
```

**原则：port 不为 AI 长新操作。** AI 需要的形态学查询绝大多数可由 S7/S13/S14/S15 原语组合出来；真正组合不出的极少数（相关性、阈值停留）才以能力门控原语下沉，
且必须先证明"应用层拉样本自算不可行"（数据量门槛论证）。

#### AI 工具集（MCP `tools/call` 暴露，粗粒度 × 结构化入参）

| 工具                  | 语义                                               | 门面实现（用到的 port 原语）                                             |
|-----------------------|----------------------------------------------------|--------------------------------------------------------------------------|
| `query_latest`        | 设备/点位当前值（按名称或 ID，可批量）             | 最新值服务（PG 投影）                                                    |
| `query_history`       | 时间窗内多序列历史（含 M4 降采样开关）             | `history` / `bucketedAggregate(FIRST/LAST/MIN/MAX)`（S5/S7/S14/S15）     |
| `compute_stats`       | 一组序列的统计画像：均值/标准差/分位数/极值/样本量 | `aggregate` + `bucketedAggregate(PERCENTILE)`（S6/S15）                  |
| `compare_periods`     | 同期对比（本周 vs 上周、昨日同时段）               | 两次 `aggregate`/`bucketedAggregate` + 门面差值与百分比                  |
| `rank_entities`       | 按活跃度/均值/极值给设备或点位排名                 | `countByDimension`（计数维度）或序列集循环 `aggregate` + 门面排序        |
| `trend_analysis`      | 趋势与突变：逐桶序列 + 线性拟合/变化率在门面算     | `bucketedAggregate(AVG)` + 门面最小二乘                                  |
| `threshold_report`    | 超阈值时段报告：何时超、超多久、峰值多少           | `bucketedAggregate(MIN/MAX)` 定位 + 窗口内 `history` 精查 + 门面合并区间 |
| `correlate`           | 两序列相关性（皮尔逊）                             | **能力门控原语** `correlation`（下方）或降级：窗口内桶化序列门面计算     |
| `data_quality_report` | 覆盖率/缺口/静默源/质量位分布                      | `lastSeenPerSeries` + `count` + S17 质量位过滤                           |

工具表刻意 **九个封顶**：每个入参都是强类型 JSON Schema（枚举/范围约束）， 模糊表述（"帮我看看数据"）由 LLM 先选工具再填参，而不是一个万能
query 接口。

#### 下沉 port 的唯一新原语：相关性

`correlation(SeriesKey a, SeriesKey b, TimeWindow, Duration alignBucket)` —— 两序列按桶对齐后的皮尔逊系数 +
对齐桶数。Timescale/TDengine/Influx 3 都能 SQL 端 JOIN 聚合表达；IoTDB 退化（能力 `correlation=false`）→ 门面拉两列桶化
序列自算（M4 级数据量，可行）。 **阈值停留（threshold duration）不再下沉**：
`threshold_report` 用桶聚合 + 局部精查的组合已够，避免为它发明 STATE_DURATION 的跨库语义（原待决 §12.11
的结论：由该组合方案替代，状态时长分析关闭）。

#### 安全与租户

- 工具入参 **只收名称或 ID + 窗口**，SeriesKey 的 tenantId 由门面从 MCP 鉴权 上下文注入——AI 永远无法跨租户取数（S11
  硬约束在门面二次校验）。
- 每个工具带 `TsdbDeadline`（S18）；单次工具调用的扫描量上限（序列数 × 窗口 × 粒度）在门面校验，超限返回结构化降级建议（缩窗口/降粒度），不静默截断。
- 工具调用审计进既有 `dc3_action`（Agentic 已有该表）。

## 10. TCK —— 验收门槛

`dc3-tsdb-tck`，每库一个容器跑同一套中立套件（timescale、tdengine、iotdb； influx 视许可）：

1. 追加 → 回读保真：`PointValueSample` 每个字段往返不丢（含 null
   `numericValue` 与双时间戳）
2. 末 N 条：新的在前、条数精确
3. 历史游标：降序分页稳定，游标精确续读（跨页不跳不重）
4. 单窗口聚合：已知夹具（含非数值样本——AVG/MIN/MAX/SUM 必须跳过、COUNT 必须 计入）
5. 分桶聚合：桶边界对齐 epoch 锚定窗口；空桶按 `gapFill` 补零或省略
6. count：按序列与（能力门控）全租户
7. （序列, 时间戳）重复追加：恰好一条样本，后写胜
8. 乱序回填：早于最新值的样本被接受且可读
9. 租户隔离：跨租户序列读不到任何东西（反向测试）
10. 保留：过期窗口数据消失（时序容忍；能力门控）
11. 精度：微秒时间戳往返保真或按声明精度可预测取整
12. 突发：5k 样本批次完整落库
13. 租户桶计数（S13-①）：桶边界对齐、跨序列合计正确、空桶按 gapFill 处理
14. 按维度计数（S13-②）：device/point/driver 三维度分组与 TopN 排序正确
15. 逐序列 lastSeen（S13-③）：窗口内每序列的最新时间精确
16. 延迟直方图（S13-④，能力门控）：双时间戳样本落入正确毫秒箱
17. 多序列过滤器（S14）：多序列 last/history/aggregate 各归各序列、互不串扰
18. FIRST/LAST 桶值（S15）：每桶首/末与窗口边界对齐，可组成 M4 渲染
19. PERCENTILE（S15，能力门控）：已知夹具的 P50/P95 精度在存储声明容差内
20. rollup 分级读（S16，NATIVE/MANUAL 能力门控）：写入→等待物化→宽桶查询结果 与原始扫描一致；NONE 档如实退化为原始扫描
21. quality 质量位（S17）：非零质量码随样本往返不丢
22. 读 deadline（S18）：窗口过大时超时抛 TsdbQueryTimeout 而非挂死
23. 相关系数（S19，能力门控）：已知构造序列（完全正相关/负相关/无相关）的 皮尔逊系数在容差内；对齐桶数正确

## 11. 迁移计划

- **Phase 1 —— 抽取 port，行为零变化。**
  建 `dc3-tsdb`（core + timescale 适配器）：超表 SQL 原样搬，改自然 upsert （去掉 `ON CONFLICT … RETURNING` 幂等，换摄入窗口），
  `RepositoryService`
  退役、三个调用方迁移，port 补 S7/S8/游标，并 **把十条看板语句全部改表达到 port 上**（§9.4）。最新值服务（`dc3_point_latest`
  ）从存储接口移入数据中心。 *门槛：既有单测全绿；E2E（`PostgresHypertableIT`、看板）不改而绿；
  `LocalDateTime→Instant` 全线换算完成并锁 UTC（§6.2）。*
- **Phase 2 —— TCK + TDengine 适配器。** 国内需求最高；超级表映射会最早拷打 port 的序列模型，等于提前把 port
  磨硬。跃迁能力随阶段落地：S17 质量位与 S15 FIRST/LAST 成本极低随 Phase 1/2 直接进；S14 多序列与 S18 运维面进 Phase 2；S16
  多级保留+rollup 在 Phase 2 以 timescale cagg 首发验证、Phase 3 推广到其余三库。 **S19 AI 分析门面（
  `DataAnalyticsFacade` + 九工具）与 MCP 网关对接随 Phase 2 落地**——Agentic 链路已存在，门面只依赖 port 原语与关系 元数据，是
  Phase 2 里对上层可见度最高的一块。
- **Phase 3 —— InfluxDB 3 + IoTDB 适配器**；发布能力矩阵；
  `dc3/dependencies/<store>/` 下的 `dc3.tsdb.type` compose profile。
- **全程**：`dc3_point_latest` 永不搬家；即使 PG 卸掉 Timescale 扩展也保留它。

## 12. 待决问题

1. **延迟直方图的归宿** —— 每样本存 `receiveTime`（S9，所有存储每样本多付一个 时间戳）还是把接收延迟指标挪去可观测栈（Prometheus
   摄入直方图）、port 砍掉 S9？倾向：保留 S9（只是一个字段，且存储原生好过引入第二套系统）。
2. **幂等窗口的实现** —— 每数据中心副本内存 LRU（简单、重启即失——至少一次下 可接受）还是 Redis 背书（重启不失、多一个依赖）？倾向
   LRU，按 MQ 重投视野 定容。
3. **TDengine/IoTDB 的全租户扫描** —— 超级表扫描与路径模板查询存在但有成本；
   `tenantWideScan=false` 时 `top`/`hourlyActivity` 是否降级为"应用层逐序列 循环"？
4. **InfluxDB 版本** —— 3.x Core（免费、SQL）还是 Enterprise；Phase 3 先做 v3 Java 客户端的批量与 SQL 覆盖度 spike 再定。
5. **点位禁用** —— 点位禁用/删除时，适配器是否删其子表/序列数据？（今天没人删 历史。）倾向：不自动删；显式注销用 `deleteRange`。
6. **库间迁移工具** —— 一键双读双写模式，还是离线拷贝 CLI（`dc3-tsdb-copy`）？ 存量历史换库前必须解决。倾向离线 CLI，Phase 3。
7. **`dc3-common-repository` 的结局** —— 已决（Phase 1 实施）：整体删除，无兼容 别名。`PointValueBO`/`PointValueQuery` 迁入
   `dc3-common-model`（包名不变），
   `WindowAggregateQuery`/`WindowAggregateResult`/`PointQueryBO` 随旧路径退役； 调用方直接面向 `TsdbStore` +
   数据中心摄入/最新值服务。
8. **外置 Timescale 形态** —— 同一物理 PG 的独立库（运维简单、仍抢实例资源） 还是独立 PG 实例（彻底隔离、多一个服务）？倾向独立实例——既然要外置，就
   外置彻底；内嵌模式继续作为小部署默认。
9. **租户差异化保留** —— 现在全局 180 天；按租户设保留（TDengine 按库 KEEP、 Influx 按分区）是否值得做？倾向：Phase 3
   前不做，全局策略 + 能力声明足够。
10. **rollup 物化函数集与滞后窗口** —— 各级物化哪些函数（默认七函数）、物化 滞后多久（cagg
    水位）算"可读"？倾向：物化滞后对读侧完全透明不暴露，宽桶 查询允许读到略旧的 rollup（看板场景分钟级滞后无感）。
11. ~~**状态时长分析（STATE_DURATION）**~~ —— 已由 §9.7 的 `threshold_report`
    组合方案解决（桶聚合定位 + 窗口精查 + 门面合并区间），不再单设 port 原语。 若未来高频"状态机分析"证明该组合过重，再评估能力门控原语。

## 13. 与 storage-abstraction.md 的关系

本文取代 [`storage-abstraction.md`](./storage-abstraction.md) 的 §4 （Layer 2）：那边勾画时假设存储仍贴着 PG；MQ
的落地经验与本次分析把目标拓宽到 独立存储 + TCK 门槛。Layer 1（关系方言）与 Layer 3（向量占位）不受影响。

**Phase 3（2026-08-21，四适配器齐装 + 能力矩阵发布——TSDB 抽象全部完成）**：
`dc3-tsdb-influxdb`（3.11.2-core，v3 HTTP 行协议+query_sql CSV 直连，零客户端 依赖）与 `dc3-tsdb-iotdb`
（2.0.10-standalone，session API，树路径
`root.dc3.t*.d*.p*`——路径节点不能纯数字，设计 §7 的 `root.dc3.{tenant}...`
映射据此修订）通过契约套件认证： **timescale 24/24、tdengine 24/24（2 跳过）、 influxdb 24/24（2 跳过）、iotdb 24/24（3 跳过）**
。跳过=适配器如实声明不 支持、套件按能力门控跳过；对应能力由门面降级或明确拒绝，绝不给错数据。 能力矩阵按各适配器
**实际声明**发布于 `docs/tsdb-stores.md`（§8 原预估表 已由其取代）。外置存储服务（tdengine/influxdb/iotdb）进 optional
compose 栈，IoTDB 挂载两行配置（µs 精度+全接口 RPC）。两个新适配器的方言教训 （全在选型文档）：influxdb 整型字段必须 `i` 后缀否则列绑
Float64 永久丢 精度、JSON 数字走科学计数法必须读 CSV；iotdb `WHERE time` 裸数字按毫秒 解释（与库精度无关，µs 字面量静默匹配空集）须用
`+00:00` ISO 形态、 session 在端口映射下必须关重定向、TCK 容器须覆盖
`dn_rpc_address=0.0.0.0`（模板默认只听 127.0.0.1，端口映射永远够不着）。
