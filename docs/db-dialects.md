# 关系库选型指南

DC3 的关系核心（认证/元数据/告警/命令与事件历史/可观测性）运行在可插拔的
数据库方言后面（设计见 [docs/design/storage-abstraction.md](./design/storage-abstraction.md)
§3）。一个引擎一个适配器，由 `dc3.db.type` 选择；双方言契约套件
（`dc3-db-tck`）用同一组 mapper 级断言对 PostgreSQL 和 MySQL 8 各跑一遍，
通过即认证。

## 现状矩阵

| | postgres（默认） | mysql |
|---|---|---|
| 镜像 | 生产 `dc3-postgres-base` / TCK `timescale/timescaledb-ha:pg18` | `mysql:8.4`（≥8.0 硬门槛：窗口函数、CTE、SKIP LOCKED、表达式默认） |
| 模块 | `dc3-db/dc3-db-postgres` | `dc3-db/dc3-db-mysql` |
| 种子 | `dc3/dependencies/postgres/initdb/` | `dc3/dependencies/mysql/initdb/`（`pg2mysql_seed.py` 派生，05 手维护） |
| 时间 | `TIMESTAMPTZ`（timestamptz handler 桥接） | `DATETIME(6)`，存取锁定 UTC（TCK 以 `serverTimezone=UTC` 断言） |
| latest 投影 | `ON CONFLICT DO UPDATE` + 行值元组守卫 | `ON DUPLICATE KEY UPDATE` + IF 链展开的元组守卫 |
| RETURNING | 支持，但为可移植已弃用（upsert→同事务 re-select） | 无——re-select 是唯一路径 |
| 序列 | 已退役——版本/围栏令牌行内 `+1`（见下） | 无序列对象，同款行内 `+1` |
| 咨询锁 | `pg_advisory_xact_lock`（事务级，免释放） | `GET_LOCK` 会话级，调用方 try/finally `advisoryUnlock`（PG 版 no-op） |
| operate_time 维护 | BEFORE UPDATE 触发器 | `ON UPDATE CURRENT_TIMESTAMP(6)` 列属性（显式 SET 优先） |
| 修订触发器 | 行级 `track_driver_device_revision_change()` | 行级三触发器（DELIMITER 体），同构语义 |
| 契约套件 | 8/8 | 8/8 |

## 换型操作

1. `make up STACK=optional SERVICES="mysql"` 启动 MySQL（utf8mb4，种子自动灌入）。
2. 主栈环境：`DC3_DB_TYPE=mysql`（默认 `postgres`）。
3. Maven 侧：默认只打包 `dc3-db-postgres`——MySQL 部署把消费模块依赖换成
   `dc3-db-mysql`（与 MQ/TSDB 家族同款约定）。

> MySQL 核心 + 时序库：TimescaleDB 只存在于 PostgreSQL 内——MySQL 部署必须
> 外置时序存储（TDengine/InfluxDB/IoTDB，见 [tsdb-stores.md](./tsdb-stores.md)）；
> `dc3_history` 库只剩 `dc3_point_latest` 投影。

## 关键语义决策（TCK 锁死）

- **行内递增取代序列**：`assignment_version`/`fencing_token` 在每次变更时
  `col = col + 1`。逐驱动/逐设备单调是校验方依赖的性质；全局序列只是碰巧
  提供了它，而 MySQL 没有序列对象。
- **MySQL 的 SET 子句按顺序生效**：后续表达式读到前面赋值的新值（PostgreSQL
  读更新前快照）——守卫列（fencing_token/operate_time/last_state_flag）必须
  排在被守卫列（driver_id/owner_node/entity_state_flag）**之前**。双库契约
  套件对此有专项断言。
- **JSON 文本进 MySQL 必须参数化**（或 `NO_BACKSLASH_ESCAPES` 会话）：默认
  反斜杠转义会把内嵌转义 JSON 的 `\"` 吃掉（PG 不转义）——seed 文件头已设
  会话模式，应用侧一律参数绑定。

## 认证复跑

```bash
export DOCKER_HOST="unix://$(podman info --format '{{.Host.RemoteSocket.Path}}')"
export TESTCONTAINERS_RYUK_DISABLED=true
mvn -s .mvn/settings.xml -f dc3-db/pom.xml -DskipTests install
mvn -s .mvn/settings.xml -pl dc3-db/dc3-db-tck test          # 两库各 8 例
```
