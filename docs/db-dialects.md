# 关系库选型指南

DC3 的关系核心（认证/元数据/告警/命令与事件历史/可观测性）运行在可插拔的
数据库方言后面（设计见 [docs/design/storage-abstraction.md](./design/storage-abstraction.md)
§3）。一个引擎一个适配器，由 `dc3.db.type` 选择；双方言契约套件 （`dc3-db-tck`）用同一组 mapper 级断言对每个引擎各跑一遍，通过即认证。

当前认证三引擎： **PostgreSQL（默认）/ MySQL 8 / MariaDB 10.6+**，契约 24/24。范围决策（2026-08-24 定）： **DB2、H2、HSQLDB
明确不支持**——DB2 方言成本高且需求为零，H2/HSQLDB 是嵌入式测试库，不适合多服务并发写的 IoT 生产部署。SQLite
因库级写锁与无网络协议同样不适合多服务形态（未来若 出单体进程内嵌模式可另议）。Oracle 与 SQL Server 是真实的传统企业需求，
路线见文末——未拍板前不动工。

## 现状矩阵

|                   | postgres（默认）                                               | mysql                                                                  | mariadb                                                                                      |
|-------------------|----------------------------------------------------------------|------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| 镜像              | 生产 `dc3-postgres-base` / TCK `timescale/timescaledb-ha:pg18` | `mysql:8.4`（≥8.0 硬门槛：窗口函数、CTE、SKIP LOCKED、表达式默认）     | `mariadb:10.11`（≥10.6：SKIP LOCKED、表达式默认）                                            |
| 模块              | `dc3-db/dc3-db-postgres`                                       | `dc3-db/dc3-db-mysql`                                                  | `dc3-db/dc3-db-mariadb`（官方 mariadb 驱动）                                                 |
| 种子              | `dc3/dependencies/postgres/initdb/`                            | `dc3/dependencies/mysql/initdb/`（`pg2mysql_seed.py` 派生，05 手维护） | 复用 MySQL 种子（`utf8mb4_unicode_ci` 两库通吃，非 MySQL 8 专属的 0900_ai_ci）               |
| 时间              | `TIMESTAMPTZ`（timestamptz handler 桥接）                      | `DATETIME(6)`，存取锁定 UTC（TCK 以 `serverTimezone=UTC` 断言）        | 同 MySQL                                                                                     |
| latest 投影       | `ON CONFLICT DO UPDATE` + 行值元组守卫                         | `ON DUPLICATE KEY UPDATE` + 行别名 `AS new` + IF 链                    | 同形，但 `VALUES(col)` 引用待插行——**MariaDB 从未采纳 AS new 行别名**（实证 10.11 语法错误） |
| RETURNING         | 支持，但为可移植已弃用（upsert→同事务 re-select）              | 无——re-select 是唯一路径                                               | 同 MySQL                                                                                     |
| 序列              | 已退役——版本/围栏令牌行内 `+1`（见下）                         | 无序列对象，同款行内 `+1`                                              | 同                                                                                           |
| 咨询锁            | `pg_advisory_xact_lock`（事务级，免释放）                      | `GET_LOCK` 会话级，调用方 try/finally `advisoryUnlock` + 返回值校验（'0'/NULL = 锁忙即失败重试） | 同 MySQL                                                                                     |
| operate_time 维护 | BEFORE UPDATE 触发器                                           | `ON UPDATE CURRENT_TIMESTAMP(6)` 列属性（显式 SET 优先）               | 同 MySQL                                                                                     |
| 修订触发器        | 行级 `track_driver_device_revision_change()`                   | 行级三触发器（DELIMITER 体），同构语义                                 | 同                                                                                           |
| 契约套件          | 8/8                                                            | 8/8                                                                    | 8/8                                                                                          |

## 换型操作

1. `make up STACK=optional SERVICES="mysql"`（或 `mariadb`）启动引擎（utf8mb4，种子自动灌入；`99-grants.sql` 把应用用户授权扩到全部五个库——entrypoint 默认只授 `MYSQL_DATABASE` 一个库）。
2. 主栈环境：`DC3_DB_TYPE=mysql`（或 `mariadb`；默认 `postgres`）。
3. **每服务覆写 JDBC URL**：各中心库不同（auth→`dc3_auth`、data→`dc3_data`+`DC3_DB_HISTORY_URL`、manager→`dc3_manager`、agentic→`dc3_agentic`）。源码运行为对应服务导出 `DC3_DB_URL`；容器部署用 `docker-compose.override.yml` 按服务注入。只改 `DC3_DB_TYPE` 不改 URL 会静默继续连 PostgreSQL（`application-*.yml` 的 URL 默认值是逐服务写死的 PG 形态）。漏装适配器 jar 时启动即 fail-fast（`MybatisPlusConfig` 守卫）。
4. Maven 侧：默认只打包 `dc3-db-postgres`——MySQL 部署把消费模块依赖换成
   `dc3-db-mysql`（与 MQ/TSDB 家族同款约定）。

> MySQL 核心 + 时序库：TimescaleDB 只存在于 PostgreSQL 内——MySQL 部署必须
> 外置时序存储（TDengine/InfluxDB/IoTDB，见 [tsdb-stores.md](./tsdb-stores.md)）；
> `dc3_history` 库只剩 `dc3_point_latest` 投影。

## 关键语义决策（TCK 锁死）

- **行内递增取代序列**：`assignment_version`/`fencing_token` 在每次变更时
  `col = col + 1`。逐驱动/逐设备单调是校验方依赖的性质；全局序列只是碰巧 提供了它，而 MySQL 没有序列对象。
- **MySQL 的 SET 子句按顺序生效**：后续表达式读到前面赋值的新值（PostgreSQL
  读更新前快照）——守卫列（fencing_token/operate_time/last_state_flag）必须 排在被守卫列（driver_id/owner_node/entity_state_flag）
  **之前**。双库契约 套件对此有专项断言。
- **JSON 文本进 MySQL 必须参数化**（或 `NO_BACKSLASH_ESCAPES` 会话）：默认 反斜杠转义会把内嵌转义 JSON 的 `\"` 吃掉（PG
  不转义）——seed 文件头已设 会话模式，应用侧一律参数绑定。
- **部分唯一索引用生成列守卫保真**：PostgreSQL 的 `CREATE UNIQUE INDEX ... WHERE deleted = 0`
  翻译为 MySQL 存储生成列（`CASE WHEN <谓词> THEN 1 ELSE NULL END STORED`）+ 复合唯一索引——NULL 不参与唯一性，删除行不受约束，与 PG 部分索引语义一致（`pg2mysql_seed.py` 自动生成，41 条全覆盖；非唯一部分索引按纯性能优化省略）。

## 认证复跑

```bash
export DOCKER_HOST="unix://$(podman info --format '{{.Host.RemoteSocket.Path}}')"
export TESTCONTAINERS_RYUK_DISABLED=true
mvn -s .mvn/settings.xml -f dc3-db/pom.xml -DskipTests install
mvn -s .mvn/settings.xml -pl dc3-db/dc3-db-tck test          # 三引擎各 8 例
```

## 未拍板：Oracle 与 SQL Server 路线

两者都是真实企业需求，但方言成本远超 MySQL（各约 1.5-2 倍工作量），且各有一个硬前提：

- **Oracle**：全部 upsert 改 `MERGE INTO`、`ROWNUM`/`OFFSET` 分页、JSON 用
  `JSON_VALUE`、`SYSDATE/SYSTIMESTAMP`；认证容器 `gvenzl/oracle-free` 可用。 需要社区明确需求信号再动工。
- **SQL Server**：`MERGE` + `TOP ?` + `OFFSET/FETCH`、JSON 用 `JSON_VALUE`
  （2016+）、无 SKIP LOCKED（`READPAST` 行锁提示近似）； **官方镜像无 ARM64 构建**——Apple Silicon 上无法本地认证，只能在 amd64
  CI 跑，这是当前最 大的实际障碍。
