# 关系库运行时

IoT DC3 的关系数据面统一使用 **PostgreSQL 18 + R2DBC**。这是一个架构约束，不是运行时可切换的方言选项：认证、管理、数据、Agentic、操作状态和审计数据都由同一套 PostgreSQL 语义承载。

## 为什么只保留 PostgreSQL

- PostgreSQL 同时提供 `JSONB`、行级锁、CTE、`RETURNING`、窗口函数和 TimescaleDB/pgvector 扩展，覆盖平台的事务、时序和 Agentic 数据需求。
- 一个数据库驱动、一个连接池、一个事务管理器消除方言分支、重复 TCK 和多套种子脚本，降低线上行为差异。
- 所有服务使用 Spring Boot 管理的单一 R2DBC `ConnectionFactory`；禁止 JDBC、MyBatis、HikariCP 和同步阻塞桥接。
- 数据库版本和 schema 指纹在启动时校验；不匹配直接拒绝启动，不做隐式降级或兼容迁移。

## 运行时配置

每个 center 只需要提供 PostgreSQL R2DBC URL（默认值来自 `dc3/env/dev.env`）：

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://*************@localhost:35432/dc3
dc3:
  r2dbc:
    schema-fingerprint: ${DC3_SCHEMA_FINGERPRINT}
    schema-contract: r2dbc-flag-day-v1
```

`dc3.db.type`、MySQL/MariaDB 驱动和跨引擎 URL 已移除。设置未知数据库不会触发备用适配器，而会在启动校验阶段失败。

## 数据与事务不变量

1. 业务表按 schema 划分：`dc3_auth`、`dc3_manager`、`dc3_data`、`dc3_history`、`dc3_agentic` 和 `public` 运行时表。
2. 租户数据的每条读写 SQL 都带 `tenant_id`；跨租户关联在服务层和 SQL 层同时校验。
3. 软删除行只在明确的恢复/审计查询中出现，普通查询必须 `deleted = 0`。
4. 更新和删除同时检查 `tenant_id`、`id`、`version`（适用时），受影响行数为零即报告并发冲突或资源不存在。
5. JSON 统一使用 `JSONB`；写入通过命名参数和 `CAST(... AS JSONB)`，禁止字符串拼接。
6. 时间统一为 UTC 微秒精度；应用边界使用 `Instant`，数据库列使用 `TIMESTAMPTZ(6)`。
7. 分页统一为 `offset`/`limit`/`sort` 的 `OffsetPage(items, offset, limit, total, hasNext)`；历史样本使用签名 cursor。

## 验证

```bash
python3 dc3/bin/schema_fingerprint.py --check
python3 dc3/bin/check_r2dbc_migration.py
mvn -s .mvn/settings.xml -q -pl dc3-db/dc3-db-tck -am test
```

`dc3-db-tck` 只启动 PostgreSQL Testcontainers，复用生产 R2DBC repository port，覆盖租户隔离、软删除、乐观锁、幂等写入、租约 fencing、分页和 schema 指纹。任何新增存储实现必须先获得架构评审，不得通过增加第二套运行时适配器来规避契约。
