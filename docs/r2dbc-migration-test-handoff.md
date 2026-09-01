# R2DBC flag-day 全平台测试交接与发布验收

| 项目 | 约束 |
| --- | --- |
| 状态 | 待执行；完成本文全部硬门禁前不得宣称迁移验收通过 |
| 设计基线 | [`design/relational-r2dbc.md`](./design/relational-r2dbc.md) |
| 执行对象 | Claude 或后续承担白盒、灰盒、黑盒验证的工程 Agent |
| 目标范围 | `auth`、`manager`、`data`、`agentic`、single center、gateway、Web、CLI、PostgreSQL/TimescaleDB、RabbitMQ |
| 容器运行时 | 只允许 Podman；不得静默回退到 Docker daemon |
| 发布策略 | flag day；不测试、不实现旧关系库、旧分页、旧响应信封或双写兼容 |
| 证据原则 | 只接受本轮从干净构建开始生成的命令、日志、报告和数据库查询结果 |

## 1. 结论先行

当前代码已经具备 R2DBC 迁移的主要实现和较强的单元测试基线，但**尚无证据证明迁移已经一步到位**。现有测试不能支持“所有数据库路径、所有 Repository、所有服务拓扑、所有页面均已在真实环境验证”的结论。

本轮验收不是再跑一遍已有测试，而是补齐以下闭环：

1. 设计、DDL、Java、protobuf、HTTP、Web 和 CLI 使用同一数据契约。
2. 每个生产 R2DBC 持久化组件都经过真实 PostgreSQL 测试，而不是只测 mock。
3. 生产 `dc3-postgres` 镜像从空卷完整初始化，所有扩展、Schema、Timescale 对象和 fingerprint 均经过查询验证。
4. single 与 distributed 两套拓扑都通过同一业务用例；local facade 与 gRPC facade 的语义一致。
5. Gateway、Web 和 CLI 使用真实后端，不以 stub HTTP server 或 mock API 代替全链路测试。
6. 分页、流式响应、取消、背压、租户隔离、乐观锁、故障恢复和持续重启都通过自动化测试。
7. 所有缺陷先由失败测试复现，修复后从当前阶段开始重跑，后续阶段连续执行。

任意一个必测项为 `FAIL`、未解释的 `SKIP` 或没有原始证据时，最终结论只能是“未通过”。

## 2. 不可变架构边界

### 2.1 唯一平台数据库

平台持久化唯一目标是 PostgreSQL 18 + TimescaleDB + R2DBC。本文所说的“不同数据库搭配”是对**同一目标技术栈的不同真实部署形态**做交叉验证：

- 完整生产 `dc3-postgres` 镜像，执行全部 canonical init SQL。
- 上游 `timescale/timescaledb-ha:pg18` 容器，只用于 Repository/TCK 的独立交叉验证。
- 单实例共享 `dc3_auth`、`dc3_manager`、`dc3_data`、`dc3_history`、`dc3_agentic`、`public` Schema。
- distributed 服务分别连接独立 PostgreSQL 实例，验证服务边界不存在跨库直读依赖。
- 空卷冷启动、持久卷重启、连接中断、错误 fingerprint、缺表和错误凭证组合。

不得重新引入 MySQL、MariaDB、InfluxDB、IoTDB、TDengine 或旧 selector。仓库中的 MySQL、Oracle、SQL Server 等**南向驱动**用于连接外部设备或数据源，不属于平台关系持久化，不得误删，也不得被当作平台多数据库支持。

### 2.2 最终数据契约

本轮不接受“设计写新格式、实现继续跑旧格式”的状态。最终发布契约必须是：

- 业务聚合 ID、租户 ID 及其外键：PostgreSQL 原生 `UUID`，Java 使用 `UUID`，值满足 RFC 9562 UUIDv7；HTTP/JSON、Web、CLI 和 protobuf 使用规范化 UUID 字符串。
- 不得把 UUID 塞入 `BIGINT`，也不得继续使用 `UuidV7.nextLong()` 作为所谓 UUIDv7 实现；数值计数、序号和指标仍可使用整数。
- 绝对时间：PostgreSQL `TIMESTAMPTZ`，Java 业务边界使用 `Instant`，protobuf 使用 `google.protobuf.Timestamp`，HTTP 使用 UTC RFC 3339 且以 `Z` 结尾。
- 只有确实不代表时间线上一个瞬间的领域值，才允许 `LocalDateTime`，并必须在字段契约中说明无时区语义。
- 扩展字段：PostgreSQL `JSONB`；Repository 固定以 `String.class` 读取后交给 JSON mapper；HTTP 输出 canonical JSON，不泄露驱动对象。
- 货币或必须精确的小数使用 `NUMERIC`/`BigDecimal`；设备测量值同时保留原始字符串和可选 numeric projection，不用二进制浮点替代精确业务值。
- API 成功响应直接返回资源、`OffsetPage` 或 `CursorPage`；失败返回 RFC 9457 `application/problem+json`。

由于本次明确不考虑兼容和迁移，若当前表、种子、实体、protobuf、Web 或 CLI 仍使用 `Long/BIGINT` 业务 ID，应直接完成全链路硬切换并重建测试数据，不加双字段、转换层、旧字段别名或灰度开关。

### 2.3 响应式语义

“全面异步”不等于把所有方法机械改成 `Flux`：

- 单条资源和一次有界业务结果使用 `Mono<T>`。
- 有界分页使用 `Mono<OffsetPage<T>>` 或 `Mono<CursorPage<T>>`。
- 只有实时点值、Agentic token、事件 watch 等真实流才使用 `Flux<T>`，并通过 SSE、NDJSON 或 gRPC server-streaming 暴露。
- 不允许 controller 返回无界 `Flux` 后在网关或客户端一次性收集。
- 请求线程和响应式链路不得出现 `.block()`、`.subscribe()` 失控启动、`boundedElastic()` 包装关系数据库调用或 JDBC bridge；唯一允许的启动期阻塞必须有明确边界和超时。

### 2.4 分页契约

配置类、管理类有界列表使用 offset 分页：

```json
{
  "offset": 0,
  "limit": 50,
  "sort": [
    {"field": "createTime", "direction": "DESC"}
  ]
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

高频变化、高基数和历史列表使用签名 keyset cursor：

```json
{
  "cursor": null,
  "limit": 50,
  "sort": [
    {"field": "deviceTime", "direction": "DESC"}
  ]
}
```

```json
{
  "items": [],
  "nextCursor": null,
  "hasNext": false
}
```

硬规则：

- `limit` 为 `1..200`，`offset >= 0`，并检查加法溢出。
- 客户端排序字段必须映射到 endpoint 自己的固定 SQL 白名单；值不得直接拼入 SQL。
- offset 页的 `COUNT` 和 items 查询在同一 `REPEATABLE READ`、read-only、`REQUIRES_NEW` R2DBC 事务中顺序执行。
- 相同排序键必须追加唯一稳定 tie-breaker，通常为 `id`；禁止跨页重复或漏项。
- cursor 必须绑定租户、查询条件摘要、位置、过期时间和签名；不得把 offset 做 Base64 后冒充 cursor。
- 客户端和服务端都不得接受或返回 `page`、`current`、`pages`、`size`、`records`。

## 3. 当前事实与证据缺口

以下结果是迁移开发过程中的历史记录，只能作为基线，不是本轮发布证据：

| 项目 | 历史结果 | 本轮判定 |
| --- | --- | --- |
| 后端单元测试 | 2818 tests，0 failures，0 errors，79 skipped | 需从 clean 重新执行；逐项解释 skip |
| Web Vitest | 69 files / 633 tests | 需重新执行，且不能替代真实后端 Playwright |
| CLI Vitest | 9 files / 66 tests | 需重新执行，且不能替代真实 Gateway CLI E2E |
| PostgreSQL R2DBC TCK | 9/9 | 仅实例化少量 Store，未覆盖全部生产 Repository |
| 迁移静态门禁 | 开发过程中曾为 0 | 当前工作树仍在变化，必须重新执行 |

### 3.1 已确认的阻断问题

执行者必须先为下列问题增加自动化复现，再修复，不能只在最终报告中备注：

1. **Schema fingerprint 漂移**：`08-iot-dc3-runtime.sql` 当前值为 `bf799f...6840`，但 `docker-compose-dev.yml`、`docker-compose.yml`、`dc3/env/dev.env` 和 `dc3/env/dev.env.sh` 仍默认 `c0f7f...6fa`。真实服务可能在启动闸门直接失败。根治要求是消除重复事实源或增加覆盖所有分发配置的 CI 一致性测试，不只是手工替换四处字符串。
2. **ID 契约未落地**：设计声明 UUIDv7，但主模型、protobuf 和多数业务表仍是 `Long/BIGINT`；`UuidV7.nextLong()` 的注释也明确它只是 legacy `BIGINT` fallback。必须完成原生 UUID 全链路硬切换，或修正设计；本计划按用户已确定的 flag-day 目标要求前者。
3. **时间契约未落地**：DDL 已大量使用 `TIMESTAMPTZ`，但迁移域仍有大量 `LocalDateTime` 持久化/业务字段。必须区分绝对时间和无时区领域值，并让绝对时间统一为 `Instant`。
4. **现有 `dc3-e2e` 不是全栈 E2E**：当前 harness 只启动 TimescaleDB 和 RabbitMQ；REST Assured 测试访问本地 `HttpServer` stub；Timescale 测试自行创建 `_e2e` 表。它可以证明测试工具可用，不能证明 Auth/Manager/Data/Agentic/Gateway 真实链路。
5. **Repository 实库覆盖不足**：当前 PostgreSQL TCK 只有 9 个用例，实例化 `R2dbcPointValueLatestStore`、`R2dbcEntityStateStore`、`R2dbcPointValueIngestOutbox`、`R2dbcEntityAlarmStore`、`R2dbcNotifyHistoryStore`、`R2dbcDriverStore`，不能代表 62 个 `R2dbc*Store` 全部通过。
6. **生产初始化覆盖不足**：当前 TCK 使用上游 `timescale/timescaledb-ha:pg18`，没有执行 `00-iot-dc3-extensions.sql`、`07-iot-dc3-observability.sql` 和完整 `05-iot-dc3-history.sql`，因此不能证明生产镜像的 AGE、pgvector、continuous aggregate、policy 和完整 history DDL。
7. **single 验证不足**：`SingleApplicationTest` 只检查入口类是否有 `@SpringBootApplication`，没有启动真实 ApplicationContext。single POM 已包含 Agentic，但 README 仍写只组合 Auth/Data/Manager；没有现成 single compose 拓扑供 Gateway/Web 重复验证。
8. **Podman 默认不受保证**：Makefile 当前先探测 `docker compose`，再探测 `podman compose`。本项目规则要求 Podman，本轮必须显式传 `COMPOSE='podman compose'`，并修正默认选择及对应门禁，避免静默使用 Docker。
9. **文档/代码语义漂移**：`dc3-e2e/README.md` 对真实服务覆盖的描述高于当前实现；`docs/README.md`、旧数据库/TSDB 选型文档和 single README 仍有过时表述。代码、可执行配置和文档必须一起校正。

### 3.2 必须审计而不能预判通过的风险

- 所有 mutable entity 是否都有 `version`，update/delete 是否同时检查 `tenant_id + id + version + deleted=0`。
- `R2dbcGroupBindStore`、`R2dbcLabelBindStore` 等压成少量超长行的实现是否遗漏 version、错误映射或异常语义；先补测试，再按项目风格重写为可审查代码。
- distributed 是否存在服务绕过 facade 直接读取其他业务 Schema。
- 所有服务共用数据库 owner 账号是否违反最小权限；目标应为 init/DDL 账号与 runtime 账号分离，distributed 每域独立最小权限角色，single 使用权限并集角色。
- `00` 后直接进入 `02` 的 init SQL 编号是否是有意设计；不得凭文件名猜测缺少 `01`，应根据 DDL 依赖图验证。
- `public.dc3_platform_lock` 等运行时表是否有生产 owner 和真实调用方；无调用表应删除，有调用表必须纳入实库测试。

## 4. Claude 执行协议

### 4.1 工作方式

1. 先读设计、DDL、Store、controller、facade、Gateway route、Web API wrapper 和 CLI contract，再写测试。
2. 每发现一个 bug，先提交能稳定失败的最小自动化测试，再修实现；不得只加日志、timeout 或 catch 后吞错。
3. 不得通过关闭 fingerprint、放宽租户条件、移除断言、增加 retry、跳过测试或 mock Repository 让门禁变绿。
4. 不得 `git reset`、`git clean`、覆盖用户未提交改动或读取旧 `target` 报告冒充新结果。
5. 一个阶段全部通过后立即进入下一阶段；失败时停止后续阶段，修复并重跑当前阶段及受影响的先前门禁。
6. 新增测试必须进入标准命令和 CI，不允许只留下临时脚本或人工步骤。
7. 所有测试数据使用 `e2e_<run-id>_*` 或独立租户动态创建，结束后清理；破坏性 sweep 只运行在 disposable 数据集。
8. 密钥、密码、token、连接串写入证据前脱敏；不得把真实模型 API key 提交到仓库。

### 4.2 证据目录

每轮创建独立证据目录，建议放在已被 Git 忽略的 Maven `target` 下：

```bash
export RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)"
export EVIDENCE_DIR="$PWD/dc3-e2e/target/validation/$RUN_ID"
mkdir -p "$EVIDENCE_DIR"/{commands,logs,junit,sql,openapi,playwright,coverage,performance}
```

每条门禁至少保存：

- 完整命令和 UTC 开始/结束时间。
- exit code、stdout、stderr。
- Maven Surefire/Failsafe XML、Vitest JSON/JUnit、Playwright HTML/JUnit、覆盖率报告。
- `podman ps`、`podman inspect`、`podman logs`、镜像 digest、compose resolved config。
- 关键 SQL 的原始输出。
- 失败截图、trace、请求 ID、脱敏后的请求/响应。
- 修复前失败证据和修复后通过证据。

禁止使用文件修改时间无法证明属于本轮的报告。已删除的 `dc3-tsdb-tck` 目录若残留旧 `target`，必须明确忽略；其历史 Timescale/Influx/IoTDB/TDengine 报告不属于当前 Maven reactor，也不是本轮证据。

### 4.3 状态定义

- `PASS`：本轮自动化断言通过，原始证据存在。
- `FAIL`：断言失败、服务异常退出、日志出现未预期错误或结果与设计不一致。
- `SKIP`：只有平台物理能力确实不适用时才能使用，并写明负责人、原因和关闭条件；任一必测项 `SKIP` 阻断发布。
- `NOT_RUN`：未执行，阻断发布。

## 5. 连续执行阶段

阶段顺序是依赖图，不得挑着执行。Stage 0 到 Stage 14 全绿后才能进入最终报告。

### Stage 0：保护现场与建立干净证据基线

#### 操作

```bash
git status --short
git rev-parse HEAD
java -version
mvn -version
node --version
pnpm --version
podman version
podman info
podman machine inspect
podman compose version
podman ps --all
podman volume ls
```

- 保存工作树状态；当前大量改动属于迁移工作，禁止擅自 reset/revert。
- 运行当前 reactor 的 `mvn clean`，确保后端编译和报告不复用旧产物。
- 不扫描已不在 POM 中的孤儿 `target` 作为测试结果。
- 记录当前占用端口和容器；不得直接复用或删除已有 `dc3-postgres`、`dc3-rabbitmq` 或其他项目容器。
- 为本轮创建独立 compose project、容器名、volume 和端口 override；该 override 必须进入仓库并可由 CI 重复执行。

#### Podman 硬门禁

所有 Make compose 命令显式使用：

```bash
make config STACK=db COMPOSE='podman compose'
make config STACK=dev COMPOSE='podman compose'
make config STACK=app COMPOSE='podman compose'
```

Testcontainers 必须连接 Podman API socket。macOS 下先从 `podman machine inspect` 动态取得 socket，禁止硬编码用户临时目录：

```bash
export DOCKER_HOST="unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

如果 Testcontainers 无法连接 Podman，应修复测试基础设施；不得改连 Docker daemon。证据中用容器 ID 交叉比对 `podman ps` 与 Testcontainers 日志，证明实际运行时是 Podman。

#### 通过条件

- 源码现场已记录，证据目录为空白新建。
- dedicated validation 拓扑不会触碰开发者已有容器/卷。
- compose 三份配置可由 Podman 解析。
- Testcontainers 的真实后端已证明是 Podman。

### Stage 1：架构与静态白盒门禁

#### 先运行现有门禁

```bash
make validate-postgres-init
make validate-schema-fingerprint
make validate-r2dbc-migration
make validate-documentation
make validate-annotations
make validate-logging
mvn -s .mvn/settings.xml -q -DskipTests compile
```

#### 增加并执行以下自动化门禁

1. **fingerprint 单一事实源**：canonical SQL hash、`08-runtime` seed、compose、env 模板和运行时期望值完全一致；任一 SQL 改动但未刷新所有产物时 CI 失败。
2. **数据格式门禁**：业务 ID/FK 不再出现 `BIGINT/Long/int64/number`；绝对时间不再使用 `LocalDateTime`；HTTP UUID 为字符串且时间为 UTC `Z`。
3. **legacy 协议门禁**：生产代码不得出现旧 `R<T>`、`records/current/pages/size`、旧 gRPC page envelope、平台 `jdbc:` 或旧数据库 selector。
4. **响应式门禁**：除明确 allowlist 的启动验证外，不得出现 `.block()`；不得在业务链路手工 `.subscribe()`；关系持久化不得使用 JDBC/MyBatis。
5. **Repository 清单门禁**：动态扫描所有 `R2dbc*.java` 持久化组件，与实库 contract test manifest 做集合相等断言；新增 Store 未加实库测试时 CI 失败。
6. **DDL owner 门禁**：每张表、view、materialized view、policy 有唯一业务 owner；无主表和无调用表失败。
7. **跨域访问门禁**：distributed 业务代码只通过 facade 跨域，不得直接 SQL 查询其他业务 Schema；显式只读投影必须在架构清单中批准。
8. **DB 权限门禁**：runtime 角色无 `CREATE/DROP/ALTER`，每域角色不得写其他域 Schema；single 角色只拥有四域运行所需权限并与 init owner 分离。

#### 通过条件

- 所有静态计数为 0，或只剩明确的南向 driver/JDBC allowlist。
- 第 3.1 节已知阻断问题都有失败测试，并已修复。
- Store、表、endpoint、页面、CLI 命令四份 coverage manifest 已生成，后续阶段逐项填充。

### Stage 2：生产 PostgreSQL 镜像完整冷启动

#### 测试夹具要求

先增加专用 validation compose override，至少覆盖：

- 独立 project name、container name、volume、network 和 host ports。
- 从当前源码构建 `dc3/dependencies/postgres/Dockerfile`，不得直接复用三周前的本地镜像。
- 保存最终 resolved compose 和镜像 digest。
- healthcheck 只表示可连接，不替代 DDL 断言。

`dc3-postgres` 必须从空 disposable volume 启动。以下操作会删除测试卷，只能对带本轮 `RUN_ID` 的专用 project 执行，并在执行前确认 project/volume 名称；不得对开发或生产卷执行：

```bash
# WARNING: 仅示意；必须替换为本轮专用 validation compose 文件和 project。
podman compose -p "dc3-r2dbc-$RUN_ID" \
  -f dc3/docker-compose-db.yml \
  -f dc3-e2e/compose/validation-db.override.yml down -v
```

#### 必查 SQL

通过 `podman exec ... psql` 保存以下事实，而不是只看启动日志：

```sql
SHOW server_version;
SHOW timezone;

SELECT extname, extversion
FROM pg_extension
WHERE extname IN ('timescaledb', 'vector', 'age')
ORDER BY extname;

SELECT schema_name
FROM information_schema.schemata
WHERE schema_name IN (
  'dc3_auth', 'dc3_manager', 'dc3_data',
  'dc3_history', 'dc3_agentic', 'public'
)
ORDER BY schema_name;

SELECT fingerprint_version, ddl_hash, schema_contract,
       id_format, time_format, json_format
FROM public.dc3_schema_fingerprint;

SELECT hypertable_schema, hypertable_name
FROM timescaledb_information.hypertables
ORDER BY hypertable_schema, hypertable_name;

SELECT view_schema, view_name, materialized_only
FROM timescaledb_information.continuous_aggregates
ORDER BY view_schema, view_name;

SELECT application_name, schedule_interval, config
FROM timescaledb_information.jobs
ORDER BY application_name, job_id;
```

另外导出并断言：

- 全部表、列类型、默认值、`NOT NULL`、PK、FK、unique、check、partial index。
- 业务 ID/FK 均为 UUID，绝对时间均为 `TIMESTAMPTZ`。
- `dc3_point_value` hypertable、compression、retention、1m/1h continuous aggregate 和 refresh policy 正确。
- AGE 可 `LOAD 'age'`，pgvector 可创建/查询 vector，Timescale 可写入/聚合。
- seed data 的租户、认证、菜单、资源关系完整，所有 FK 可满足。
- init SQL 重复执行策略明确：仅首次空卷初始化；应用启动不得隐式改 Schema。

#### 生命周期组合

1. 空卷冷启动，所有 init SQL 按实际文件顺序执行。
2. 停容器但保留卷，再启动；fingerprint 和数据不变，init 脚本不重复破坏数据。
3. 强制终止后重启，PostgreSQL 完成恢复且约束/数据一致。
4. 使用错误 fingerprint 启动任一 center，必须在 readiness 前 fail-fast。
5. 删除 fingerprint 行、改变 contract/format、缺少业务表分别启动，必须给出可定位错误并拒绝流量。

#### 通过条件

- `00`、`02`、`03`、`04`、`05`、`06`、`07`、`08` 的全部实际效果都有 SQL 证据。
- 冷启动、持久卷重启和恢复启动均通过。
- 任何部分初始化或错误 contract 都不会让 center 进入 ready。

### Stage 3：全量 R2DBC 实库 contract matrix

#### 数据库矩阵

同一组 Repository contract 至少执行两次：

| 代号 | 数据库 | 用途 |
| --- | --- | --- |
| DB-PROD | 当前源码构建的完整 `dc3-postgres` | 发布事实源；必须覆盖全部 DDL/扩展 |
| DB-UPSTREAM | `timescale/timescaledb-ha:pg18` + contract 所需 canonical DDL | 独立交叉验证 SQL/R2DBC 行为；不冒充 AGE/pgvector/observability 完整验证 |

DB-UPSTREAM 不能继续只加载裁剪版 history 后宣称生产初始化通过。若某 contract 必须依赖生产扩展，只在 DB-PROD 执行并在矩阵中明确标记，不允许静默 skip。

#### 每个 Store 的最低测试集

- insert 后 get，数据库生成/应用生成 ID 的类型、版本和时间正确。
- list 的全部过滤条件、默认排序、复合排序和稳定 tie-breaker。
- update 成功时 version 原子递增；旧 version 并发更新只有一个成功。
- delete 使用租户 + ID + version，软删除后所有普通 get/list/聚合不可见。
- 租户 A 的 ID、关联 ID、cursor、idempotency key 不能读取或修改租户 B 数据。
- unique、FK、check、not-null 冲突映射为稳定领域错误和 RFC 9457，不泄露 SQL/表名/密码。
- nullable 字段通过 `bindNull`；空字符串与 null 的领域语义不混淆。
- JSONB 嵌套对象、数组、Unicode、数字、布尔、null 往返等价，canonical 输出稳定。
- `Instant` 在数据库微秒精度往返，非 UTC 输入归一为同一个瞬间。
- 事务中后一步失败时前序写入回滚；取消信号不留下半完成状态。
- 连接和 statement 在成功、失败、取消三条路径全部释放。

#### offset 分页并发测试

每个含 `OffsetPage` 的 Store 都要在真实 PostgreSQL 上验证：

1. 在 count 和 items 之间并发 insert/delete，结果仍来自同一 repeatable-read snapshot。
2. 100 个相同主排序值连续翻页，无重复、无遗漏。
3. `offset=-1`、`limit=0/201`、加法溢出、重复排序字段、未知排序字段全部拒绝。
4. 排序字段注入字符串无法进入 SQL。
5. count/items 不使用 `Mono.zip` 跨连接并发，事务日志证明使用同一连接和 `REPEATABLE READ`。

#### cursor 分页测试

- 相同时间戳下按完整复合键翻页，无重复、无遗漏。
- cursor 被修改、过期、跨租户、跨 endpoint、改变过滤条件后重放全部拒绝。
- 首末页、空页、恰好等于 limit、limit+1 正确设置 `nextCursor/hasNext`。
- cursor 不暴露数据库 offset、密钥、租户原值或可篡改 JSON。
- 在高并发新增记录时，已建立的游标序列语义明确并由测试固定。

#### 辅助持久化组件

除附录 A 的 62 个 `R2dbc*Store` 外，至少覆盖：

- `R2dbcPointValueIngestOutbox`
- `R2dbcPointCommandContext`
- `R2dbcRuleStateLookup`
- `R2dbcOperationRepository`
- `R2dbcPageExecutor`
- `R2dbcCursorExecutor`
- `SchemaFingerprintVerifier` / startup validator
- `SpringR2dbcPageTransaction`
- DDL 中所有 lock、lease、operation、outbox runtime table 的真实 owner

#### 通过条件

- 动态 Repository manifest 100% 对应实库 contract；不能只按类名数量判断。
- DB-PROD 全绿；DB-UPSTREAM 的适用 contract 全绿且无未解释 skip。
- tenant、CAS、事务、JSONB、UUID、时间、offset/cursor 均有并发和负向证据。

### Stage 4：TimescaleDB 与点值全链路

围绕 `R2dbcTsdbStore`、latest projection、ingest outbox、RabbitMQ consumer 和 dashboard aggregate 执行：

1. 单条、批量 500、突发 5000 条写入；重复 `message_id` 幂等。
2. 同一 device/point、相同 `device_time` 的多条值用完整复合键确定顺序。
3. 迟到数据、未来时间、乱序、重复、非 numeric 值、极大/极小 numeric 值。
4. tenant/device/point 交叉隔离；不存在的关联 ID 不产生孤儿数据。
5. latest 只被更新事件推进，旧事件不能覆盖新值。
6. history cursor 全量遍历与直接 SQL 主键集合相等。
7. 1m/1h continuous aggregate 的 count/min/max/sum/avg/first/last 与 raw SQL 交叉验证。
8. real-time aggregate 可见尚未 materialize 的最新 tail。
9. compression、retention、refresh policy 存在且配置符合 DDL；测试环境不得等待真实 7/30/365 天，应通过受控时间数据和显式 policy job 验证。
10. consumer 在 commit 前失败不 ack，重放不重复；outbox owner fence 和 backoff 正确。
11. DB/RabbitMQ 短暂中断后恢复消费，不丢、不乱确认、不形成无限内存队列。

通过条件是 API/Store 结果、raw table、latest table、aggregate view 和消息确认五方可相互解释，不能只断言 HTTP 200。

### Stage 5：single center 真实拓扑

#### 目标拓扑

single 的发布目标按当前 POM 定义为一个进程包含 Auth、Manager、Data、Agentic：

- HTTP `8100`，base path `/single`。
- gRPC `9100`。
- Auth/Manager/Data facade 使用 local mode。
- 单一 R2DBC pool 和 transaction manager。
- 外部依赖为 PostgreSQL/TimescaleDB、RabbitMQ，以及 Agentic 的确定性 OpenAI-compatible 测试服务。

#### 必须先补齐的基础设施

- 为 `dc3-center-single` 增加可重复的 Podman validation compose service；镜像使用 Dockerfile 的 `dc3-center-single` target。
- 增加真实 `@SpringBootTest`/容器启动测试，不能只检查注解。
- 修正 README 对 Agentic 范围的描述。
- 验证全部 controller mapping 无冲突，尤其 Manager/Data 共享 `/dashboard` 前缀时的完整 method path。
- 为 Gateway 增加 single route profile/override，使 `/api/v3/auth|manager|data|agentic/**` 正确转发到 `/single/**`；不得靠手工临时代理。

#### 必测内容

- readiness、liveness、OpenAPI 文档、资源注册完整。
- Auth 登录后可依次创建 tenant/user/role、Manager 资源、Data rule/history、Agentic session。
- local facade 传播 request ID、tenant、principal、权限和错误语义。
- 全业务流只建立一个应用 R2DBC pool；无重复 ConnectionFactory、JDBC datasource 或跨 facade HTTP 自调用。
- Agentic tool 通过 local facade 查询 Manager/Data，与直接业务 API 结果一致。
- 停止 single 后 Gateway 返回可诊断 503；重启后恢复且已有数据库状态可读。

#### 通过条件

single + Gateway + Web + CLI 能完成 Stage 8/9 的同一套用例，不是只测 `/actuator/health`。

### Stage 6：distributed 多服务真实拓扑

#### 启动顺序

使用专用 Podman validation project，按依赖连续启动：

1. PostgreSQL/TimescaleDB + RabbitMQ。
2. Auth `8300/9300`。
3. Manager `8400/9400`。
4. Data `8500/9500`。
5. Agentic `8600`。
6. Gateway `8000`。
7. Web `8080`。

以下 readiness 全部成功后才开始业务测试：

```text
http://localhost:8300/auth/actuator/health/readiness
http://localhost:8400/manager/actuator/health/readiness
http://localhost:8500/data/actuator/health/readiness
http://localhost:8600/agentic/actuator/health/readiness
http://localhost:8000/actuator/health/readiness
http://localhost:8080/
```

#### 数据库部署组合

distributed 至少执行两套：

| 组合 | 说明 | 验证目标 |
| --- | --- | --- |
| DIST-SHARED | 四个 center 共享一个完整 PostgreSQL 实例，各用最小权限域账号 | 当前生产拓扑、Schema 隔离 |
| DIST-ISOLATED | Auth、Manager、Data、Agentic 各连独立完整 PostgreSQL 实例 | 证明跨域只走 gRPC facade，不存在共享库暗耦合 |

DIST-ISOLATED 不是新增运行时数据库类型；四个实例仍是完全相同的 PostgreSQL 18/TimescaleDB 构件。若服务因直接跨 Schema SQL 无法运行，视为架构 bug，改为 facade，不得把测试删掉。

#### 必测内容

- gRPC facade 的 deadline、取消、tenant/principal metadata、NOT_FOUND/INVALID_ARGUMENT/ALREADY_EXISTS 映射。
- Auth→Manager→Data→Agentic 的完整依赖链与反向错误传播。
- Gateway route、StripPrefix、public token route 顺序、Authentic filter、OpenAPI aggregation。
- direct center HTTP 与 Gateway HTTP 的成功资源和 Problem Details 语义一致。
- 每个 runtime DB role 尝试跨域写入必须被数据库拒绝。
- 任一 service 滚动重启期间其他 service 不出现永久缓存脏读或无限 retry。

#### 通过条件

DIST-SHARED 和 DIST-ISOLATED 都完成 Stage 8 的业务矩阵；不能只启动容器后判绿。

### Stage 7：全模块业务白盒 + HTTP 黑盒

测试用例必须从运行时 OpenAPI 枚举 endpoint，并与 controller manifest 做集合对比。每个 endpoint 至少有授权成功、无权限、错误输入和关键领域失败用例。

#### Auth

- salt、login、check、logout/cancel、改密、错误密码、禁用用户、过期 token。
- tenant、principal、user profile、local credential、membership、role、resource、menu、API、service account CRUD。
- role-principal、role-resource 绑定和权限缓存失效。
- 两租户同名数据、跨租户 ID 猜测、token 租户切换。
- identity audit cursor、排序、过滤和不可篡改性。
- OAuth client、consent、JWKS、MCP connection/tool/confirmation、resource registry 聚合。
- 密码、secret、token 不出现在响应、Problem Details、应用日志或 SQL 证据。

#### Manager

- Driver、Profile、Point、Device 的完整创建依赖链。
- Command/Event 及 param/attribute/config 的 CRUD 和级联约束。
- Driver attribute、Group/Label 及 bind 的租户隔离、唯一约束、CAS。
- dictionary/list options 使用新分页协议。
- dashboard 统计与 SQL 直接聚合一致。
- device import 返回 `202 OperationAccepted`，轮询/监听到完成、失败、取消；幂等 key 不重复创建任务。
- stale version update/delete 返回 409 Problem Details，Web/CLI 能展示而不是覆盖新数据。

#### Data

- point latest/history、command submit/history、event history、entity state。
- rule/rule state、alarm 触发/确认/批量确认、notify config/history/admin。
- dashboard stats、alert analytics、health projection。
- RabbitMQ 真实消息进入 outbox、raw history、latest、rule、alarm 的完整链路。
- 重复消息、乱序消息、死信、重放和租户错误消息。

#### Agentic

- model provider/config、session、message、action、attachment 的真实 PostgreSQL CRUD。
- 使用本地确定性 OpenAI-compatible server 返回固定 token/tool-call/error，不调用不稳定外部模型作为发布门禁。
- SSE/NDJSON 首 token、完整 token 序列、finish reason、tool result 和最终 message 落库一致。
- 客户端取消、Gateway 断连、模型 4xx/5xx/timeout 分别落为 cancelled/failed，不能误记 completed。
- tool 通过 Auth 权限和 Manager/Data facade；高风险工具确认票据一次性、过期、跨租户不可用。
- attachment 路径穿越、非法类型、超限和孤儿清理。

#### Gateway

- public login 只命中 `auth_route_token`，其他 auth/manager/data/agentic route 必须认证。
- 缺 token、坏 token、过期 token、无权限、下游 4xx/5xx 使用 RFC 9457。
- request ID/trace ID 跨 HTTP→gRPC→日志一致。
- SSE 不缓存整流、不改 chunk 顺序，客户端取消传播到 Agentic 和模型端。
- `/v3/api-docs/*` 与运行时 endpoint 集合一致，不含旧 envelope/schema。

### Stage 8：single 与 distributed 语义交叉验证

同一动态 fixture 和业务脚本分别跑在 SINGLE、DIST-SHARED、DIST-ISOLATED。对响应做规范化后比较：

- 忽略本轮动态 UUID、时间、request ID、端口等非语义字段。
- 比较状态码、content type、字段集合、枚举、排序、分页、错误 type/title/status/code。
- 比较最终数据库业务状态和 RabbitMQ 可观察结果。
- 比较 local facade 与 gRPC facade 的空值、NOT_FOUND、冲突和取消语义。

必须覆盖以下连续场景：

1. 登录并创建独立租户。
2. 创建用户/角色/权限并重新登录。
3. 创建 Driver→Profile→Point→Device→Command/Event。
4. 写入点值，查询 latest/history/dashboard。
5. 提交 command、报告 event、触发并确认 alarm。
6. 创建 Agentic provider/config/session，流式对话并调用真实 facade tool。
7. 对所有列表执行 offset/cursor 翻页和非法旧分页请求。
8. 以 stale version 更新/删除并校验 409。
9. 跨租户重复执行 get/list/update/delete/cursor 重放并校验隔离。

任一拓扑需要特殊兼容字段或特殊业务分支才能通过，视为失败。

### Stage 9：Web 与 CLI 真实后端黑盒

#### Web 静态和组件门禁

```bash
cd dc3-web
pnpm check
pnpm lint:check
pnpm test:guard
pnpm test:ci
pnpm build
```

#### Playwright

对 SINGLE 和 distributed Gateway 分别执行全部 spec：

```bash
cd dc3-web
E2E_BASE_URL=http://localhost:8080 \
E2E_START_SERVER=0 \
pnpm test:e2e
```

`test:e2e:sweep` 包含破坏性删除检查，只能在本轮 disposable tenant/dataset 执行：

```bash
# WARNING: 确认目标是 disposable validation 环境后执行。
E2E_BASE_URL=http://localhost:8080 \
E2E_START_SERVER=0 \
pnpm test:e2e:sweep
```

必须补足：

- 运行时路由与页面 manifest 集合相等，每个可访问页面至少打开一次且无 console error、Vue warn、失败网络请求。
- login/logout、权限菜单、直接 URL、刷新、401 回登录、403 展示。
- 每个 CRUD 页的新增、编辑、详情、删除、stale version 冲突。
- offset 页的 total/next/排序，cursor 页的前进/返回栈；前端不再发旧 `page/size`。
- UUID 始终作为 string，不发生 JavaScript `Number` 精度转换。
- UTC 时间跨时区展示正确；浏览器分别用 UTC、Asia/Shanghai、America/New_York 执行关键断言。
- Agentic 流逐 token 展示，取消按钮真实终止后端流，不继续计费/落 completed。
- RFC 9457 错误详情可读，不弹出 `[object Object]`，不泄露内部 SQL。
- desktop/tablet/mobile 关键页面与长 UUID、长错误文案布局。

#### CLI

先执行：

```bash
pnpm --dir dc3-cli build
pnpm --dir dc3-cli test
```

再用构建后的真实 `dc3` 命令连接两个拓扑的 Gateway，覆盖：

- config、login、身份/tenant 状态、logout。
- Auth/Manager/Data/Agentic 所有已发布 command surface。
- offset/cursor 多页遍历、JSON 输出和 table 输出。
- UUID string、RFC 3339 时间、stdin/file 参数。
- stale version、Problem Details、网络中断、SSE cancel 的 exit code 和 stderr。
- CLI 与同一 HTTP 请求的语义结果一致。

单元测试中的 mocked fetch 不能替代这组 CLI 黑盒。

### Stage 10：故障注入与恢复

所有故障只作用于本轮 Podman validation project，并保存故障前、中、后日志与请求结果。

| 故障 | 预期 |
| --- | --- |
| PostgreSQL stop/restart | readiness 下降；请求快速失败；恢复后 pool 重连且无数据损坏 |
| PostgreSQL 强制终止 | WAL 恢复后 fingerprint/约束/数据一致 |
| RabbitMQ stop/restart | 未确认消息重投；幂等；无无限内存堆积 |
| Auth stop | Gateway auth 路由 503；其他服务不绕过认证 |
| Manager/Data stop | facade deadline 生效；无永久线程/连接占用 |
| Agentic stop | SSE 结束为可诊断错误；Gateway 不伪造成功 |
| Gateway stop/restart | Web/CLI 明确网络错误，恢复后无需清空业务数据 |
| 客户端取消 | DB query、gRPC、SSE、模型流收到取消并释放资源 |
| pool exhaustion | `max-acquire-time` 生效；无死锁；恢复后连接数回落 |
| query timeout | transaction 回滚，Problem Details 稳定，不泄露 SQL |
| 错 fingerprint/contract | center 启动失败且永不 ready |
| 错 R2DBC URL/凭证 | fail-fast，不能退回内存库/JDBC |

MQ consumer 额外执行多实例、rebalance、zero-assignment、重复投递和 leader 切换测试。不得用加大 timeout 掩盖竞态。

### Stage 11：性能、资源与背压

先测基线，再报告 P50/P95/P99、吞吐、错误率、CPU、RSS、heap、GC、event-loop、R2DBC active/acquire/pending、PostgreSQL connection/query 和 RabbitMQ queue 指标。没有经产品确认的 SLA 时不得编造固定毫秒门槛，但以下正确性门槛是强制的：

- 并发结束后连接、线程、subscription 和队列长度回到稳定区间，无持续增长。
- 5000+ 点值 burst 不 OOM、不静默丢数据，最终数据库集合与输入去重集合一致。
- 100 个并发 CAS 对同一记录只能有一个指定 version 成功，其余稳定返回 conflict。
- 100 个并发租户互不串数据，cache key 含 tenant。
- offset 深页保存 `EXPLAIN (ANALYZE, BUFFERS)`；高基数列表若深 offset 成本不可控，应改为 cursor，而不是增加数据库资源。
- cursor 逐页耗时不随已翻页数量线性退化。
- SSE 慢消费者遵守背压/限流策略，取消后生产端停止；不得无限 buffer。
- Agentic 长对话、附件和 tool result 有明确上限并被负向测试。

压力测试至少各执行三轮，首轮冷、后两轮热；报告原始值和方差，不只给平均值。

### Stage 12：拒绝性与旧协议清除测试

以下配置/请求必须失败，且不得兼容运行：

- `jdbc:*` 作为平台数据源。
- `r2dbc:mysql:*`、非 PostgreSQL R2DBC driver。
- `DC3_DB_TYPE`、`DC3_TSDB_TYPE`、旧 relation/TSDB selector。
- 缺失或空 `spring.r2dbc.url`。
- 同时存在两个 `ConnectionFactory`、两个 `R2dbcDialect` 或 JDBC `DataSource` bean。
- fingerprint 缺失、格式非法、hash 错、contract 错、data format 错。
- HTTP 旧 `R<T>` 假定、`response.data.records`、旧分页字段。
- gRPC 旧 page/envelope message。
- UUID 作为 JSON number、非法 UUID、nil UUID、非 v7 业务 ID。
- 无时区绝对时间、纳秒超过数据库约定精度、非法 JSONB。

静态门禁不得误杀南向 SQL driver 和 driver 本地 SQLite buffer，但必须证明这些 JDBC 路径不会成为 center 平台持久化 bean。

### Stage 13：全量回归与标准命令

在所有新增测试已经接入标准生命周期后，从 clean 连续执行：

```bash
mvn -s .mvn/settings.xml clean test
make test-it
make test-e2e
make coverage

cd dc3-web
pnpm lint:check
pnpm check
pnpm test:guard
pnpm test:ci
pnpm build

cd ..
pnpm --dir dc3-cli build
pnpm --dir dc3-cli test
```

要求：

- `make test-e2e` 此时必须启动/连接真实 center、Gateway 和基础设施，不再只是 harness 自测。
- Surefire 与 Failsafe 报告分开汇总，不能漏掉 `*IT`。
- 所有 skip 逐条列出；required E2E、Repository contract、Playwright 和 CLI black-box 为 0 skip。
- 覆盖率不是唯一标准；Store/endpoint/page/CLI command 的行为 manifest 必须 100% `PASS`。
- 重跑静态门禁，确保修测试期间没有重新引入 legacy 代码。

### Stage 14：制品级 Podman 最终验收

最后一轮必须使用即将发布的镜像，而不是 IDE JVM：

1. 从当前 commit/worktree 构建数据库、single、四 center、Gateway、Web 镜像。
2. 保存所有镜像 digest 和 SBOM/依赖报告（若仓库已有生成入口则使用现有入口，不新增无关工具链）。
3. 空卷分别启动 SINGLE 和 distributed release compose。
4. 重跑 Stage 8 核心业务、Stage 9 Playwright/CLI、Stage 10 关键重启用例。
5. 扫描所有容器日志：无 schema mismatch、blocking call、connection leak、unhandled error、明文 secret。
6. 停止并重启整套 Podman project，验证持久卷状态；最后只删除本轮 disposable project。

## 6. 发布硬门禁

只有同时满足以下条件才能写“迁移全面通过”：

- [ ] 第 3.1 节全部已知阻断问题有复现测试和根因修复。
- [ ] 生产数据库空卷完整初始化及持久卷重启通过。
- [ ] 所有生产 R2DBC 持久化组件有真实 DB contract，manifest 100% PASS。
- [ ] Auth、Manager、Data、Agentic 全 endpoint 矩阵通过。
- [ ] SINGLE、DIST-SHARED、DIST-ISOLATED 三套业务结果交叉一致。
- [ ] Gateway route、认证、Problem Details、SSE cancel 通过。
- [ ] Web 全页面和 CLI 全命令连接真实后端通过。
- [ ] offset/cursor 的并发、一致快照、签名、租户隔离和旧协议拒绝通过。
- [ ] UUID、Instant、JSONB 契约在 DB→Java→gRPC→HTTP→Web/CLI 全链路一致。
- [ ] DB/RabbitMQ/service 故障与恢复通过，无连接/线程/消息泄漏。
- [ ] 标准 test/IT/E2E/coverage/build/compose 门禁全部 exit code 0。
- [ ] required suites 为 0 failure、0 error、0 skip；其他 skip 全部有书面理由。
- [ ] 最终制品镜像在 Podman 空环境和持久卷重启后均通过。

## 7. 最终报告模板

Claude 完成后在证据目录生成 `validation-report.md`，结构固定如下：

```markdown
# IoT DC3 R2DBC flag-day validation report

## Verdict
PASS / FAIL

## Source and environment
- Git HEAD / worktree diff hash:
- UTC run window:
- OS / Java / Maven / Node / pnpm:
- Podman client/server:
- Image digests:

## Known blockers resolution
| ID | Reproduction test | Root cause | Fix | Result | Evidence |

## Stage results
| Stage | Topology/DB | Passed | Failed | Skipped | Duration | Evidence |

## Persistence coverage
| Component | DB-PROD | DB-UPSTREAM | Tenant | CAS | Tx | JSONB | Time/UUID | Evidence |

## API coverage
| Service | Endpoint | Method | Success | Auth | Validation | Conflict | Tenant | Evidence |

## Client coverage
| Web route / CLI command | SINGLE | DIST-SHARED | DIST-ISOLATED | Evidence |

## Fault and performance results
| Scenario | P50 | P95 | P99 | Throughput | Errors | Recovery | Evidence |

## Skips and residual risks
- Every skip with owner and closure condition.
- Any blank row makes Verdict FAIL.

## Final gate commands
| Command | Exit code | Tests | Failures | Errors | Skipped | Evidence |
```

报告必须附上失败证据，不能只给最终绿色截图；如果仍有空白覆盖、未解释 skip、依赖人工点测或只在一种拓扑通过，Verdict 必须是 `FAIL`。

## 附录 A：当前 62 个 `R2dbc*Store` 基线清单

此清单用于启动覆盖审计，最终以 Stage 1 动态扫描结果为准；新增、删除或重命名后必须同步 contract manifest。

### A.1 Auth（19）

- `R2dbcApiStore`
- `R2dbcAuditLogStore`
- `R2dbcLocalCredentialStore`
- `R2dbcMcpCatalogStore`
- `R2dbcMcpRuntimeStore`
- `R2dbcMenuStore`
- `R2dbcPermissionStore`
- `R2dbcPrincipalStore`
- `R2dbcResourceLookupStore`
- `R2dbcResourceRegistryStore`
- `R2dbcResourceStore`
- `R2dbcRolePrincipalBindStore`
- `R2dbcRoleResourceBindStore`
- `R2dbcRoleStore`
- `R2dbcServiceAccountStore`
- `R2dbcTenantDictionaryStore`
- `R2dbcTenantMembershipStore`
- `R2dbcTenantStore`
- `R2dbcUserStore`

### A.2 Manager（23）

- `R2dbcCommandAttributeConfigStore`
- `R2dbcCommandAttributeStore`
- `R2dbcCommandParamStore`
- `R2dbcCommandStore`
- `R2dbcDashboardStore`
- `R2dbcDeviceImportJobStore`
- `R2dbcDeviceStore`
- `R2dbcDriverAttributeConfigStore`
- `R2dbcDriverAttributeStore`
- `R2dbcDriverLeaseStore`
- `R2dbcDriverStore`
- `R2dbcEventAttributeConfigStore`
- `R2dbcEventAttributeStore`
- `R2dbcEventParamStore`
- `R2dbcEventStore`
- `R2dbcGroupBindStore`
- `R2dbcGroupStore`
- `R2dbcLabelBindStore`
- `R2dbcLabelStore`
- `R2dbcPointAttributeConfigStore`
- `R2dbcPointAttributeStore`
- `R2dbcPointStore`
- `R2dbcProfileStore`

### A.3 Data（14）

- `R2dbcAlertAnalyticsStore`
- `R2dbcAlertStore`
- `R2dbcCommandHistoryStore`
- `R2dbcEntityAlarmStore`
- `R2dbcEntityStateStore`
- `R2dbcEventHistoryStore`
- `R2dbcNotifyAdminStore`
- `R2dbcNotifyConfigStore`
- `R2dbcNotifyHistoryStore`
- `R2dbcPointCommandStore`
- `R2dbcPointValueLatestStore`
- `R2dbcRuleStateStore`
- `R2dbcRuleStore`
- `R2dbcTsdbStore`

### A.4 Agentic（6）

- `R2dbcActionStore`
- `R2dbcAttachmentStore`
- `R2dbcMessageStore`
- `R2dbcModelConfigStore`
- `R2dbcModelProviderStore`
- `R2dbcSessionStore`

## 附录 B：当前 canonical init SQL

按当前文件系统实际存在的顺序验证，不引用旧文档中的假定文件：

1. `00-iot-dc3-extensions.sql`
2. `02-iot-dc3-auth.sql`
3. `03-iot-dc3-data.sql`
4. `04-iot-dc3-manager.sql`
5. `05-iot-dc3-history.sql`
6. `06-iot-dc3-agentic.sql`
7. `07-iot-dc3-observability.sql`
8. `08-iot-dc3-runtime.sql`

如果执行过程中调整 DDL，必须重新生成 fingerprint、重建生产数据库镜像、从空卷重跑 Stage 2，并重跑所有受影响 Store、API、Web 和 CLI 测试。
