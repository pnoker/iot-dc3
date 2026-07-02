# D-1 租户隔离框架兜底 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development`。Steps use checkbox (`- [ ]`)。执行前先读 `docs/superpowers/specs/2026-07-02-d1-tenant-line-interceptor-design.md`。

**Goal:** 落地 MyBatis-Plus `TenantLineInnerInterceptor` 框架级 fail-closed 租户隔离，修 D-1 真缺陷。

**Architecture:** 自定义 `TenantLineHandler`（读 `TenantContextHolder`，null 时抛异常 fail-closed，`ignoreTable` 覆盖 11 张无 tenant 表 + `isIgnored()` 上下文）注册到 `MybatisPlusConfig`（租户拦截器在分页前）；tenant-free 路径用 `runIgnoreAction` 包裹。

**Tech Stack:** Java 21、Spring Boot、MyBatis-Plus 3.5.16（`TenantLineHandler`/`TenantLineInnerInterceptor`、JSqlParser `LongValue`/`Expression`）、JUnit 5、Reactor Mono。

## Global Constraints

- **路径根**：`PROJ=/Users/pnoker/Code/pnoker/IoTDC3/github`。后端仓库 `$PROJ/iot-dc3`。
- **分支**：`fix/d1-tenant-line-interceptor`（从 `develop` 切，base `21b5b2bfb`）。
- **commit**：`fix(...)` / `feat(...)` scope，**无 Co-Authored-By**。
- **MyBatis-Plus 3.5.16**：`TenantLineHandler.getTenantId()` 返回 `net.sf.jsqlparser.expression.Expression`（不是 Long）；`ignoreTable(String)` 返回 boolean；拦截器**必须租户在前、分页在后**。
- **fail-closed**：`getTenantId() == null && !isIgnored()` → 抛 `TenantNotScopedException`（不返回 null——默认 null 会注入 `tenant_id = NULL` 静默错误）。
- **验证**：每个 task 跑相关模块测试 `./mvnw -pl <module> -am test`；全部完成后跑 `./mvnw test`。

## File Structure

**创建**：
- `dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/exception/TenantNotScopedException.java`
- `dc3-common/dc3-common-postgres/src/main/java/io/github/pnoker/common/config/TenantLineHandlerImpl.java`
- `dc3-common/dc3-common-postgres/src/test/java/io/github/pnoker/common/config/TenantLineHandlerImplTest.java`

**修改**：
- `dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/tenant/TenantContextHolder.java`（加 `runIgnoreAction` 变体）
- `dc3-common/dc3-common-postgres/src/main/java/io/github/pnoker/common/config/MybatisPlusConfig.java`（注册拦截器）
- `dc3-common/dc3-common-web/src/main/java/io/github/pnoker/common/config/ExceptionConfig.java`（映射 500）
- 6 个 agentic BO（补 `implements TenantOwned`）
- tenant-free 路径（`runIgnoreAction` 包裹，见 Task 5/6）

---

### Task 1: 基础设施 — TenantNotScopedException + runIgnoreAction + ExceptionConfig 映射

**Files:**
- Create: `dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/exception/TenantNotScopedException.java`
- Modify: `dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/tenant/TenantContextHolder.java`
- Modify: `dc3-common/dc3-common-web/src/main/java/io/github/pnoker/common/config/ExceptionConfig.java`
- Test: `dc3-common/dc3-common-postgres/src/test/java/io/github/pnoker/common/tenant/TenantContextHolderTest.java`（加 runIgnoreAction 用例）

**Interfaces:**
- Produces: `TenantNotScopedException(String)`、`TenantContextHolder.runIgnoreAction(Runnable)`（void 变体，委托 `runIgnore`）

- [ ] **1.1 创建 `TenantNotScopedException`**

```java
package io.github.pnoker.common.exception;

/**
 * Thrown when a tenant-scoped query runs without a tenant id bound to the thread and
 * the ignore flag is not set — i.e. a code path forgot to establish tenant context
 * (via BaseController.async) or forgot to wrap cross-tenant work in runIgnore.
 * Maps to HTTP 500: this is a programming error, not a client error.
 */
public class TenantNotScopedException extends RuntimeException {

    public TenantNotScopedException(String message) {
        super(message);
    }
}
```

- [ ] **1.2 加 `runIgnoreAction` 到 `TenantContextHolder`**（void 路径用，避免 `return null` 别扭）

在现有 `runIgnore(Supplier)` 后追加：

```java
/**
 * Void variant of {@link #runIgnore(Supplier)} for actions with no result.
 * Same nesting/exception safety.
 */
public static void runIgnoreAction(Runnable action) {
    boolean previous = isIgnored();
    IGNORE.set(Boolean.TRUE);
    try {
        action.run();
    } finally {
        IGNORE.set(previous);
    }
}
```

- [ ] **1.3 `TenantContextHolderTest` 加 runIgnoreAction 用例**

```java
@Test
void runIgnoreActionDisablesFilteringAndRestores() {
    assertThat(TenantContextHolder.isIgnored()).isFalse();
    TenantContextHolder.runIgnoreAction(() -> {
        assertThat(TenantContextHolder.isIgnored()).isTrue();
    });
    assertThat(TenantContextHolder.isIgnored()).isFalse();
}

@Test
void runIgnoreActionRestoresOnException() {
    assertThat(TenantContextHolder.isIgnored()).isFalse();
    assertThatThrownBy(() ->
        TenantContextHolder.runIgnoreAction(() -> { throw new IllegalStateException("boom"); })
    ).isInstanceOf(IllegalStateException.class);
    assertThat(TenantContextHolder.isIgnored()).isFalse();
}
```

- [ ] **1.4 `ExceptionConfig` 加 500 映射**（先 Read `ExceptionConfig.java` 看现有 `@ExceptionHandler` 模式，照模式加）

```java
@ExceptionHandler(TenantNotScopedException.class)
public Mono<R<Void>> handleTenantNotScoped(TenantNotScopedException e) {
    log.error("Tenant-scoped query executed without tenant context", e);
    return Mono.just(R.fail(500, "System error: tenant scope missing"));
}
```
（`R.fail` 签名按现有模式；import `TenantNotScopedException`）

- [ ] **1.5 验证 + commit**

```bash
./mvnw -pl dc3-common/dc3-common-constant -am test
./mvnw -pl dc3-common/dc3-common-web -am test
```
Expected: PASS。
```bash
git add dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/exception/TenantNotScopedException.java \
        dc3-common/dc3-common-constant/src/main/java/io/github/pnoker/common/tenant/TenantContextHolder.java \
        dc3-common/dc3-common-constant/src/test/java/io/github/pnoker/common/tenant/TenantContextHolderTest.java \
        dc3-common/dc3-common-web/src/main/java/io/github/pnoker/common/config/ExceptionConfig.java
git commit -m "feat(tenant): add TenantNotScopedException + runIgnoreAction + 500 mapping"
```

---

### Task 2: TenantLineHandlerImpl（fail-closed + ignoreTable 白名单）

**Files:**
- Create: `dc3-common/dc3-common-postgres/src/main/java/io/github/pnoker/common/config/TenantLineHandlerImpl.java`
- Test: `dc3-common/dc3-common-postgres/src/test/java/io/github/pnoker/common/config/TenantLineHandlerImplTest.java`

**Interfaces:**
- Consumes: `TenantContextHolder.getTenantId()`/`isIgnored()`、`TenantNotScopedException`
- Produces: `TenantLineHandler` bean（供 Task 3 注入）

- [ ] **2.1 写失败测试 `TenantLineHandlerImplTest`**

```java
package io.github.pnoker.common.config;

import io.github.pnoker.common.exception.TenantNotScopedException;
import io.github.pnoker.common.tenant.TenantContextHolder;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantLineHandlerImplTest {

    private final TenantLineHandlerImpl handler = new TenantLineHandlerImpl();

    @AfterEach
    void clear() { TenantContextHolder.clear(); }

    @Test
    void getTenantIdReturnsLongValueWhenBound() {
        TenantContextHolder.setTenantId(42L);
        assertThat(handler.getTenantId()).isInstanceOf(LongValue.class);
        assertThat(((LongValue) handler.getTenantId()).getValue()).isEqualTo(42L);
    }

    @Test
    void getTenantIdThrowsWhenNullAndNotIgnored() {
        // no setTenantId, not ignored → fail-closed
        assertThatThrownBy(() -> handler.getTenantId())
            .isInstanceOf(TenantNotScopedException.class);
    }

    @Test
    void ignoreTableTrueForWhitelistedTables() {
        assertThat(handler.ignoreTable("dc3_tenant")).isTrue();
        assertThat(handler.ignoreTable("dc3_user")).isTrue();
        assertThat(handler.ignoreTable("dc3_resource")).isTrue();
    }

    @Test
    void ignoreTableFalseForTenantScopedTable() {
        TenantContextHolder.setTenantId(1L); // not ignored
        assertThat(handler.ignoreTable("dc3_device")).isFalse();
    }

    @Test
    void ignoreTableTrueForAllTablesWhenIgnored() {
        TenantContextHolder.runIgnoreAction(() -> {
            assertThat(handler.ignoreTable("dc3_device")).isTrue();  // normally scoped
            assertThat(handler.ignoreTable("dc3_point")).isTrue();
        });
    }

    @Test
    void getTenantIdColumnIsTenantId() {
        assertThat(handler.getTenantIdColumn()).isEqualTo("tenant_id");
    }
}
```

- [ ] **2.2 跑测试确认失败**

```bash
./mvnw -pl dc3-common/dc3-common-postgres -am test -Dtest=TenantLineHandlerImplTest
```
Expected: FAIL（`TenantLineHandlerImpl` 不存在）。

- [ ] **2.3 实现 `TenantLineHandlerImpl`**

```java
package io.github.pnoker.common.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import io.github.pnoker.common.exception.TenantNotScopedException;
import io.github.pnoker.common.tenant.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * MyBatis-Plus tenant-line handler backed by {@link TenantContextHolder}.
 * <p>
 * Fail-closed: when no tenant id is bound and the thread is not in a
 * {@link TenantContextHolder#runIgnoreAction} scope, {@link #getTenantId()} throws
 * {@link TenantNotScopedException} rather than letting the query run unscoped.
 * Tables without a tenant_id column (system/lookup tables) are whitelisted in
 * {@link #ignoreTable} so the interceptor does not inject a non-existent column.
 */
@Component
public class TenantLineHandlerImpl implements TenantLineHandler {

    /** Tables without a tenant_id column — interceptor must NOT inject for these. */
    private static final Set<String> IGNORE_TABLES = Set.of(
        "dc3_tenant",
        "dc3_principal",
        "dc3_user",
        "dc3_local_credential",
        "dc3_external_identity",
        "dc3_resource",
        "dc3_role_resource_bind",
        "dc3_api",
        "dc3_mcp_tool_catalog",
        "dc3_mcp_connection_tool",
        "dc3_menu"
    );

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            // isIgnored() paths are routed through ignoreTable (returns true) and never reach here.
            throw new TenantNotScopedException(
                "Tenant-scoped query executed without tenant id on thread; "
                + "wrap cross-tenant/tenant-free work in TenantContextHolder.runIgnoreAction");
        }
        return new LongValue(tenantId);
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // Ignored context (runIgnoreAction) → skip injection for ALL tables.
        // Otherwise skip whitelisted (tenant_id-less) tables.
        return TenantContextHolder.isIgnored() || IGNORE_TABLES.contains(tableName);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }
}
```

- [ ] **2.4 跑测试确认通过**

```bash
./mvnw -pl dc3-common/dc3-common-postgres -am test -Dtest=TenantLineHandlerImplTest
```
Expected: PASS（6/6）。

- [ ] **2.5 commit**

```bash
git add dc3-common/dc3-common-postgres/src/main/java/io/github/pnoker/common/config/TenantLineHandlerImpl.java \
        dc3-common/dc3-common-postgres/src/test/java/io/github/pnoker/common/config/TenantLineHandlerImplTest.java
git commit -m "feat(tenant): add TenantLineHandlerImpl with fail-closed + whitelist"
```

---

### Task 3: 注册拦截器（租户在前）+ agentic 6 BO 补 TenantOwned

**Files:**
- Modify: `dc3-common/dc3-common-postgres/src/main/java/io/github/pnoker/common/config/MybatisPlusConfig.java`
- Modify: 6 个 agentic BO（`dc3-common/dc3-common-agentic/src/main/java/io/github/pnoker/common/agentic/bo/*.java`）：`AttachmentBO`、`ActionBO`、`MessageBO`、`ModelProviderBO`、`ModelConfigBO`、`SessionBO`

**Interfaces:**
- Consumes: `TenantLineHandlerImpl`（Task 2）、`TenantOwned`（`dc3-common-public`）

- [ ] **3.1 改 `MybatisPlusConfig`**（注入 handler，租户拦截器在分页前）

```java
@Bean
@ConditionalOnMissingBean
public MybatisPlusInterceptor mybatisPlusInterceptor(TenantLineHandler tenantLineHandler) {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    // Order matters: tenant-line MUST be added before pagination
    interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
    return interceptor;
}
```
import：`com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor`、`com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler`。保留 `@ConditionalOnMissingBean` / `@AutoConfiguration` / `@EnableTransactionManagement`。

- [ ] **3.2 agentic 6 BO 补 `implements TenantOwned`**

逐个 Read 每个 BO，确认已有 `tenantId` 字段（DDL 表有 tenant_id，DO 应映射）。若 BO 有 `getTenantId()`（来自 `BaseBO` 或字段），加 `implements TenantOwned`；若无，加字段 delegate。

样例（`AttachmentBO`，先 Read 看现状）：
```java
// 若已有 tenantId 字段（来自 BaseBO）：
public class AttachmentBO extends BaseBO implements TenantOwned {
    // getTenantId() 已由 BaseBO 提供 → 加 implements 即可
}
// 若无 tenantId 字段：加 private Long tenantId; （从 DO 映射）
```
6 个 BO 都如此处理（先 Read 判定）。

- [ ] **3.3 验证编译 + 测试**

```bash
./mvnw -pl dc3-common/dc3-common-postgres -am test   # MybatisPlusConfig 装配
./mvnw -pl dc3-common/dc3-common-agentic -am compile  # 6 BO 编译
```
Expected: 编译通过 + 测试 PASS。

- [ ] **3.4 commit**

```bash
git add dc3-common/dc3-common-postgres/src/main/java/io/github/pnoker/common/config/MybatisPlusConfig.java \
        dc3-common/dc3-common-agentic/src/main/java/io/github/pnoker/common/agentic/bo/
git commit -m "feat(tenant): register TenantLineInnerInterceptor + agentic BO implements TenantOwned"
```

---

### Task 4: runIgnore 包裹启动路径（InitRunner + EventListener）

**Files:** 逐个 Read 判定（清单见下）；用 `TenantContextHolder.runIgnoreAction(...)` 包裹 `run()` / 事件回调体。

**判定规则**（每处先 Read 看方法体）：
- 方法体**实际查 DB**（调 `*Service.*` / mapper）→ 用 `runIgnoreAction` 包裹整个方法体
- 方法体**空或仅日志**（如 AuthInitRunner 当前空）→ **跳过**，不改

**清单**（逐个 Read `run()`/回调体判定）：
- `dc3-common/dc3-common-auth/.../init/AuthInitRunner.java`（run，可能空）
- `dc3-common/dc3-common-manager/.../init/ManagerInitRunner.java`
- `dc3-common/dc3-common-data/.../init/DataInitRunner.java`
- `dc3-common/dc3-common-driver/.../init/DriverInitRunner.java`
- `dc3-common/dc3-common-thread/.../init/ThreadInitRunner.java`
- `dc3-common/dc3-common-mqtt/.../init/MqttInitRunner.java`
- `dc3-common/dc3-common-data/.../biz/impl/EntityStateExpiryScanner.java`（`@EventListener` 方法）
- `dc3-common/dc3-common-resource-registrar/.../ResourceRegistrar.java`（`@EventListener(ApplicationReadyEvent.class)` 方法，查 `dc3_resource` 白名单表——白名单已处理，**仅当另查有 tenant 表才包**）

**包裹模式**：
```java
@Override
public void run(ApplicationArguments args) {
    TenantContextHolder.runIgnoreAction(() -> {
        // 原 run() 体（DB 查询逻辑）
    });
}
```
EventListener 方法同理。import `io.github.pnoker.common.tenant.TenantContextHolder`。

- [ ] **4.1 逐个 Read 清单内 8 个文件，标注哪些需包裹、哪些跳过（空/仅查白名单表）**
- [ ] **4.2 对需包裹的，用 `runIgnoreAction` 包裹方法体**
- [ ] **4.3 编译 + 单测**

```bash
./mvnw -pl dc3-common/dc3-common-auth,dc3-common/dc3-common-manager,dc3-common/dc3-common-data,dc3-common/dc3-common-driver,dc3-common/dc3-common-thread,dc3-common/dc3-common-mqtt -am compile
```
Expected: 编译通过。

- [ ] **4.4 commit**

```bash
git add -A
git commit -m "feat(tenant): wrap startup tenant-free paths in runIgnoreAction"
```

---

### Task 5: runIgnore 包裹请求路径（OAuthController + 跨租户 service）

**Files:** 逐个 Read 判定（清单见下）。

**判定规则**：
- 路径在**认证前**（无 tenant 上下文）且查有 tenant_id 表 → 包裹
- 路径在 `BaseController.async` 内（已有 tenant）→ **不包**
- 查 `dc3_tenant`/`dc3_user`/`dc3_resource` 等白名单表 → 白名单已处理，**仅当另查有 tenant 表才包**

**清单**：
- `dc3-common/dc3-common-auth/.../controller/OAuthController.java`（登录/token 签发，认证前）
- `dc3-common/dc3-common-auth/.../controller/TenantController.java`（查 `dc3_tenant` 白名单表——多半不需；**仅当某方法另查有 tenant 表才包**该方法的跨租户部分）
- `dc3-common/dc3-common-auth/.../controller/DictionaryForAuthController.java`（字典查询，看表）
- `dc3-common/dc3-common-driver/.../service/DriverMetadataListener.java`（驱动元数据事件，trusted 内部路径）
- `dc3-common/dc3-common-gateway/.../service/impl/FilterServiceImpl.java`
- `dc3-common/dc3-common-driver/.../service/DriverSenderServiceImpl.java`（若存在该类）
- `dc3-common/dc3-common-manager/.../service/impl/TopicServiceImpl.java`
- `dc3-common/dc3-common-data/.../service/impl/DashboardServiceImpl.java`

**包裹模式**（service 方法）：
```java
public XxxBO someInternalMethod(Long id) {
    return TenantContextHolder.runIgnore(() -> {
        // 原跨租户查询逻辑
    });
}
// void 方法：
TenantContextHolder.runIgnoreAction(() -> { ... });
```

- [ ] **5.1 逐个 Read 清单内文件，标注每方法是否查有 tenant_id 表 + 是否在 async 外 → 判定包不包**
- [ ] **5.2 对需包裹的，用 `runIgnore`/`runIgnoreAction` 包裹**
- [ ] **5.3 编译**

```bash
./mvnw compile -pl dc3-common/dc3-common-auth,dc3-common/dc3-common-driver,dc3-common/dc3-common-gateway,dc3-common/dc3-common-manager,dc3-common/dc3-common-data -am
```
Expected: 通过。

- [ ] **5.4 commit**

```bash
git add -A
git commit -m "feat(tenant): wrap request-time tenant-free paths in runIgnore"
```

---

### Task 6: 集成验证 + 全套回归

**Files:** 无新文件（验证性质）

- [ ] **6.1 跑全套测试**（发现遗漏 runIgnore 的路径会以 `TenantNotScopedException` 暴露）

```bash
./mvnw test
```
Expected: 全 PASS。**若某测试因 `TenantNotScopedException` 失败**：说明该路径漏了 tenant 上下文——回 Task 4/5 补 `runIgnoreAction`，再跑。

- [ ] **6.2 启动冒烟**（可选，若本地有 postgres/testcontainers 基建）

确认应用能起来（InitRunner 不抛 `TenantNotScopedException`）。

- [ ] **6.3 更新 identification-defects.md**（标注 D-1 已修）

`docs/superpowers/analysis/identification-defects.md` D-1 段加：**已修复（2026-07-02，commit <sha>）：落地 TenantLineInnerInterceptor + fail-closed handler**。

- [ ] **6.4 commit**

```bash
git add docs/superpowers/analysis/identification-defects.md
git commit -m "docs(superpowers): mark D-1 fixed in identification defect analysis"
```

---

## Self-Review

- **Spec 覆盖**：handler（Task 2）+ 拦截器注册（Task 3）+ ignoreTable 白名单 11 表（Task 2）+ runIgnore 包裹 tenant-free（Task 4/5）+ agentic 6 BO（Task 3）+ TenantNotScopedException 500（Task 1）+ 测试（各 Task）✅
- **决策落地**：144 处 wrapper.eq 不在本 plan（后续技术债，Task 6 不动）；requireTenant 保留（本 plan 不撤）；fail-closed→500（Task 1.4）✅
- **fail-closed 正确性**：`ignoreTable` 在 `isIgnored()` 时对所有表返回 true → runIgnore 上下文不调 `getTenantId` → 不会误抛；非 ignore + null tenant → `getTenantId` 抛异常 ✅
- **顺序**：Task 1（基础设施）→ Task 2（handler）→ Task 3（注册 + BO）→ Task 4/5（包裹）→ Task 6（回归）。Task 4/5 依赖 Task 1-3（拦截器装了才需包裹）✅
- **runIgnore 清单精度**：Explore 清单可能过宽（AuthInitRunner 空、TenantController 查白名单表）——Task 4.1/5.1 加"先 Read 判定每处是否真需"步骤，避免过度包裹（过度包裹会削弱隔离）✅

## 执行交接

见下（subagent-driven vs inline）。
