# 设计：基于 Spring Data R2DBC 的关系访问层（每方言、每服务模块）

|            |                                                                                                                              |
|------------|------------------------------------------------------------------------------------------------------------------------------|
| **状态**   | 已批准 2026-08-28 —— §5 决策已确认（D14 推迟至 P1 TCK spike；D18/D19 在首次评审时增补）                                        |
| **日期**   | 2026-08-28                                                                                                                    |
| **范围**   | 关系持久层：`dc3-common-auth` + `dc3-db` 家族先行；`dc3-common-manager` / `dc3-common-data` 随后跟进                            |
| **目标**   | 用 Spring Data R2DBC 取代 MyBatis-Plus + JDBC；端到端响应式；GraalVM native image 就绪                                         |
| **取代**   | `storage-abstraction.md` §3（关系方言机制——“MyBatis 即 Port”）；其 TSDB 一半已移入 `tsdb-abstraction.md`                        |
| **相关**   | `mq-abstraction.md`、`tsdb-abstraction.md` —— 本设计将该家族模式扩展到关系层                                                    |

## 1. 动机

三个驱动因素，按优先级排序：

1. **GraalVM native image。** `native-maven-plugin` 1.1.10 已在根 POM 中托管版本，但 MyBatis-Plus 挡住了这条路：运行期
   mapper 代理、反射驱动的 bean 装配、基于 jsqlparser 的 SQL 改写，全都与封闭世界（closed-world）编译相抵触。Spring Data
   R2DBC 自带 native 支持（Boot 3 起内置 hints；驱动基于 Netty）。这次依赖替换是每个 center 服务得以 native 构建的前置一步。
2. **真正的端到端响应式。** 控制器今天已是 WebFlux，但 Service → Manager → Mapper 链路是同步阻塞的，靠
   `BaseController.async(...)` 桥接到 `Schedulers.boundedElastic()`，并手工 set/clear `TenantContextHolder`。响应式持久层
   一步移除这座桥、这次线程池跳转和 ThreadLocal 生命周期隐患。
3. **方言自由。** 当前的方言机制是共享 XML 内按 MyBatis `databaseId` 分叉（42 条语句散布在 6 个文件中），靠“可移植优先”
   的纪律把关。R2DBC 没有 `databaseId`；自然的形态是每个方言一个实现模块、承载原生 SQL——与 MQ 和 TSDB 家族已在使用的
   契约/适配器/TCK 结构相同（`dc3-common-repository` 中的 `RepositoryService` Port、`dc3-tsdb-*` 中的适配器、
   `dc3-tsdb-tck` 中的 TCK）。

权重，直说：驱动因素 1 是硬性要求。对 auth 本身而言，驱动因素 2 是一笔架构一致性投资——auth 是管理面（登录/CRUD、人为
驱动的 QPS），移除 boundedElastic 跳换并不宣称带来时延或吞吐收益；它的回报（native image、消除 ThreadLocal 生命周期隐患、
为高并发的 data/manager 浪潮做好准备）要到后面才兑现。代价——把 12 个 service 加上 OAuth/MCP 运行时重写为响应式链路、
调试更难——已被接受，并由 P2/P3 的 go/no-go 闸门、TCK 与 e2e 加以约束。

**不会**改变的部分：经 `dc3.db.type`（postgres | mysql | mariadb）的部署期选择、initdb 种子 SQL（由容器 entrypoint 执行，
与访问层无关）、表结构，以及 HTTP/gRPC 契约面。

## 2. 目标 / 非目标

**目标**

- 鉴权中心(auth)是试点：全面 R2DBC，classpath 上零 MyBatis，native image 可编译、可启动。
- 每个（方言 × 服务）一个模块，承载该方言的原生 SQL——没有共享 XML，没有 `databaseId`。
- Repository 行为 TCK：同一套套件经 Testcontainers 在 PostgreSQL、MySQL、MariaDB 上全绿。
- 租户隔离得以保留，且保证**强于**今天（见 §6）。
- gRPC 契约不变；HTTP 契约不变，**唯独**刻意重设计的分页信封除外（D8——嵌套 `page` 对象；一步切换，无兼容窗口）。

**非目标**

- 数据库引擎的运行期热替换（不变：部署期选择，重启切换）。
- 在本轨中重写 manager/data——它们稍后按同一模式跟进；其 MyBatis 技术栈原封不动。
- 与 MyBatis-Plus 的功能对齐。只重建 auth 实际用到的功能（盘点见 §3）。
- ORM 级的 schema 管理。initdb 保持权威；不引入 Flyway/Liquibase。

## 3. 塑造设计的现状事实

已核实的盘点（2026-08-28），每条事实都约束一项决策：

| # | 事实 | 推论 |
|---|------|-------------|
| F1 | `dc3-common-auth` 的 16 个 Manager 接口中，**15 个是空的**（`extends IService<XxxDO>`，零方法）。只有 `IdentityAuditLogManager` 拥有自定义方法。 | 契约是一张白纸——按 Service 实际需要来定义它，而不是按 `IService`。 |
| F2 | Service 层依赖 `IService` + `LambdaQueryWrapper`（模糊查询、`page(...)`、`getOne(wrapper)`、`wrapper.apply("1 = 0")`、针对无租户表的两步 membership→`IN principalIds` 租户过滤）。 | 动态查询逻辑移入具名 repository 方法，接收既有 Query BO（D7）。 |
| F3 | `OAuthMcpMapper` 是纯手写 mapper（30 个方法，无 `BaseMapper`），被 biz 层**直接注入**（`OAuthMcpRuntimeServiceImpl`），写 7 张表；auth 的全部 `databaseId` 分叉都位于其 XML 中。`ResourceRegistryLockMapper`（咨询锁）同样是手写的。 | 它已经是 Repository 的形状；迁移即 SQL 搬家 + 响应式签名。它是最大的单一聚合（D13）。 |
| F4 | `TenantContextHolder` 基于 ThreadLocal（位于 `dc3-common-constant`）；写入点恰好有 3 类入口：`BaseController.async`、本地 Facade、gRPC 服务器（手工 set/clear）。消费方是 `TenantLineHandlerImpl`（fail-closed：无租户 → `TenantNotScopedException`）。手写 XML SQL 已携带显式 `tenant_id = #{tenantId}`——拦截器只覆盖 wrapper 路径。 | 新契约中的显式租户参数是既有模式的延续，不是新纪律（D5）。ThreadLocal 只留给 MyBatis 时代的模块。 |
| F5 | 11 张表没有 `tenant_id` 列（`TenantLineHandlerImpl` 中的白名单）：`dc3_tenant`、`dc3_principal`、`dc3_user`、`dc3_local_credential`、`dc3_external_identity`、`dc3_resource`、`dc3_role_resource_bind`、`dc3_api`、`dc3_mcp_tool_catalog`、`dc3_mcp_connection_tool`、`dc3_menu`。 | 无租户 repository 在契约中以名称标明；无需运行期白名单（D6）。 |
| F6 | ID 处处都是应用侧雪花 ID：`@TableId(type = ASSIGN_ID)`（60+ 个 DO），OAuth/MCP 路径另有显式 `IdWorker.getId()`。无数据库序列。 | 换成一个小的第一方雪花 ID 生成器；无 schema 变更（D10）。 |
| F7 | 经 `@TableLogic` 的逻辑删除（47 个 DO，`deleted` 标志；形如 `... WHERE deleted = 0` 的部分唯一索引）。 | 实现中写显式 `deleted = 0` 谓词；契约将该语义文档化（D9）。 |
| F8 | `create_time` / `operate_time` 纯属数据库侧（PG 触发器函数 / MySQL `ON UPDATE CURRENT_TIMESTAMP`）；更新时故意把 `operate_time` 置空并回读。 | 插入排除时间列；保留回读模式；零 schema 变更（D11）。 |
| F9 | auth 中有 19 处 `@Transactional(rollbackFor = Exception.class)`（`OAuthMcpRuntimeServiceImpl` 9 处、`ResourceRegistrySyncServiceImpl` 3 处、`MenuServiceImpl` 3 处、`ServiceAccountServiceImpl` 3 处）。另有 `UserController.add/delete` 中一处不受管理的 3 表写（principal → user → membership）——既有缺陷。 | 用 `R2dbcTransactionManager` 做响应式事务；迁移期间修复 UserController 缺口（D12）。 |
| F10 | 分页是统一的一种模式：12 个 ServiceImpl 中 `Page<DO>` + `PageUtil.page(Pages)`；HTTP 返回 MyBatis-Plus `Page<VO>`（泄漏的框架类型——内部字段序列化进 JSON）；gRPC 用 proto `GrpcPage`（仅 data/driver 契约——auth 的 gRPC 契约面没有分页）；OAuth/MCP 手工 `limit/offset` + `count`。 | 重新设计信封（D8）：前端消费收拢到 `types/common.ts` 的 `PageResult` + `usePagedList`/`useEntityListPage` + `mock/response.ts`，只读 `records/total/size/current`——一次协调好的前端切换。 |
| F11 | SQL 异常没有专门转译；业务唯一性靠 check-then-insert（`getOne` → `DuplicateException`）；兜底 advice 返回 500。 | check-then-insert 仍为主路径；增加一个薄的 `R2dbcException` 映射助手（D17）。 |
| F12 | `dc3-common-dal`（label/group 表）只有 manager 在用；auth 仅从中导入 `DictionaryBO`，`DictionaryForAuthService` 经 `TenantManager` 读 `dc3_tenant`。data/agentic 在 POM 中依赖 dal，但代码中零引用。 | auth 试点**不**触碰 `dc3-common-dal`。范围缩小。 |
| F13 | 每个 center 服务的数据源是单一 `master` 路由（dynamic-datasource），URL 为 `currentSchema=dc3_auth`（PG）/ 每服务一个数据库（MySQL）；Hikari max 64。 | R2DBC 只需一个连接工厂 + `r2dbc-pool`；dynamic-datasource 从 auth 中完全退出。 |
| F14 | 今天的选择机制：`dc3.db.type` → 每方言 jar 的 `@ConditionalOnProperty` + EnvironmentPostProcessor profile 激活；适配器 jar 是可选的 Maven 依赖（auth 默认自带 postgres；mysql/mariadb 只在 TCK 中）。 | 复用同一机制；没有新的选择概念（D3）。 |
| F15 | 既有 `dc3-db-{postgres,mysql,mariadb}` jar 携带 MyBatis 专属配置（分页 `DbType`、timestamptz TypeHandler、driver-class-name）。过渡期 manager/data 仍需要它们。 | 既有方言模块原样保留；R2DBC 配置放进新的每服务方言模块（D3）。 |

## 4. 目标架构

```text
dc3-common/dc3-common-auth
│   contract: io.github.pnoker.common.auth.repository.*   (plain reactive interfaces, Mono/Flux)
│   entities: domain DOs (no ORM annotations required by the contract)
│   services/biz: rewritten as reactive chains on the contract
│
dc3-db/dc3-db-r2dbc-core            ← new, pure types, ZERO Spring/R2DBC deps: snowflake IDs, PageResult/PageInfo,
│                                      R2dbcException mapping — safe for the contract module to depend on (D2/D4)
├── dc3-db-r2dbc-boot               ← new: shared R2DBC auto-config helpers (ConnectionFactory/pool config,
│                                      D19 codec, observability); spring-data-r2dbc compiles only here
├── dc3-db-auth-postgres            ← Pg auth repositories: Spring Data + @Query native SQL,
│                                      application-auth-postgres.yml (r2dbc URL, pool), auto-config
├── dc3-db-auth-mysql               ← ditto, MySQL dialect
├── dc3-db-auth-mariadb             ← ditto, MariaDB dialect
└── dc3-db-tck                      ← extended: repository behavior contracts × 3 engines
        (existing dc3-db-core / dc3-db-{postgres,mysql,mariadb} untouched — they keep serving
         manager/data's MyBatis stack until their own migration)
```

依赖方向：`dc3-db-auth-{dialect} → dc3-common-auth (contract) + dc3-db-r2dbc-core (pure types) + dc3-db-r2dbc-boot + spring-data-r2dbc + dialect r2dbc driver`。契约模块只依赖 `dc3-db-r2dbc-core`，永远接触不到 Spring Data、驱动类型或 MyBatis——core/boot 拆分正是让 D2 保持成立、同时 D4 仍允许契约使用 `PageResult` 的关键。

选择机制精确复用 F14：每个 `dc3-db-auth-{dialect}` 自动配置都是 `@ConditionalOnProperty(prefix = "dc3.db", name = "type", havingValue = "{dialect}")`（postgres 为 `matchIfMissing = true`），经由自己的 profile yml 提供基于 `ConnectionFactory` 的配置；适配器缺失或不匹配时启动即快速失败——与今天 `MybatisPlusConfig` 的做法相同。

## 5. 决策清单

每条决策：背景 → 备选项 → **建议** → 后果。状态列是评审跟踪表。

| # | 决策 | 建议 | 状态 |
|---|------|------|------|
| D1 | **契约归属**——Repository 接口放在哪里。备选项：(a) `dc3-common-auth` 的 `repository` 包；(b) `dc3-db-core` 作为契约枢纽。 | **(a)**——沿袭 TSDB 先例（`RepositoryService` 放在 `dc3-common-repository`，适配器依赖它）。避免 `dc3-db-core` 变成业务大杂烩；实现 → 契约的依赖方向干净。 | **已确认 2026-08-28** |
| D2 | **契约风格**——纯 `Mono`/`Flux` 接口，还是扩展 Spring Data repository 类型。 | **纯接口，零框架类型。**方言模块可以在内部扩展 `ReactiveCrudRepository` 并做适配。TCK 测的是契约，不是 Spring Data。 | **已确认 2026-08-28** |
| D3 | **模块布局**——嵌在既有方言模块之下，还是在 `dc3-db` 下平铺；若平铺，按哪个命名轴。 | **平铺、服务优先**：`dc3-db/dc3-db-auth-{postgres,mysql,mariadb}`（后续 `dc3-db-manager-*`、`dc3-db-data-*`）。服务优先，因为日常工作会把同一契约的三个方言实现并排打开，`dc3-center-auth` 的依赖列表呈现单一前缀，TCK 契约也按业务模块组织。既有 `dc3-db-*` 模块不动（F15）。否决嵌套：`dc3-db-postgres` 是一个服务于行将退役的 MyBatis 过渡期的 jar，嵌套将需要聚合器/继承层面的别扭扭曲。 | **已确认 2026-08-28** |
| D4 | **共享 R2DBC 基础设施**——新建 `dc3-db-r2dbc-core`，还是把助手散落各处。 | **两个薄模块，按依赖重量拆分**：`dc3-db-r2dbc-core` 只放纯类型——第一方雪花 ID（与 MP 兼容的形状：timestamp+worker+sequence，保证 ID 在过渡期保持趋势有序）、`PageResult<T>`/`PageInfo`、`R2dbcException` → 业务异常映射——零 Spring/R2DBC 依赖，契约模块可以依赖它而不违反 D2；`dc3-db-r2dbc-boot` 放依赖 Spring 的共享助手（ConnectionFactory/连接池自动配置约定、自定义转换、D19 编解码、可观测性），spring-data-r2dbc 只在这里是编译期依赖。任何地方都不碰 MyBatis。 | **已确认 2026-08-28**（修订：core/boot 拆分，使 D2 与 D4 不再互相矛盾） |
| D5 | **租户传递**——显式 `tenantId` 参数，还是 Reactor Context 传播，还是保留 ThreadLocal。 | **显式参数**（第一个参数，`Long tenantId`）。编译期 fail-closed——比今天的运行期异常更强；延续手写 SQL 已有的做法（F4）；消灭 R2DBC 路径上的 ThreadLocal 生命周期隐患。三类入口本就显式持有 `tenantId`（Controller 经安全上下文，gRPC 经请求字段，本地 Facade 经参数）——这一改动是删除 ThreadLocal 的绕路，而不是新增一路参数传递。`TenantContextHolder` 为 manager/data 的 MyBatis 技术栈原样保留。 | **已确认 2026-08-28** |
| D6 | **无租户契约面**——`runIgnore`/白名单语义如何延续。 | 无租户 repository（F5 各表）上采用**具名方法变体**：没有 `runIgnore`，没有全局白名单。系统/登录路径调用的方法，名字本身就说明其无作用域（如 `findByLoginName`）；每个租户作用域方法在没有租户参数的情况下干脆不存在。 | **已确认 2026-08-28** |
| D7 | **动态查询**——如何移植 `LambdaQueryWrapper` 语义？ | **每个 Query BO 对应具名 repository 方法**（`Mono<PageResult<UserDO>> listByQuery(Long tenantId, UserQuery q)`）；每个方言渲染自己的 SQL。不做 criteria-builder 式移植——12 个 fuzzyQuery 方法是有限、可评审的 SQL。 | **已确认 2026-08-28** |
| D8 | **分页**——中立类型与信封形状。 | **重新设计、一步切换、无兼容窗口（不与 MP 兼容）：嵌套 page 对象。**`PageResult<T> = { records: T[], page: PageInfo }`、`PageInfo = { current, size, total, pages }`，二者都放在 `dc3-db-r2dbc-core`（D4）。为什么嵌套而非扁平的 MP 形状中立类型（扁平今天零成本，却要永远偿还）：分页响应恰好保持两个顶层键——载荷与元数据——未来的分页元数据（排序回显、近似总数标志、游标）只扩展 `PageInfo`，永不触碰载荷、也不累积新的顶层字段，而累积正是当初把 MP 内部字段泄漏出来的机制（F10）；凡是只关心计数的地方 `PageInfo` 都可复用；前端得到一个泛型 `PageResult<T>`，元数据带类型。`orders` 不回显（调用方知道自己发了什么）。`PageUtil` 的钳制/默认排序移入 repository 层。为什么不设兼容层：这次破坏恰好只付一次——双格式窗口会让泄漏的形状在 manager/data 迁移期间继续存活，并迫使日后二次破坏；形状就在此处改变，何况持久层本来就在此刻重写。切换范围有界：dc3-web（四个收拢触点——`types/common.ts`、`usePagedList`、`useEntityListPage`、`mock/response.ts`——现改为读取 `records` + `page.*`）、openapi 快照与 e2e 在同一变更集内切换；外部 REST 集成方经由发布说明与重新生成的 OpenAPI 获得书面记录的破坏性变更。auth 的 gRPC 契约不带分页，proto 因此不动。随着 manager/data 迁移，`PageResult`/`PageInfo` 成为项目级信封。 | **已确认 2026-08-28**（复审：一步切换，明确否决兼容方案） |
| D9 | **逻辑删除**——`@TableLogic` 语义如何存续。 | **显式 SQL 谓词**（每次读取带 `deleted = 0`，删除用 `SET deleted = 1`）+ 每个契约方法的 Javadoc 注明是否过滤已删除行。部分唯一索引（`WHERE deleted = 0`）继续保障唯一性。 | **已确认 2026-08-28** |
| D10 | **ID 生成**——迁移后 MP `IdWorker` 不可用。 | **`dc3-db-r2dbc-core` 中的第一方雪花 ID**，位布局与 MP `ASSIGN_ID` 相同；插入前显式调用（`OAuthMcpRuntimeServiceImpl` 的既有模式已经正是这么做的——把它推广开来）。**workerId 分配是正确性不变量，不是细节**：显式环境变量分配（`DC3_DB_WORKER_ID` 风格；StatefulSet 序号 / 每副本固定值）、单节点开发用确定性的 IP-hash 兜底，以及启动检测——同一部署内拒绝重复的 workerId+datacenter 组合；TCK 增加双实例唯一性契约。双栈过渡期，已分配空间还必须不与 `dc3-center-single` 中仍在写同一批表的 MP `IdWorker` 实例冲突。 | **已确认 2026-08-28**（修订：workerId 分配规则） |
| D11 | **时间戳**——`create_time`/`operate_time` 由谁写。 | **数据库保持权威**（F8）：插入排除两列，更新绝不设置 `operate_time`，需要触发器值的业务代码在更新后回读。零 schema 变更，零行为变更。 | **已确认 2026-08-28** |
| D12 | **事务**——响应式事务策略。 | **响应式方法上的 `@Transactional(rollbackFor = Exception.class)` + `R2dbcTransactionManager`**，由每个方言模块自动配置。既有 19 处全部平移（F9）；`UserController.add/delete` 那处不受管理的 3 表写作为迁移的一部分包进 service 级事务（缺陷修复，见 §11）。 | **已确认 2026-08-28** |
| D13 | **OAuth/MCP 契约面拆分**——一个镜像 30 方法 mapper 的 `OAuthMcpRepository`，还是按聚合拆分 repository。 | **按聚合拆分**：`OAuthClientRepository`、`OAuthAuthorizationRepository`、`McpConnectionRepository`（+ `dc3_mcp_connection_tool`——`replaceConnectionTools` 是 connection 聚合的操作）、`McpToolCatalogRepository`、`McpAuditLogRepository`（+ `dc3_mcp_tool_confirmation`；确切归属在实现时按聚合根规则决定）。7 张表（F3）映射为 5 个聚焦契约；方言分叉语句（JSON cast、upsert 惯用法）落在各自方言模块。响应式事务绑定的是 `ConnectionFactory` 而非 repository，因此 biz 级 `@Transactional` 可以自由跨 repository 组合。 | **已确认 2026-08-28** |
| D14 | **MySQL 驱动**——对 MySQL 服务器用 `r2dbc-mysql`（社区）还是 `r2dbc-mariadb`（官方）。 | **由 TCK spike 决定，两个候选都接好线**：MySQL 没有官方 R2DBC 驱动；社区驱动口碑良好但由志愿者维护，官方 MariaDB 驱动同样说 MySQL 协议（注意点：`caching_sha2_password`、JSON 编解码）。闸门：在 `mysql:8.4` 上通过完整 TCK 的那个留下；理由记录于此。MariaDB 方言用 `r2dbc-mariadb`（没有悬念）。 | 待定 —— P1 spike |
| D15 | **方言模块内部的查询风格。** | **默认用 Spring Data 接口 + `@Query` 原生 SQL；批量 upsert 与连接绑定操作用 `DatabaseClient`**（咨询锁——见 §8）。逐语句自由选择，在契约层之上不可见（D2）。 | **已确认 2026-08-28** |
| D16 | **共存与切换**——auth 移除 MyBatis 的时机。 | **每服务硬切换**：auth 只随 R2DBC 发布（去掉 `dc3-db-core`、dynamic-datasource、`mapping/*.xml`、`@MapperScan`）；`dc3-center-single` 在 manager/data 迁移完之前对同一数据库同时运行两套技术栈（过渡期两个连接池——已接受，有界）。 | **已确认 2026-08-28** |
| D17 | **错误转译**——SQL 异常映射。 | **`dc3-db-r2dbc-core` 中的薄助手**：`R2dbcDataIntegrityViolationException` → 既有 `DuplicateException`/`BusinessException` 的映射，在方言实现中的自然位置应用。check-then-insert 仍是主要 UX 路径（F11）；该映射是兜底，使约束冲突不再以 500 的形式暴露。 | **已确认 2026-08-28** |
| D18 | **Facade 与 gRPC 服务器边界**——auth 的非 HTTP 接缝，早期草案中缺席。auth 服务还经由 7 个阻塞的 Facade 契约（`TokenFacade.checkValid` 返回 `boolean`，另有 Permission/User/Tenant/LocalCredential/ResourceRegistry/McpRuntime）、7 个 gRPC 服务器（`grpc/*Server.java`——`StreamObserver` 回调把服务包进 `TenantContextHolder.runIgnore`）以及本地 Facade 实现暴露；消费方是网关（阻塞 gRPC stub）和每个 WebFlux 服务经 `FacadePermissionProvider` 的安全链。备选项：(a) 现在就将 Facade 契约改为响应式；(b) 保留阻塞契约并显式架桥。 | **(b)——阻塞边缘，显式且临时。**Facade 契约在本轨保持同步；响应式→阻塞桥只存在于 auth 的 gRPC 服务器与本地 Facade 实现中，在 gRPC executor / boundedElastic 线程上执行（绝不在事件循环上——防范事件循环上的 `block()` 错误），`runIgnore` 包装改为无作用域方法变体（§6）。这使试点的爆炸半径有界——选项 (a) 会把网关的 `FilterServiceImpl`、`dc3-common-facade-grpc` 和共享的 `dc3-common-web` 拖进 auth 轨道。“真正的端到端响应式”在 auth 内部覆盖 controller→repository；边缘经决策保持同步，直到 Facade 浪潮随 manager/data 落地，届时 `TokenGrpcFacade`、`FilterServiceImpl`、`FacadePermissionProvider` 得以简化。进程内 gRPC 测试继续使用阻塞的 `GrpcInProcessExtension` 装置。 | **已确认 2026-08-28** |
| D19 | **时间戳编解码**——`timestamptz` ↔ `LocalDateTime` 由谁转换（`TimestamptzLocalDateTimeTypeHandler` 的后继）。R2DBC PostgreSQL 驱动原生把 `timestamptz` 映射为 `OffsetDateTime`，而非 `LocalDateTime`；MySQL/MariaDB 的 `DATETIME` 无需转换。 | **`dc3-db-r2dbc-boot` 中的第一方 `R2dbcCustomConversions`**（D4 拆分）：为 PG 方言注册 UTC 转换器（OffsetDateTime ↔ LocalDateTime），保持 F8/D11 语义与 §9 的往返契约；MySQL/MariaDB 模块使用默认映射。显式决策，因为它是 R2DBC-PG 迁移的头号陷阱，否则会以一条无主的红色 TCK 项浮出水面。 | **已确认 2026-08-28** |

## 6. R2DBC 上的租户隔离

契约把租户作用域变成**类型级属性**：

- 租户拥有表的每个 repository 只暴露携带 `tenantId` 的方法——缺租户的代码无法编译出查询，这把今天的运行期
  `TenantNotScopedException` 升级为编译期保证。
- 覆盖 11 张无租户表（F5）的 repository 完全没有租户参数；对这些表的跨租户读取（如 user → memberships）照今天一样经过
  显式的两步 service 逻辑。
- 系统路径（启动同步、过期扫描器、上下文之前的登录、MCP 运行时）使用名字明示的无作用域方法变体——取代 auth 路径上的
  `TenantContextHolder.runIgnore(...)` 穿线。
- TCK 提供**否定测试**：跨租户读取在每个 list/get 上必须返回空/不存在；无租户 repository 不得长出租户过滤；跨租户写上的
  约束冲突保持可映射（D17）。

`TenantContextHolder`、`TenantLineHandlerImpl` 与租户拦截器**不做修改**——它们继续服务 manager/data，直到其迁移移除它们。

## 7. Repository 契约约定

- 包 `io.github.pnoker.common.auth.repository`；每个聚合一个接口；参数对象是既有 BO/Query 对象与领域 DO。
- 方法命名遵循项目 CRUD 动词策略（`add/delete/update/getById/list...`；`select*` 保留给持久化风味的读取——与 AGENTS.md
  同一规则，应用于新层）。
- 返回形状：`Mono<X>` 表示 0..1，`Flux<X>` 表示多个，`Mono<PageResult<X>>` 表示分页；写入返回 `Mono<Void>`，或在业务
  需要数据库计算值时返回回读实体（F8 模式）。
- 事务在 services/biz 中声明（同今天，F9）；repository 对事务无感知。
- 契约不要求 DO 上有 ORM 注解；方言模块可以保留私有 `@Table` 行类再映射到领域 DO，若干净也可直接注解共享 DO——实现期间
  按聚合决定，TCK 不关心（D2 使这一点保持为实现细节）。

## 8. 方言实现约定

每个方言模块（以 `dc3-db-auth-postgres` 为参照）：

- **配置**：`application-auth-{dialect}.yml` 提供 `spring.r2dbc.*`（URL 模板来自同一 `DC3_DB_*` 环境变量家族——PG 为
  `options=search_path=dc3_auth`，URL 编码，与今天 JDBC URL 相同的 search-path 语义；MySQL/MariaDB 为每服务一个数据库）
  与等价于今天 Hikari 设置的 `r2dbc-pool` 容量（F13）。Profile 由模块的 EnvironmentPostProcessor 激活，镜像 F14。
  可观测性对等是配置契约的一部分：r2dbc-pool 的 Micrometer 指标与连接健康指示器，由 `dc3-db-r2dbc-boot` 注册，取代
  Hikari 的指标。
- **标准 CRUD**：形状合适处使用扩展 `ReactiveCrudRepository` 的 Spring Data repository 接口。
- **方言锁定语句**——正是它们催生了每方言模块。每个方言模块写出自己的原生形式；下表是迁移盘点（与 auth 相关的）：

  | 关注点 | PostgreSQL | MySQL 8 | MariaDB | 说明 |
  |--------|------------|---------|---------|------|
  | upsert | `INSERT ... ON CONFLICT ... DO UPDATE` | `ON DUPLICATE KEY UPDATE ... AS new`（别名形式；`VALUES()` 在 8.4 中移除） | `ON DUPLICATE KEY UPDATE ... VALUES(col)` | 返回行的方式不同：PG `RETURNING` vs 再查询（既有 TCK 模式） |
  | 咨询锁 | `pg_advisory_xact_lock(hashtext(?))` 事务内 | `GET_LOCK(?, 10)` **会话级——必须在单条池化连接上运行并与 `RELEASE_LOCK` 成对** | 同 MySQL | R2DBC 风险点：MySQL/MariaDB 形式须用连接绑定执行（`Mono.usingWhen` / `Connection` API） |
  | JSON 列 | 原生 JSON 编解码 | `CAST(? AS JSON)` 绑定（不得反斜杠转义 / 用绑定参数） | 无 `CAST AS JSON`——普通参数文本 | 契约类型是 `String`；转换在方言内部完成 |
  | 目录查询中的字符串操作 | `||`、`regexp_replace(..., 'g')` | `CONCAT`、默认全局替换 | 同 MySQL | 来自当前 `OAuthMcpMapper` 分叉 |
  | 分页 | `LIMIT ? OFFSET ?` | `LIMIT ?, ?` | 同 MySQL | 每个 repository 方法一条 count 查询 |

- **锁 × 事务组合规则**（约束上表咨询锁一行）：`pg_advisory_xact_lock` 参与外层 `@Transactional` 并在提交时自动释放；
  MySQL/MariaDB 的 `GET_LOCK`/`RELEASE_LOCK` 对经 `Mono.usingWhen` 在单条池化连接上运行（释放同时挂接到取消），且**绝不
  在 Spring 管理的事务内执行**——事务绑定自己的连接，池压力下锁连接 + 事务连接可能互相饿死成死锁。锁 repository 方法在
  契约上豁免事务；TCK 为该配对提供否定测试。
- **雪花 ID** 在插入前由应用侧生成（D10）——不依赖 `RETURNING id`，各方言完全一致。
- **native hints**：方言模块为其行类注册 `RuntimeHints`（`@RegisterReflectionForBinding`），并贡献驱动专属 hints；由
  native 冒烟测试验证（§10）。

## 9. TCK 2.0 —— repository 行为契约

`dc3-db-tck` 在既有 mapper 契约（保留至 manager/data 迁移完）之外新增第二套套件：

- 每个 repository 接口一个抽象契约测试；三个具体子类（Postgres / MySQL / MariaDB）跑在 Testcontainers 上，夹具与今天的
  镜像一致（`postgres`、`mysql:8.4`、`mariadb:10.11`，同样的 initdb 种子）。
- 覆盖类别：CRUD + 逻辑删除语义；模糊/分页列表等价性（含默认 `create_time DESC` 排序与 `PageUtil` 钳制）；三种方言惯用法
  下的 upsert 幂等性；咨询锁获取/释放（含 MySQL/MariaDB 同连接要求）；租户否定测试（§6）；JSON 往返；UTC 时间戳往返
  （TIMESTAMPTZ ↔ `LocalDateTime`，即现有 `TimestamptzLocalDateTimeTypeHandler` 契约）。
- 闸门：新的 mapper/repository 工作必须扩展契约套件——这就是取代 `databaseId` 路由的纪律机制。

## 10. Native 验证

试点的退出判据，按顺序：

1. `dc3-center-auth` 以 `mvn -Pnative`（插件已在根 POM 托管）针对 R2DBC 技术栈完成构建。
2. native 二进制对 Testcontainers PostgreSQL 启动，服务一段有代表性的端点切片（登录 → token → 一次租户作用域列表 →
   一次 MCP 工具目录查询），并以 native 方式跑通 TCK 契约套件（允许是子集——记录是哪些）。
3. 镜像大小 / RSS / 启动时间记录在本文档中，作为 manager/data 要超越的基线。

若浮现任何阻塞性 native 问题（驱动 hints、反射遗漏），那是设计级发现：在 `dc3-db-r2dbc-core`/方言模块中修复，绝不能用
给 auth 重新加回 MyBatis 的方式解决。

## 11. 过渡与共存

- **auth 的切换是硬切换**（D16）：一个提交从 auth 路径移除 `dc3-db-core`、`dynamic-datasource`、`@MapperScan`、
  `mapping/*.xml`、16 个 Manager 壳与 `TenantContextHolder` 的使用。auth 内部不搞长期的双持久化。
- **`dc3-center-single`** 临时同时承载两套技术栈（auth 在 R2DBC 上，manager/data 在 MyBatis 上）指向同一数据库——两个
  连接池，已接受并由过渡期限定。
- **Facade/gRPC 边缘在整个过渡期保持同步（D18）**：网关与 manager/data 继续消费阻塞的 Facade 契约；响应式→阻塞桥仅限于
  auth 的 gRPC 服务器与本地 Facade 实现、位于非事件循环线程，并在 Facade 浪潮随 manager/data 落地时退役。
- **工作区中未提交的 MariaDB XML 分叉**（`OAuthMcpMapper.xml` 等）：无论落地还是丢弃都与本设计相互独立——它们的*语义*
  发现（缺 CAST-AS-JSON、`AS new` vs `VALUES()`、GET_LOCK 配对）已经吸收进 §8，XML 删除后依然留存。
- **顺路修复的缺陷**（D12）：`UserController.add/delete` 中不受管理的 principal→user→membership 写入在重写时补上
  service 级响应式事务。
- **跨服务复用公共部分**：manager/data 迁移时复制该模块模式（`dc3-db-manager-{dialect}`，……）；`dc3-db-r2dbc-core` 与
  TCK 基础设施从第一天起就是共享的。

## 12. 分阶段计划与闸门

| 阶段 | 内容 | 闸门 |
|------|------|------|
| P0 | 设计签核（本文档；§5 全部决策为已确认）。 | 已批准文档完成提交。**2026-08-28 完成**（D14 推迟至其 spike）。 |
| P1 | 骨架：`dc3-db-r2dbc-core`（雪花 ID + workerId 规则（D10）、`PageResult`、异常映射）、`dc3-db-r2dbc-boot`（自动配置、D19 编解码）、模块壳 ×3、TCK 装置扩展、自动配置 + fail-fast 选择接线。 | `make test` 全绿；空契约在 PG 上端到端接通。 |
| P2 | **垂直切片**：`TenantRepository`（纯 CRUD + 分页）与 `ResourceRegistryLockRepository`（咨询锁，方言最敏感）——契约 → 3 个方言 → TCK → 响应式 service → controller。 | TCK 3/3 全绿；PG 开发栈可启动并提供租户 CRUD。 |
| P3 | 在切片上做 native spike：`dc3-center-auth` `-Pnative` 编译 + 启动 + 端点冒烟。 | 切片满足 §10 判据。 |
| P4 | 按聚合推广：users/principals/credentials → roles/binds/memberships → menus/APIs/resources + registry sync → service accounts/audit → OAuth/MCP（最大，最后）。每个聚合连同其 TCK 契约与其迁移后的 StepVerifier 测试（附录 A）一起落地。 | 完整 TCK ×3 全绿；auth E2E（`dc3-e2e`）全绿。 |
| P5 | auth 移除 MyBatis（D16）、`dc3-center-auth` native image 作为交付物、文档更新（`db-dialects.md`、本文件状态）、记录 MySQL 驱动决策（D14）。 | native 启动；auth classpath 上零 `mybatis` 字样。 |

P2–P3 是 go/no-go 节点：如果切片证明了该模式，P4 就是机械劳动；如果证明不了，扔掉的只有切片。

## 13. 待解问题

1. **D14 驱动数据**——`r2dbc-mysql` 与 `r2dbc-mariadb`-against-MySQL 的真实 TCK 结果（P1 spike 产出）。
2. **行类策略**（§7）：直接注解共享 DO，还是每方言私有行类——P2 期间定下一个约定并记录于此。
3. **native/R2DBC 下的连接池容量**——max 64（F13）对响应式需求模式是否仍然合适；P3 期间测量。
4. **`dc3-e2e` 覆盖**——允许 P4 推广开始之前必须存在哪些 auth E2E 流程。
5. **`storage-abstraction.md` 的处置**——标记 §3 被本文档取代（一行状态编辑），或在 manager/data 落地后退役整个文件
   （其 TSDB 一半已被取代）。
6. **可观测性对等**——`dc3-db-r2dbc-boot`（P2）中要实现的确切 r2dbc-pool 指标集、健康指示器接线与慢查询日志约定；对照
   F13 的 Hikari 基线测量。

## 附录 A —— auth 迁移盘点

- **Mapper（18 个）**：16 个 `BaseMapper` 壳（与 15 个空 Manager + `IdentityAuditLogManager` 一一对应）→ 标准 repository
  契约；`OAuthMcpMapper`（30 个方法，7 张表）→ 5 个聚合 repository（D13）；`ResourceRegistryLockMapper`（咨询锁）→
  `ResourceRegistryLockRepository`。
- **`@Transactional` 位置（19 处）**：F9 已列举；全部平移至 D12。
- **auth 中的方言分叉语句**：`ResourceRegistryLockMapper.xml`（锁，3 种形式）、`OAuthMcpMapper.xml`（JSON cast ×4、目录
  字符串操作 ×2、upsert 惯用法 ×2）——语义表见 §8。
- **待重写为响应式的 Service**：12 个分页/模糊 Service + `OAuthMcpRuntimeServiceImpl` + `ResourceRegistrySyncServiceImpl`
  + `DictionaryForAuthService`（读 `dc3_tenant`，F12）。
- **Facade 与 gRPC 服务器（边缘，D18）**：7 个 Facade 契约保持阻塞；7 个 gRPC 服务器（`TokenServer`、`PermissionServer`、
  `UserServer`、`TenantServer`、`LocalCredentialServer`、`ResourceRegistryServer`、`McpRuntimeServer`）与 7 个本地 Facade
  实现加上显式响应式→阻塞桥（非事件循环线程），并把 `runIgnore` 包装换成无作用域方法变体（§6）。
- **测试（17 个文件）**：阻塞式 service/biz、controller 与 gRPC 测试（如 `TokenServiceImplTest`、
  `OAuthMcpRuntimeServiceImplTest`、`TokenServerTest`、`McpRuntimeServerTest`）以 `StepVerifier` 重写；进程内 gRPC 测试
  针对 D18 阻塞边缘继续使用 `GrpcInProcessExtension`；基于 ThreadLocal 的上下文测试迁移为显式参数断言。
- **不在范围内**：`dc3-common-dal`（F12）、manager/data/agentic 的全部 DAL、DDL/initdb、`dc3-db-core` 与既有方言模块
  （F15）。
