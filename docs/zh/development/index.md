---
title: 开发概览与规范
---

<script setup>
import DevIndexDiagram from '../../.vitepress/theme/components/DevIndexDiagram.vue'
</script>


# 开发概览与规范

这页写给准备给 IoT DC3
写后端代码的开发者：读完你会知道工程的权威规范在哪、命名与分层必须遵守哪些硬约定，以及提交一条改动该走的"第一条路径"。

> 你在这里：想动手扩展平台。下一步按目标分流——写新协议驱动看[驱动开发](./driver-authoring)
> ，调通接口看 [API 文档](./api-documentation)，跑测试看[测试](./testing)。

## 权威规范在 `AGENTS.md`

本页是入口与速览，**真正的工程规范以仓库根的 `iot-dc3/AGENTS.md` 为准**。它是一份跨 AI 工具共享的单一事实源，覆盖模块分层、Maven
命令、验证流程、提交与变更日志规则、以及下文要点的完整版本。`iot-dc3/.claude/CLAUDE.md` 只是把规范委托给它，不重复内容。动手前先通读
`AGENTS.md`；本页与它冲突时以 `AGENTS.md` 为准。

平台是 Java 21 / Spring Boot 4 / Spring Cloud 2025 的分布式服务，跨服务协调走 gRPC，元数据落 PostgreSQL，异步消息走
RabbitMQ。这套技术栈决定了下面三条不可绕过的约定：CRUD 动词随结果基数走、跨服务调用必须经 facade、领域对象按 DO/BO/VO 分层。

## CRUD 动词随"结果基数"走

平台没有自由命名空间：每个 CRUD 形态的方法、HTTP 路径、gRPC RPC、以及前端 API 函数，**动词必须反映返回结果的基数**——查单条用
`get`，查集合用 `list`。这条约定横跨 Service 接口、ServiceImpl、Controller、Local/gRPC Facade、gRPC server 与 `.proto` 里的
RPC 名，前后端两个仓库一致执行。它的价值是：看到一个方法名或一条路径，不用读实现就知道它返回一条还是一批。

| 动作  | Java 方法        | HTTP 路径     | gRPC RPC  | 前端函数             |
|-----|----------------|-------------|-----------|------------------|
| 查单条 | `getXxx(...)`  | `/get_xxx`  | `GetXxx`  | `getXxx(...)`    |
| 查集合 | `listXxx(...)` | `/list_xxx` | `ListXxx` | `listXxx(...)`   |
| 新增  | `add(BO)`      | `/add`      | n/a       | `addXxx(...)`    |
| 更新  | `update(BO)`   | `/update`   | n/a       | `updateXxx(...)` |
| 删除  | `delete(Long)` | `/delete`   | n/a       | `deleteXxx(...)` |

`add`/`delete`/`update`/`getById`/`list(Q)` 这五个基础方法由 `BaseService<B, Q>` 继承而来；子接口只在需要按维度查询时追加
`getByXxx`/`listByXxx`，且动词仍要匹配基数。`DeviceController` 是一个现成范本——它的端点恰好是 `/add`、`/delete`、`/update`、
`/get_by_id`（查单条）、`/list_by_ids`、`/list_by_profile_id`、`/list`（查集合），动词与基数严格对齐。

::: warning 三个保留动词不要混用

- `select*` 只用于 `*ManagerImpl` 里对 MyBatis Mapper 的原始调用，**不出现**在 Service/Controller/Facade 上。
- `remove*` 只用于 MyBatis-Plus 继承来的 Manager 方法（`removeById`、`remove(wrapper)`）；业务删除一律用 `delete*`。
- `find*`、`query*`、`fetch*` 不作为主 CRUD 动词。
  :::

## 分层调用：Controller(VO) → Service(BO) → Manager(DO) / Facade(跨服务)

请求进来要穿过三层，每层只认一种数据表示。Controller 接收和返回 **VO**（API 形态）；Service 接口继承 `BaseService<B, Q>`，**只在
BO 类型上工作**（业务语义，用领域枚举如 `EnableFlagEnum`）；Manager/Mapper 操作 **DO**（数据库形态，flag 用 `Byte`）。三种表示之间由
MapStruct 的 `*Builder` 转换，DO 的 flag 绝不直接泄漏到业务或响应模型。

这里有一条硬边界：**当业务代码需要别的服务的数据时，不能直连传输细节，必须经过 facade 接口**。Controller 和 Service 类不绑定
gRPC 或 REST 的任何细节——它们只调用 `dc3-common-facade-api` 里的契约接口，由部署形态决定背后是 gRPC 实现（
`dc3-common-facade-grpc`）还是同进程实现（`dc3-common-facade-local-*`）。分布式部署默认走 `grpc`（`DC3_FACADE_MODE=grpc`）。

<DevIndexDiagram lang="zh" />

图中那条标注"必须经 facade"的虚线就是边界本身：左半边是本服务内的 VO→BO→DO 直落，右半边是任何跨服务读写都要先抽象成 facade
契约、再由配置选择传输实现。这让 `grpc`（分布式）与 `local`（单体）成为纯部署拓扑选择，业务代码一行不改。各层对象的字段、枚举转换与
`*Builder` 的细节见[领域模型](../architecture/domain-model)。

## 第一条路径：从改一个端点到提交

把上面三条约定串起来，一次典型的后端改动是这样走的。假设你要给设备管理加一个"按 driverId 查设备数量"的接口：

::: code-group

```text [分层落点]
1. VO/BO/DO     在 dc3-common-model 或对应模块补字段（若需要），MapStruct *Builder 同步
2. Manager      *ManagerImpl 里用 select* 调 Mapper（仅此处可用 select*）
3. Service      在 Service 接口加 getCountByDriverId(...)，ServiceImpl 实现，只碰 BO
4. 跨服务?      若要拿别的中心的数据，走 *Facade 接口，不直连 gRPC
5. Controller   GET /get_count_by_driver_id —— 查单值用 get 动词
```

```bash [验证]
# 快速编译校验（改 Java/共享行为后必跑）
mvn -s .mvn/settings.xml -q -DskipTests compile

# 完整打包（提交前按改动比例选择）
mvn -s .mvn/settings.xml clean package

# 改了 DAL/SQL：需要容器运行时的集成测试
make test-it
```

:::

写完代码、跑过验证，再提交。

## 提交规范：Conventional Commits

提交信息直接变成发布说明（`CHANGE.md` 由 git 历史生成），所以 subject 必须具体、可读。格式固定为：

```text
<type>(optional-scope): <english imperative summary>
```

- subject 用**英文、小写、祈使句**，足够具体以便写进 `CHANGE.md`。
- 允许的 type：`feat`、`fix`、`perf`、`refactor`、`docs`、`build`、`ci`、`test`、`chore`、`style`、`security`、`revert`。
- 非根级的微小改动尽量带 scope；破坏性变更用 `!` 并在 body 说明影响。
- 不要用 `update`、`fix bug`、`change code`、`misc`、`wip`、`.` 这类弱 subject。

真实示例：

```text
feat(agentic): add session cleanup policy
fix(manager): validate tenant scope for device queries
docs(env): explain JetBrains IDEA environment variables
```

::: warning 提交前的硬约定

- AI 协作代理**未经明确确认不得创建提交**；提交前需展示拟用的 commit message 与纳入的文件，等待批准。
- 不要把无关改动塞进一个提交——按意图拆分（功能、修复、重构各自成提交）。
- 发布说明专用提交固定为 `docs(release): update generated changelog`，且 `CHANGE.md` 单独提交。
- 提交前对照上面的格式与真实示例自查 commit message；不合规格式会被 CI 拦在合并前。
  :::

## 常用 Maven 命令速查

| 场景     | 命令                                                                   | 说明          |
|--------|----------------------------------------------------------------------|-------------|
| 全量编译   | `mvn -s .mvn/settings.xml compile`                                   | 只编译，不跑测试    |
| 快速编译检查 | `mvn -s .mvn/settings.xml -q -DskipTests compile`                    | 安静模式，改完快速验证 |
| 全量打包   | `mvn -s .mvn/settings.xml clean package`                             | 编译+测试+打包    |
| 跳测试打包  | `mvn -s .mvn/settings.xml -DskipTests clean package`                 | 不跑测试        |
| 单模块打包  | `mvn -s .mvn/settings.xml -pl dc3-driver/dc3-driver-virtual package` | -pl 指定模块    |
| 查看依赖树  | `mvn -s .mvn/settings.xml dependency:tree -pl <模块>`                  | 排查传递冲突      |

::: tip 并行构建
`.mvn/maven.config` 已配 `-T 1C`，不需要手动加。
:::

## 调试技巧

### IDEA 远程调试

VM options 添加 `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005`，然后 Run → Attach to Process。

建议调试端口：Gateway 5005、Auth 5006、Manager 5007、Data 5008、Agentic 5009。

### 模块导航速查

| 我要...         | 去这里                                     |
|---------------|-----------------------------------------|
| 改设备/驱动/位号业务逻辑 | `dc3-center/dc3-center-manager`         |
| 改认证/租户/RBAC   | `dc3-center/dc3-center-auth`            |
| 改位号值存储/命令分发   | `dc3-center/dc3-center-data`            |
| 改 AI 对话/工具调用  | `dc3-center/dc3-center-agentic`         |
| 改网关路由/过滤器     | `dc3-gateway`                           |
| 新加协议驱动        | `dc3-driver/`，从 `dc3-driver-virtual` 复制 |
| 改 gRPC proto  | `dc3-api/`，改完重新 `mvn compile`           |
| 改前端页面         | `dc3-web/`（独立 pnpm 项目）                  |

## 延伸阅读

- [领域模型](../architecture/domain-model) — DO/BO/VO 各层字段、枚举转换与 MapStruct `*Builder` 细节
- [驱动开发](./driver-authoring) — 复制 `dc3-driver-virtual` 模板扩展新协议驱动，实现 `DriverCustomService`
- [API 文档](./api-documentation) — OpenAPI/Swagger 暴露方式、鉴权头与导出流程
- [测试](./testing) — 单元、集成、E2E 与覆盖率约定
