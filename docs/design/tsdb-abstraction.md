# TSDB 抽象（当前架构）

> 状态：已落地。本文描述源码中的唯一实现，不保留历史适配器方案。

## 1. 第一性原理

平台要解决的是三件事：可靠接收设备样本、按租户和序列读取历史、提供可预测的聚合查询。数据库不是
业务边界；业务边界是可取消、可超时、可验证的响应式 Port。因此：

- 数据面只选择一个技术栈：PostgreSQL/TimescaleDB + R2DBC；
- 传输和存储都使用 Reactor，取消必须能到达数据库请求；
- 错误通过异常/transport status 表达，不把错误塞入成功 payload；
- 不做兼容层、双写和运行时多库分支，避免语义漂移和测试矩阵爆炸。

## 2. 组件关系

```text
driver -> data ingest service -> R2dbcTsdbStore -> PostgreSQL/TimescaleDB
                              -> dc3_point_value_latest (relational projection)
gateway -> data facade -> PointValueService -> TsdbStore
```

`dc3-tsdb-core` 仅包含模型和 Port。`dc3-common-data` 是实现和唯一生产消费者；其他中心通过 facade
和 gRPC/HTTP 契约访问，不直接接触 `DatabaseClient`。

## 3. 领域模型

```java
record SeriesKey(long tenantId, long deviceId, long pointId) {}
record SeriesFilter(long tenantId, List<SeriesKey> series) {}
record PointValueSample(
    SeriesKey series,
    Instant deviceTime,
    Instant receiveTime,
    String rawValue,
    String calValue,
    Double numericValue,
    int quality,
    String messageId,
    int schemaVersion,
    String driverNode,
    long sequence,
    long fencingToken,
    long driverId) {}
```

`SeriesFilter` 的 `tenantId` 永远存在；`series` 为空仅表示已授权的租户级扫描。序列 ID 和租户 ID 必须
为正数，所有构造路径都执行校验。样本时间使用 UTC 微秒 `Instant`；原始值和数值投影同时保存，禁止
用默认值掩盖解析失败。

## 4. 响应式 Port

`io.github.pnoker.common.tsdb.spi.TsdbStore` 定义：

```java
Mono<Integer> append(List<PointValueSample> samples);
Mono<Map<SeriesKey, List<PointValueSample>>> last(
    SeriesFilter filter, int limit, TsdbDeadline deadline);
Mono<CursorPage<PointValueSample>> history(
    SeriesFilter filter, TimeWindow window, Cursor cursor,
    int pageSize, TsdbDeadline deadline);
Mono<Map<SeriesKey, WindowAggregate>> aggregate(
    SeriesFilter filter, AggregateFunction fn, TimeWindow window,
    Double percentile, TsdbDeadline deadline);
Mono<Map<SeriesKey, List<BucketAggregate>>> bucketedAggregate(
    SeriesFilter filter, AggregateFunction fn, TimeWindow window,
    Duration bucketWidth, Double percentile, TsdbDeadline deadline);
Mono<Long> count(SeriesFilter filter, TimeWindow window, TsdbDeadline deadline);
```

分析操作还包括 `seriesCounts`、`seriesLastSeen`、`latencyHistogram` 和 `correlation`。每个操作都返回
`Mono` 或 `Flux`；Port 不暴露 `Future.get`、JDBC `ResultSet` 或阻塞迭代器。

## 5. R2DBC 实现

`R2dbcTsdbStore` 使用 `DatabaseClient` 绑定参数执行 SQL，使用已注入的 `R2dbcDialect` 处理时间和方言
差异，使用 `TransactionalOperator` 包围需要原子性的写入和读快照。表固定为
`dc3_history.dc3_point_value`，实现不读取环境变量来选择其他存储。

### 写入不变量

1. `samples` 不得为 `null`、空或含 `null`，数量不得超过 `capabilities.maxAppendBatch`。
2. 每个样本的租户、设备、点、driver identity、fencing token 和时间都经过正数/范围校验。
3. PostgreSQL 使用 `ON CONFLICT`，MySQL 方言使用 `ON DUPLICATE KEY UPDATE`；两者都绑定全部参数，
   不拼接用户输入。
4. 返回接受数量；任意行失败时整批失败，不返回部分成功。

### 读取不变量

- `tenant_id` 是所有 WHERE 和 join 的第一过滤条件；series 集合必须与 filter 的租户一致；
- `last`、`history` 和 count 上限硬失败，不自动 clamp；
- cursor 是 `(deviceTime, series, messageId)` 的稳定锚点，使用严格小于比较支持降序翻页；
- bucket width、percentile、deadline 必须为有限正值；
- sort、聚合函数和表名均来自枚举/白名单，不接收任意 SQL 片段；
- 每个查询挂接 deadline，取消时释放 R2DBC publisher 和事务。

## 6. 分页契约

TSDB 历史查询使用 cursor：

```json
{
  "items": [ ... ],
  "nextCursor": "opaque-base64"
}
```

管理实体查询使用偏移分页：

```json
{
  "items": [ ... ],
  "offset": 0,
  "limit": 50,
  "total": 123,
  "hasNext": true
}
```

两者不能混用；客户端不得发送 `page/current/pages/records`。offset、limit 和 sort 只在关系查询边界
出现，历史大窗口不得通过不断增大 offset 模拟 cursor。

## 7. 一致性与租户隔离

最新值投影、历史写入和幂等 outbox 在 Data 域内按明确事务边界处理。跨服务调用只能经 facade；gRPC
请求携带 tenant ID，服务端再次校验资源归属。缓存键必须包含 tenant ID、series identity 和 schema
version。任何 `tenant_id IS NULL` 分支都视为缺陷，除非数据模型明确声明全局记录。

## 8. 错误、取消与超时

业务错误在服务层抛出 `NotFoundException`、`RequestException` 等，由 gRPC server 映射为
`NOT_FOUND`、`INVALID_ARGUMENT`、`FAILED_PRECONDITION` 或 `INTERNAL`。成功响应不含应用层错误信封。
客户端将 transport status 转为 Reactor error 或空结果（仅对明确的 not-found 查询）。

HTTP 流式接口将断开映射为 Reactor cancel；MCP/agentic 流程在取消时停止工具调用并写入最终状态，不能
把取消误记为成功。所有跨服务请求设置 deadline，重试仅允许在幂等操作且未观察到副作用时执行。

## 9. 能力声明

R2DBC 实现声明微秒精度、租户级扫描、分桶聚合和相关性分析能力；rollup 为 `NONE`，不会伪装成已物化
聚合。能力由 `TsdbCapabilities` 暴露，调用方在不支持时明确拒绝或选择正确的原始扫描路径，不能静默
返回近似数据。

## 10. 测试门槛

- `dc3-tsdb-core`：模型、cursor 比较、percentile 和 deadline 参数契约；
- `dc3-common-data`：每个 R2DBC store 的租户、软删除、乐观锁、冲突、空输入和取消负向测试；
- integration：真实 PostgreSQL 下验证事务原子性、COUNT/items 快照一致性、微秒边界和跨租户拒绝；
- gRPC/HTTP contract：验证直接 payload、标准 status、offset 分页字段和 cursor 不透明性；
- 前端/CLI：验证空页、最后一页、total 变化、流取消及不再发送旧字段。

```bash
mvn -s .mvn/settings.xml -q -DskipTests compile
mvn -s .mvn/settings.xml -q test \
  -pl dc3-tsdb/dc3-tsdb-core,dc3-common/dc3-common-data -am
```

新增数据库后必须先提供真实异步驱动和完整契约测试；在此之前不添加第二个适配器或兼容 API。
