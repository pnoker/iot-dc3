# 时序存储现状

## 结论

DC3 当前只保留一条时序存储实现：`dc3-common-data` 的原生 `R2dbcTsdbStore`。它直接实现
`dc3-tsdb-core` 的响应式 `TsdbStore` Port，使用 `DatabaseClient` 和 `TransactionalOperator` 访问
PostgreSQL/TimescaleDB。平台不再提供 JDBC、同步 HTTP、同步 Session 或“`Mono.fromCallable` 包装”的外部
TSDB 适配器，也不再提供运行时 `dc3.tsdb.type` 选择。

## 模块边界

| 模块 | 职责 |
| --- | --- |
| `dc3-tsdb-core` | 定义 `TsdbStore`、`TsdbModel`、游标分页和能力声明；不依赖具体数据库 |
| `dc3-common-data` | 实现 `R2dbcTsdbStore`，承载写入、历史读取、聚合和租户隔离 |
| `dc3-center-data` | 配置 R2DBC `ConnectionFactory`，通过 facade 暴露数据能力 |

`dc3-tsdb/dc3-tsdb-timescale`、`dc3-tsdb-tdengine`、`dc3-tsdb-influxdb`、`dc3-tsdb-iotdb` 和
`dc3-tsdb-tck` 已从源码构建中移除；`target/` 下的历史 jar 不属于可发布模块。

## 数据模型

历史表是 `dc3_history.dc3_point_value`，逻辑主键为 `(tenant_id, device_id, point_id, create_time)`。
写入列同时保存原始值、计算值、数值投影、质量码、消息幂等字段、驱动节点、序号、fencing token 和
接收时间。所有时间使用 UTC `Instant`，精度为微秒；数值不可解析时 `num_value` 为 `NULL`，不把字符串
强行转换为零。

`dc3_point_value_latest` 等最新值投影属于关系数据面，由 Data 域事务维护；TSDB Port 只负责历史样本，
避免跨存储双写和读写竞态。

## Port 操作

`TsdbStore` 的写入和读取全部返回 `Mono`/`Flux`：

- `append`：非空、有界批量写入；同一批次原子失败或成功，数据库冲突按最后写入胜；
- `last`：按 `SeriesFilter` 返回每个序列最新样本；
- `history`：半开时间窗内按 `deviceTime/messageId/series` 的降序游标分页；
- `aggregate`、`bucketedAggregate`：窗口聚合和升序分桶聚合；百分位参数严格校验；
- `count`、`seriesCounts`、`seriesLastSeen`、`latencyHistogram`、`correlation`：分析查询，均带租户范围。

所有查询都要求正数 `tenantId`、正数序列 ID 和有限 deadline。`limit/pageSize` 超出上限或非法时直接
返回 `IllegalArgumentException`，不静默截断；超时由 Reactor `timeout` 终止并释放连接。

## 分页与排序

历史样本使用不透明 cursor，不暴露页码；cursor 包含设备时间、序列和 messageId，服务端验证租户和窗口。
管理类关系查询统一使用 `offset`/`limit`/`sort`，返回 `items`、`total`、`hasNext`。排序字段由白名单
映射到 SQL 列，禁止把客户端字符串直接拼进 SQL。`COUNT` 与 `items` 在同一事务快照下计算，避免总数与
列表跨快照漂移。

## 租户与一致性不变量

1. 每条 SQL 都显式带 `tenant_id`；跨租户序列、join 和 correlation 请求直接拒绝。
2. 软删除数据必须带 `deleted = 0`；更新和删除同时检查租户及版本，乐观锁冲突不重试覆盖。
3. 批量写入有 `maxAppendBatch` 上限；不存在无界 `concatMap` 或无限缓存。
4. 连接、事务和取消信号由 R2DBC/Reactor 管理，不允许阻塞桥接。
5. 数据库方言由已配置的 R2DBC dialect 提供，启动时必须且只能存在一个有效连接配置。

## 验证

```bash
mvn -s .mvn/settings.xml -q -DskipTests compile
mvn -s .mvn/settings.xml -q test -pl dc3-tsdb/dc3-tsdb-core,dc3-common/dc3-common-data -am
```

需要真实 PostgreSQL 时运行 `make up-db` 后执行 Data 域 R2DBC integration tests。新增存储实现必须先
提供真正的异步客户端、租户隔离测试、取消/超时测试和分页一致性测试；仅用同步客户端包裹 Reactor
不满足本项目架构要求。
