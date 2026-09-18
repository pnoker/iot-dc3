# 设计：统一 PostgreSQL R2DBC 存储

| 项目 | 决策 |
| --- | --- |
| 状态 | 已落地（flag day） |
| 范围 | Auth、Manager、Data、Agentic、运行时操作、审计和时序历史 |
| 关系库 | PostgreSQL 18 |
| 访问层 | Spring Boot R2DBC + `DatabaseClient` |
| 时序扩展 | TimescaleDB，仍位于同一 PostgreSQL 实例 |
| 分页 | 关系查询 offset；历史样本签名 cursor |
| 兼容策略 | 不保留 JDBC/MyBatis、旧 `R<T>`、旧页码协议或双写适配层 |

## 1. 第一性原理

平台的核心问题不是“支持多少数据库”，而是让租户数据在并发写入、取消、重试和跨服务调用下保持可证明的一致性。多套数据库方言会复制事务、锁、JSON、分页、种子和测试语义；收益只有在明确的部署需求存在时才成立。当前没有这种需求，因此选择一个能同时覆盖关系、时序、JSON 和向量能力的 PostgreSQL，并把其语义直接暴露在一个响应式存储边界内。

## 2. 运行时拓扑

```text
HTTP/WebFlux ─┐
gRPC          ├─> Reactive service ─> Repository port ─> DatabaseClient
Agentic/MCP ──┘                                  │
                                                └─> PostgreSQL 18
                                                     ├─ dc3_* schemas
                                                     ├─ TimescaleDB hypertables
                                                     └─ pgvector / AGE capabilities
```

Spring Boot 只创建一个连接池和一个 `R2dbcTransactionManager`。服务通过 facade 访问其他域；控制器和业务代码不感知连接 URL、驱动或传输细节。

## 3. 数据格式

- ID 使用 UUIDv7 生成的 64 位逻辑标识，写入和 gRPC/HTTP 表示保持稳定。
- 时间使用 UTC `Instant`，数据库统一 `TIMESTAMPTZ(6)`；禁止本地时区 `LocalDateTime` 作为跨服务协议。
- 扩展字段使用规范化 JSONB（稳定键序、无未定义数字/时间类型）；所有参数使用绑定变量。
- 响应直接返回资源或 `items` 集合；错误使用 RFC 9457 `application/problem+json`，gRPC 使用标准 status code。

## 4. 分页与排序

### 4.1 关系列表

```json
{
  "offset": 0,
  "limit": 50,
  "sort": [{"field": "createTime", "direction": "DESC"}]
}
```

```json
{
  "items": [],
  "offset": 0,
  "limit": 50,
  "total": 0,
  "hasNext": false
}
```

`limit` 强制为 `1..200`；`offset + limit` 防止整数溢出。排序字段先过 endpoint 白名单，再映射为固定 SQL 列。count 与 items 在同一个 R2DBC 事务快照中顺序执行，不使用 `Mono.zip` 跨连接并发查询。

### 4.2 历史样本

历史点值按 `(device_time, series_id, message_id)` 生成签名不透明 cursor。请求只携带 `cursor` 和 `limit`，服务端验证租户、窗口和签名；不接受 `page`、`current`、`size`、`records` 或 SQL 列名。

## 5. 租户与并发

每个 tenant-scoped repository 方法都接收 `tenantId`，SQL 同时带租户谓词。跨表 join、correlation、series 聚合先限定租户再 join。软删除默认过滤；写入使用幂等键或唯一约束；更新/删除使用版本条件并检查 `rowsUpdated`。驱动租约采用 fencing token，过期 claim 使用行锁和单事务。

## 6. 响应式与取消

所有 repository/service/facade/controller 方法返回 `Mono`/`Flux`。R2DBC publisher 的取消信号释放连接、终止下游 gRPC/SSE，并把 Agentic 消息状态落为 `CANCELLED`；禁止 `.block()`、`Schedulers.boundedElastic()` 和“同步客户端外包 Reactor”。长任务返回 `202 OperationAccepted`，状态由 `dc3_operation` 查询或 watch。

## 7. 测试门禁

- `check_r2dbc_migration.py`：MyBatis/JDBC/旧分页/旧响应/阻塞桥接均为零。
- PostgreSQL schema fingerprint：种子脚本、运行时表和 JSON/时间格式变更必须同步更新指纹。
- `dc3-db-tck`：真实 PostgreSQL 容器验证租户隔离、软删除、乐观锁、幂等、fencing、分页和取消边界。
- Web/CLI contract：只发送 `offset`、`limit`、`sort` 或 `cursor`，只读取 `items`、`total`、`hasNext`/`nextCursor`；删除统一 HTTP `DELETE`。
- gRPC contract：普通 CRUD unary，真实流才 server-streaming；错误使用 `NOT_FOUND`、`INVALID_ARGUMENT`、`PERMISSION_DENIED`、`ALREADY_EXISTS` 等标准状态。

任何门禁失败都阻止发布。该设计不提供兼容层或渐进双写；需要其他数据库时必须重新评审并建立独立契约，而不是在当前运行时偷偷增加分支。
