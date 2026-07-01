# 识别与定位层 — 身份唯一性与租户隔离代码缺陷分析（Task 3）

> 切片目标②：把 `identification.md` 落地到 `deviceId`（Device 身份唯一）+ `tenantId`（租户隔离边界）两条线，对 10 个靶子类做三分类缺陷分析。
>
> 靶子性质：身份唯一性 + 隔离边界正确性，**不是**传感层的"计算精度"。故不套用 sensing 模板（标定/精度/截断），而按"唯一性破坏 / 跨租户串台 / 隔离边界"归类。
>
> 学术依据：`analysis/identification-knowledge-entries.md`。两条关键条目：
> - **EPC 全局唯一**（Core PDF p50 §3.1.3）：序列号"能唯一标识每一个物品"——对应 `deviceId` 必须全局唯一。
> - **身份 + 归属二元边界**（`identification.md` §在 IoT DC3 中如何落地）：`deviceId` 回答"是哪一个"、`tenantId` 回答"归谁、谁能看见"——对应 EPC 的"序列号 + 厂商前缀"二元结构。

## 1. 阅读结论（10 个靶子类）

### 1.1 设备身份（4 类）

| 类 | 文件 | 关键结论 |
|---|---|---|
| `DeviceDO` | `dc3-common/dc3-common-manager/.../entity/model/DeviceDO.java` | `@TableId(IdType.ASSIGN_ID)` 雪花 ID（全局唯一）；`deviceCode` 字段在 add 时被置 null，由 `DeviceBuilder#afterProcess` 用 `UUID.randomUUID()` 兜底（`DeviceBuilder.java:84-86`），故 code 全球唯一。**`DeviceDO` 不 `implements TenantOwned`**，但有 `@TableField("tenant_id") private Long tenantId`（行 97-99）——隔离靠字段值而非接口契约（见 2.3）。 |
| `DeviceServiceImpl` | `dc3-common/dc3-common-manager/.../service/impl/DeviceServiceImpl.java` | `deviceName` 唯一性靠 `checkDuplicate`（行 553-566）的 select-then-insert，**作用域是 `(tenant_id, device_name)`**（行 558-559 加 tenant 过滤）；`deviceCode` 不做唯一校验（UUID 本身全局唯一）。update 有租户防篡改守卫（行 162-164：新旧 tenantId 不一致抛 404）。 |
| `DeviceLockManager` | `dc3-common/dc3-common-driver/.../command/DeviceLockManager.java` | 进程内 `ConcurrentHashMap<Long, LockRef>` + `ReentrantLock`，按 deviceId 加锁；引用计数防止锁表膨胀（行 92-100）。锁实例进程内有效，**非分布式锁**。 |
| `DriverMetadata` | `dc3-common/dc3-common-driver/.../metadata/DriverMetadata.java` | `Set<Long> deviceIds`（`ConcurrentHashMap.newKeySet()`，行 61）持有本驱动注册的设备 ID；setter 用 `replaceContents` 原地替换保引用不漂（行 105-110）。集合只存 deviceId，不存 tenantId——但这是驱动内部缓存，来源是 RabbitMQ 元数据事件（见 2.4）。 |

### 1.2 租户隔离（6 类）

| 类 | 文件 | 关键结论 |
|---|---|---|
| `TenantOwned` | `dc3-common/dc3-common-public/.../entity/common/TenantOwned.java` | 标记接口，仅 `getTenantId()` 一个契约。**只 BO 实现**（`DeviceBO extends BaseBO implements TenantOwned`，`DriverBO`/`PointBO`/`ProfileBO`/… 16 个 BO），DO 全部不实现——隔离检查只在 controller→BO 边界发生。 |
| `TenantContextHolder` | `dc3-common/dc3-common-constant/.../tenant/TenantContextHolder.java` | ThreadLocal 存 tenantId + ignore 标志；Javadoc 自称 fail-closed（行 26-31）：tenantId 为 null 且未 ignore 时查询应被拒绝。**但无 MyBatis `TenantLineInnerInterceptor` 消费它**（见 2.1），fail-closed 仅靠 `BaseController#async` 在 finally 里 `clear()`（行 147）维持线程干净，没有 SQL 拦截兜底。 |
| `EntityTenantServiceImpl` | `dc3-common/dc3-common-manager/.../service/impl/EntityTenantServiceImpl.java` | 多态绑定（group/label bind）的租户守卫；按 `EntityTypeEnum` 分派到 Driver/Profile/Point/Device 的 `getById`，再用泛型 `requireTenant` 校验（行 66-69）。仅 2 处调用（`GroupBindController:207`、`LabelBindController:207`）。 |
| `BaseController` | `dc3-common/dc3-common-web/.../base/BaseController.java` | 统一入口：`requireTenant`（单实体，行 79-84）、`filterTenant`（批量，行 89-97）、`async`（绑定 ThreadLocal tenant，行 135-150）。**全 22 个 manager controller 均调用其一**（grep 确认无一遗漏），覆盖完整。 |
| `AuthenticGatewayFilter` | `dc3-common/dc3-common-gateway/.../filter/AuthenticGatewayFilter.java` | 网关鉴权过滤器；解析租户/凭证/用户后把 `PrincipalHeader` 写入请求头（行 70-79），下游服务从 header 取 tenantId。租户身份在网关一次解析、下游信任 header（`X_AUTH_PRINCIPAL`）。 |
| `DriverMetadataListener` | `dc3-common/dc3-common-driver/.../service/DriverMetadataListener.java` | 驱动 SPI 接口，单方法 `event(MetadataEventDTO)`；具体实现（`AbstractJdbcDriverCustomService:124-144` 等）按 `metadataEvent.id` 刷新连接池缓存。**`MetadataEventDTO` 只带 `id`/`metadataType`/`operateType`，不带 tenantId**——但这是驱动内部 trusted 事件路径（见 2.4）。 |

## 2. 三分类缺陷

### 2.1 真缺陷

**D-1（真缺陷）：`TenantContextHolder` 的 fail-closed 设计未接到 MyBatis 拦截层，DB 无 `TenantLineInnerInterceptor`**

- 文件:行：`dc3-common/dc3-common-postgres/src/main/java/io/github/pnoker/common/config/MybatisPlusConfig.java:51-55`
- 现象：`mybatisPlusInterceptor()` 只 `addInnerInterceptor(new PaginationInnerInterceptor(...))`，全仓库 grep `TenantLineInnerInterceptor`/`TenantLineHandler` 零命中。`TenantContextHolder` 的 fail-closed Javadoc（`TenantContextHolder.java:26-31`）描述的"tenantId 为 null 且未 ignore 时拒绝查询"在代码里**没有任何拦截器执行**——上下文里的 tenantId 只被 `BaseController#async` 用于线程清理，不被任何 SQL 改写器消费。
- 后果：隔离完全靠 (a) service 层每个 `wrapper.eq(tenant_id)` 手过滤 + (b) controller 层 `requireTenant`/`filterTenant` 手校验。**任何绕过这两道、直接走 `deviceManager.getById(id)`/`listByIds(ids)` 的路径都没有租户过滤**。当前 controller 层覆盖完整（见 1.2），所以现实未爆，但这是一道"靠纪律维持、无框架兜底"的隔离边界——新增 service 方法或忘记包 `filterTenant` 就会串台。
- 理论依据：身份 + 归属二元边界要求 `tenantId` 是强制不变量。EPC 体系里"厂商前缀"是编码内嵌的强约束；DC3 这里 tenantId 是数据列，需要框架级强约束才能等价。
- 触发条件（假想）：未来新增一个内部 service 方法 `getDeviceForInternal(id)` 直接 `deviceManager.getById(id)` 且不经过 controller 的 `requireTenant`，即跨租户读到他人设备——DB 层无任何拦截。

> 注：归为"真缺陷"而非"技术债"，因为 `TenantContextHolder` 的 Javadoc 明示了 fail-closed 契约（"queries must be rejected rather than run unscoped"），但该契约**实际未被任何组件执行**——契约与现实不一致 = 缺陷，而非"待改进"。

### 2.2 有意简化（工程取舍）

**S-1（有意简化）：`deviceName` 唯一性仅 select-then-insert，DB 无 `(tenant_id, device_name)` 唯一索引**

- 文件:行：`dc3-common/dc3-common-manager/src/main/java/io/github/pnoker/common/manager/service/impl/DeviceServiceImpl.java:553-566`（`checkDuplicate`），DDL 对照 `dc3/dependencies/postgres/initdb/04-iot-dc3-manager.sql:643`（只有 `idx_device_tenant_code_active_unique` on `(tenant_id, device_code)`，无 name 唯一索引）
- 现象：`deviceName` 在租户内的唯一性靠 `checkDuplicate`：先 `getOne` 查同名，无则 insert。两个并发请求同时建同名设备 → 都查不到 → 都插入成功 → 同租户出现重名 deviceName。`deviceCode`（UUID）有 DB 唯一索引兜底，`deviceName` 没有。
- 归类理由（有意简化而非真缺陷）：`deviceName` 是展示名（人读），重名不影响系统寻址——所有内部寻址、元数据事件、锁、`deviceIds` 集合都用 `id`（雪花，全局唯一）或 `deviceCode`（UUID，DB 强制唯一）。重名只造成用户视觉混淆，不破坏身份唯一性不变量。这是"用 DB 唯一索引保强不变量（code）、用应用校验保弱不变量（name）"的常见取舍。
- 理论依据：EPC 序列号"唯一标识每一个物品"对应的是强身份（`id`/`deviceCode`），不是展示名。

**S-2（有意简化）：`DeviceLockManager` 是进程内锁，非分布式**

- 文件:行：`dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/command/DeviceLockManager.java:44`（`ConcurrentHashMap<Long, LockRef>`，进程内）
- 现象：锁只在本驱动进程内生效。DC3 一个 driver 服务默认单实例跑（一个驱动进程管一批设备），所以进程内锁足够串行化"对同一设备的并发指令"。但若同一 driver 部署多实例（HA），两实例各自锁各自的，无法跨进程互斥。
- 归类理由：driver 单实例是当前部署假设（驱动绑定物理协议端口/连接池，水平扩缩本身受限），进程内锁在该假设下正确。属有意简化，标注边界供未来多实例时升级为 Redis 锁。

**S-3（有意简化）：`DriverMetadataListener`/`MetadataEventDTO` 不带 tenantId**

- 文件:行：`dc3-common/dc3-common-driver/src/main/java/io/github/pnoker/common/driver/service/DriverMetadataListener.java:41`（`event(MetadataEventDTO)`），`MetadataEventDTO` 字段 `id`/`metadataType`/`operateType` 无 tenantId
- 现象：驱动收到的元数据事件只有设备/位号 ID，没有租户。但事件来源是 manager-center（`DeviceServiceImpl` 在 controller 已校验租户后 publish），驱动侧消费的是"已被租户守卫过滤过"的事件——只可能包含本驱动注册的设备（注册时 driver_metadata 已按租户隔离）。
- 归类理由：这是 trusted 内部事件路径，不是外部输入。把 tenantId 加进 DTO 会冗余（驱动只该看到自己的设备）。对应已知上下文"metadata-listener tenant convention"——**核查结论：非缺陷，是有意的 trusted-path 设计**。

### 2.3 技术债

**T-1（技术债）：`DeviceDO`（及全部 17 个 DO）不 `implements TenantOwned`，隔离契约只在 BO 层**

- 文件:行：`dc3-common/dc3-common-manager/src/main/java/io/github/pnoker/common/manager/entity/model/DeviceDO.java:48`（`public class DeviceDO implements Serializable`，未 `implements TenantOwned`）；对照 `DeviceBO.java:42`（`extends BaseBO implements TenantOwned`）
- 现象：`TenantOwned` 接口只有 BO 实现，DO 全部靠 `@TableField("tenant_id") private Long tenantId` 隐式持有。后果：`requireTenant`/`filterTenant` 的泛型 `<T extends TenantOwned>` 无法约束 DO——任何直接拿 DO 做隔离检查的代码编译不过，从而**强迫**走 BO 路径（这其实强化了"controller 层校验"）。但 DO 与 BO 的 tenantId 一致性靠 `DeviceBuilder` 手工映射，无编译期保证。
- 影响：不影响正确性（builder 映射当前正确），仅是类型表达力弱——DO 有 tenantId 字段却不实现接口，语义不清。

**T-2（技术债）：`DeviceServiceImpl#listByIds` 无租户参数，全靠 controller `filterTenant` 兜**

- 文件:行：`dc3-common/dc3-common-manager/src/main/java/io/github/pnoker/common/manager/service/impl/DeviceServiceImpl.java:253-260`（`listByIds` 直接 `deviceManager.listByIds(ids)`，无 tenant 过滤）
- 现象：service 层 `listByIds` 不过滤租户，靠 `DeviceController.java:208` 的 `filterTenant(tenantId, deviceService.listByIds(deviceIds))` 在 controller 兜底（先全量查再内存过滤）。这意味着 service 方法**自身不安全**，任何非 controller 调用方（如未来内部 service-to-service）调 `listByIds` 都会拿到全租户数据。
- 影响：当前 controller 覆盖完整，未爆；但 service API 表面"像普通查询"实则依赖调用方过滤，易误用。与 D-1 同源（无框架兜底）。同类方法 `listByDriverId`/`listByProfileId` 在 tenantId 为 null 时也跳过过滤（行 225-227、246-248），同样依赖调用方传入非 null tenantId。

**T-3（技术债）：`BaseController#filterTenant` 批量端点"先查全量再内存过滤"**

- 文件:行：`dc3-common/dc3-common-web/src/main/java/io/github/pnoker/common/base/BaseController.java:89-97`（`filterTenant` 在内存 stream 过滤）
- 现象：`list_by_ids` 类端点的模式是 `filterTenant(tenantId, service.listByIds(ids))`——先把所有传入 id 的记录从 DB 拉回（含跨租户的），再在 JVM 里过滤掉非本租户的。攻击者传入大量他人设备 id，会触发对这些 id 的 DB 查询（即使结果被丢弃）。
- 影响：信息不泄露（跨租户的被过滤、不返回），但 DB 仍执行了跨租户查询，存在"按 id 探测存在性"的侧信道（响应时间/行数差异）。低危，记技术债。

## 3. 自检

| 检查项 | 结论 |
|---|---|
| 每条 `文件:行` 回读确认 | ✅ 全部回读：D-1 MybatisPlusConfig:51-55、S-1 DeviceServiceImpl:553-566 + DDL:643、S-2 DeviceLockManager:44、S-3 DriverMetadataListener:41、T-1 DeviceDO:48 vs DeviceBO:42、T-2 DeviceServiceImpl:253-260 + DeviceController:208、T-3 BaseController:89-97。 |
| 三分类不混淆 | ✅ D-1 是"契约声明 fail-closed 但无组件执行"=缺陷；S-1/S-2/S-3 是"有明确工程假设的取舍"；T-1/T-2/T-3 是"不影响正确性但待改进"。 |
| `DeviceDO` 是否 extends/implements TenantOwned | ✅ 读类声明行 48 确认：`implements Serializable`，**不实现 TenantOwned**；DO 全部如此，BO 才实现。 |
| requireTenant/filterTenant 覆盖面 | ✅ rg 扫全 22 个 manager controller，无一遗漏；`filterTenant` 27 处调用、`requireTenant`/`requireEntityTenant` 各 controller 均覆盖。 |
| DriverMetadataListener tenant 缺失核查 | ✅ 核查到：接口和 DTO 都不带 tenantId，但属 trusted 内部事件路径（事件源头 manager 已校验），归 S-3 有意简化，非缺陷。 |
| 是否强凑 | ✅ 无强凑。身份/隔离的 controller 层覆盖确实扎实（诚实记录），唯一的真缺陷 D-1 是"框架级兜底缺失"这一架构层问题，非 controller 层 bug。 |

## 4. 结论

身份唯一性（`deviceId`/`deviceCode`）实现稳健：雪花 ID + UUID 双重全局唯一，DB 对 `(tenant_id, device_code)` 有唯一索引强约束，符合 EPC 全局唯一身份理论。

租户隔离在 controller 层覆盖完整（22/22 controller 全调用 `requireTenant`/`filterTenant`/`getTenantId`），但**缺一道框架级兜底**（D-1：`TenantContextHolder` 声称 fail-closed 却无 `TenantLineInnerInterceptor` 执行），隔离纯靠"每处都记得手过滤"的纪律维持。这是本切片唯一的真缺陷，其余为有意简化与技术债。

**计数：真缺陷 1 / 有意简化 3 / 技术债 3**。
