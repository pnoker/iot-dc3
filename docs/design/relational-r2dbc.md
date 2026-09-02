# 设计：IoT DC3 全平台统一响应式持久化

| 项目 | 决策 |
| --- | --- |
| 状态 | 已落地（flag day） |
| 范围 | `auth`、`agentic`、`manager`、`data`、single center、gateway、Web 和 CLI |
| 关系栈 | PostgreSQL 18 + R2DBC + Reactor |
| 时序栈 | TimescaleDB（同一 PostgreSQL 实例） |
| 传输 | WebFlux、gRPC、SSE/NDJSON；真实流才使用 Flux/server-streaming |
| 兼容策略 | 不保留 JDBC/MyBatis、旧 `R<T>`、旧页码协议、旧 gRPC 信封或双写路径 |

## 1. 结论

Auth、Agentic、Manager 和 Data 的关系数据已经统一到同一个 R2DBC 运行时：Spring Boot 管理单一连接池和 `R2dbcTransactionManager`，业务通过显式 Repository Port 访问 PostgreSQL。TimescaleDB 只承载点值历史；最新值投影、告警、命令/事件、Agentic 记忆和操作状态仍使用同一 PostgreSQL 事务语义。

平台不再通过运行时变量选择 MySQL、MariaDB、TDengine、InfluxDB 或 IoTDB。保留这些分支只会复制锁、JSON、分页、DDL 和测试语义；当前没有足以抵消复杂度的部署需求，因此构件、compose 服务、种子和文档全部以 PostgreSQL/TimescaleDB 为唯一事实源。

## 2. 数据格式与边界

- 业务聚合 ID 使用 `BIGINT`，由 `UuidV7.nextLong()` 生成（UUIDv7 的 64 位时间有序投影）；操作、消息与游标等运行时标识使用原生 UUIDv7（`dc3_operation`、`dc3_idempotency` 等运行时表）。原生 UUID 全链路（业务表、protobuf、Web、CLI）仍是未裁决的后续架构决策；在架构评审通过前，不得引入双 ID 字段、别名或转换层。
- 时间列统一 `TIMESTAMPTZ`，数据库精度为微秒。新增代码的业务边界使用 UTC `Instant`；迁移域仍存在的 `LocalDateTime` 字段承载的仍是绝对时间（按 UTC 读写 `TIMESTAMPTZ` 列），属于迁移债务——触碰相关 store 时必须顺手收敛为 `Instant`，且不得新增 `LocalDateTime` 持久化字段。
- 扩展字段统一规范化 JSONB；Repository 在 `Row` 边界固定使用 `row.get(column, String.class)` 解码，再交给 JSON mapper，禁止依赖驱动对象的 `toString()`；命名参数绑定，禁止 SQL 字符串拼接用户输入。
- 资源成功响应直接返回 JSON；列表返回 `items`；错误返回 RFC 9457 `application/problem+json`。
- gRPC 单条 RPC 直接返回 DTO，集合 RPC 返回 `items`；资源不存在使用 `NOT_FOUND`，请求错误使用 `INVALID_ARGUMENT`。
- Facade 只暴露 BO 或只读投影，不把 Web VO、分页实现或 transport 类型泄露到业务边界。

## 3. 分页

### 3.1 关系查询

请求：

```json
{"offset":0,"limit":50,"sort":[{"field":"createTime","direction":"DESC"}]}
```

响应：

```json
{"items":[],"offset":0,"limit":50,"total":0,"hasNext":false}
```

`limit` 仅允许 `1..200`；`offset + limit` 防止溢出。排序字段经过 endpoint 白名单映射为固定 SQL 列。count 与 items 在同一个 R2DBC 事务快照中顺序执行，不能用 `Mono.zip` 跨连接并发查询。客户端不得发送或读取 `page`、`current`、`pages`、`size`、`records`。

### 3.2 历史查询

点值历史使用按 `(device_time, series_id, message_id)` 排序的签名不透明 cursor。响应返回 `items`、`nextCursor` 和 `hasNext`；服务端在解码时验证租户、时间窗口和签名。实时点值、Agentic token 和事件流才使用 `Flux`，并通过 SSE/NDJSON 或 gRPC server-streaming 输出。

## 4. 租户、删除与并发

每个租户查询显式接收 `tenantId`，SQL 在 join、聚合和关联子查询前先限制租户。普通查询必须 `deleted = 0`；更新/删除同时检查租户、ID 和版本，受影响行数为零时报告资源不存在或乐观锁冲突。唯一冲突统一映射为 `ALREADY_EXISTS`/Problem Details。跨服务 facade 调用必须携带租户，不允许 `tenant_id IS NULL` 逃逸。

## 5. Agentic 与异步操作

Agentic 记忆直接写入 `dc3_message`，不再引入 JDBC Chat Memory 或第二个事实源。模型流的完成、失败、取消分别落库；工具回调保持 Reactor 链，禁止 `.block()` 和 `Schedulers.boundedElastic()`。长时间命令/导出返回 `202 OperationAccepted`，状态通过 `dc3_operation` 查询或 watch；重试依赖幂等键和 outbox，不依赖 XA。

## 6. 构件与配置

- `dc3-db-r2dbc-core`：分页、游标、租户、操作状态和方言接口。
- `dc3-db-r2dbc-runtime`：R2DBC 池、事务和 schema fingerprint 启动闸门。
- `dc3-db-r2dbc-postgres`：唯一 PostgreSQL 方言适配器。
- `dc3-common-*/repository/R2dbc*Store`：各域显式 SQL repository。
- `dc3-tsdb-core` + `R2dbcTsdbStore`：TimescaleDB 历史 Port。

`DC3_DB_TYPE`、`DC3_TSDB_TYPE`、MySQL/MariaDB/外置 TSDB compose 服务和对应种子已删除；改变数据库必须重新进行架构评审，不通过兼容开关绕过。

## 7. 发布门禁

```bash
python3 dc3/bin/check_r2dbc_migration.py
python3 dc3/bin/schema_fingerprint.py --check
mvn -s .mvn/settings.xml -q -DskipTests compile
mvn -s .mvn/settings.xml -q -pl dc3-db/dc3-db-tck -am verify
```

发布前还必须通过 Manager OpenAPI annotation gate、gRPC contract tests、Agentic cancellation tests、Web `pnpm check/test/build` 和 CLI `pnpm build/test`。任一门禁失败都阻止发布；不存在“先上线 Auth、其余模块后补”的中间状态。
