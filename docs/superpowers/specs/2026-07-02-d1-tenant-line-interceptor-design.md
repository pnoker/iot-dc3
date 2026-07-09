# D-1 修复：租户隔离框架兜底（TenantLineInnerInterceptor）— Design Doc

- **日期**：2026-07-02
- **状态**：brainstorm 已确认，待 plan 执行
- **位置**：`iot-dc3/docs/superpowers/specs/`
- **来源**：identification 切片 D-1 真缺陷（`analysis/identification-defects.md`）；final reviewer (opus) 维持真缺陷分类

## 1. 背景与目标

identification 切片发现 D-1：`TenantContextHolder` 的 Javadoc 声称由 MyBatis-Plus tenant-line handler 消费、fail-closed，但仓库无
`TenantLineInnerInterceptor`（`MybatisPlusConfig` 只装 `PaginationInnerInterceptor`）。租户隔离实际靠 controller 手过滤（23/23
全覆盖）+ service `wrapper.eq(tenant_id)`，**无框架兜底**——任何绕过 controller 的 service 直调（如 `listByIds`）无租户过滤。

本修复落地框架级 fail-closed 隔离（A 方案），兑现 Javadoc 承诺，使任何 service 查询都受 SQL 层租户过滤。

## 2. 现状（探索结论）

- **基础设施已就位**：`TenantContextHolder`（`runIgnore`/`clear`/带测试 `TenantContextHolderTest`）、`TenantOwned` 接口、
  `BaseController.async` 的 set/clear 钩子
- **缺口 4 项**：(a) `MybatisPlusConfig` 未注册 `TenantLineInnerInterceptor`；(b) 自定义 `TenantLineHandler` 实现；(c)
  `ignoreTable` 白名单；(d) `runIgnore` 包裹 tenant-free 路径
- **MyBatis-Plus 3.5.16**（`pom.xml:92`）：`TenantLineHandler` 接口 `getTenantId()`/`ignoreTable()`/`getTenantIdColumn()`；*
  *默认 `getTenantId` 返回 null 会注入 `tenant_id=NULL`（静默错误），fail-closed 必须自实现**
- **`@ConditionalOnMissingBean`**：`MybatisPlusConfig:50` 是唯一的 `MybatisPlusInterceptor` bean，可直接改

## 3. 设计

### 3.1 `TenantLineHandlerImpl`（新增，`dc3-common-postgres/config`）

- `getTenantId()`：返回 `TenantContextHolder.getTenantId()`；**null 时抛 `TenantNotScopedException`**（fail-closed）
- `ignoreTable(name)`：`TenantContextHolder.isIgnored() || 白名单.contains(name)` → true 时跳过注入（runIgnore 上下文 + 无
  tenant 表都安全）
- `getTenantIdColumn()`：`"tenant_id"`

### 3.2 `MybatisPlusConfig` 改

`addInnerInterceptor(tenantLine)` 加在 `addInnerInterceptor(pagination)` **之前**（MyBatis-Plus 顺序要求：租户在分页前）

### 3.3 `ignoreTable` 白名单（11 张无 tenant_id 表）

`dc3_tenant`、`dc3_principal`、`dc3_user`、`dc3_local_credential`、`dc3_external_identity`、`dc3_resource`、
`dc3_role_resource_bind`、`dc3_api`、`dc3_mcp_tool_catalog`、`dc3_mcp_connection_tool`、`dc3_menu`

### 3.4 `runIgnore` 包裹 tenant-free 路径

- **6 InitRunner**：`AuthInitRunner`、`ManagerInitRunner`、`DataInitRunner`、`DriverInitRunner`、`ThreadInitRunner`、
  `MqttInitRunner`
- **2 EventListener**：`EntityStateExpiryScanner`、`ResourceRegistrar`
- **`OAuthController`**（登录签发）、**`TenantController`**（租户管理本身跨租户）、**`DictionaryForAuthController`**
- **跨租户编排**：`DriverMetadataListener`、`FilterServiceImpl`、`DriverSenderServiceImpl`、`TopicServiceImpl`、
  `DashboardServiceImpl`

### 3.5 agentic 6 BO 补 `implements TenantOwned`

`AttachmentBO`、`ActionBO`、`MessageBO`(agentic)、`ModelProviderBO`、`ModelConfigBO`、`SessionBO`

### 3.6 `TenantNotScopedException`（新增，`dc3-common-constant/exception`）

- `RuntimeException` 子类
- 全局异常处理（`ExceptionConfig`）映射 **500 + error log**

## 4. 决策

- **双重过滤 144 处 `wrapper.eq(tenant_id)`**：后续技术债分批清（装拦截器后冗余 `tenant_id=? AND tenant_id=?`
  但不报错，非阻塞；清是独立工作，混入会让本次 PR 过大）
- **`requireTenant`/`filterTenant`**：保留（defense-in-depth + 404 不暴露跨租户存在性语义，拦截器不替代）
- **fail-closed 异常**：`TenantNotScopedException` → 500 + error log（触发即"某路径漏了 tenant 上下文"的代码
  bug，非用户错；生产不应触发）
- **agentic 6 BO**：本次补 `TenantOwned`（统一接口边界，工作量小）

## 5. 测试

- **`TenantLineHandlerImpl` 单测**：fail-closed（null + 非 ignore 抛异常）、ignoreTable 白名单命中、`runIgnore` 上下文跳过
- **集成测试**：跨租户查询被拦（查不到他租户数据）、`runIgnore` 路径正常、6 InitRunner 启动不抛
- **现有测试不破坏**：`TenantContextHolderTest` 等

## 6. 风险

| 风险                                       | 对策                                   |
|------------------------------------------|--------------------------------------|
| tenant-free 路径遗漏 → 启动/请求 fail-closed 抛异常 | 全面盘点（§3.4 清单）+ 集成测试覆盖 InitRunner 启动  |
| 拦截器与现有 `wrapper.eq` 双重过滤                 | 冗余不报错；144 处后续清（§4）                   |
| `TenantController` 跨租户查询本身               | `runIgnore` 包裹（§3.4）                 |
| 拦截器顺序错（分页先于租户）                           | `tenantLine` 加在 `pagination` 前（§3.2） |

## 7. YAGNI

- 144 处 `wrapper.eq` 清理（后续技术债，不在本切片）
- `requireTenant`/`filterTenant` 撤除（保留）
- 自定义 `ignoreTable` 配置文件（硬编码 11 表名足够，表名固定）

## 8. 完成标准

- `TenantLineHandlerImpl` 注册且 fail-closed 生效（null + 非 ignore → 异常）
- 11 张无 tenant 表 + `runIgnore` 路径不被拦截器误杀
- 6 InitRunner 启动不抛异常
- 所有现有测试通过 + 新单测/集成测试覆盖
- 跨租户 service 直调被 SQL 层拦截
