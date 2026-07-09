# 冗余 tenant 手过滤清理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development`。Steps use checkbox (`- [ ]`)
> 。执行前先读 `docs/superpowers/specs/2026-07-02-tenant-cleanup-redundant-filter-design.md`。

**Goal:** 删 A/C 冗余手 tenant 过滤（107 处）+ B 逐处审查，统一框架隔离。

**Architecture:** 分模块 subagent，每模块清 A/C（删冗余 `.eq(tenant)` 单表 + 内存 filter）+ 审 B（join 原生 SQL 逐处判断）+
测试。

**Tech Stack:** Java 21、MyBatis-Plus 3.5.16、mvn。

## Global Constraints

- **路径根**：`PROJ=/Users/pnoker/Code/pnoker/IoTDC3/github`。后端仓库 `$PROJ/iot-dc3`。
- **分支**：`fix/tenant-cleanup-redundant-filter`（从 `develop` 切，base `0157e9d9`）。
- **commit**：`refactor(tenant):` / `chore(tenant):` scope，**无 Co-Authored-By**。
- ⚠️ **`./mvnw` 损坏**——用
  `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.0.3.jdk mvn ... -Dsurefire.failIfNoSpecifiedTests=false`。
- **验证铁律**（每处删前确认 4 条）：service 走 MyBatis + 表有 tenant_id + 非白名单 + 非 join 跨表关联。删后模块测试。
- **保留**：D（equals 校验）、requireTenant/filterTenant、B 中跨表关联（不确定保留）。

## 通用清理模式（每 task 适用）

**模式 A（删）**：`wrapper.eq(XxxDO::getTenantId, tenantId)` / `.eq("tenant_id", tenantId)` 单表过滤 → 删该行（拦截器自动注入等价谓词）。
**模式 C（删）**：`.stream().filter(e -> equals(tenantId, e.getTenantId()))` 内存过滤 → 删（DB 层已过滤）。
**模式 B（审查）**：原生 SQL 片段 `and dgb.tenant_id = dd.tenant_id`（跨表关联）→ **判断**：若是"过滤当前 tenant"（如
`dgb.tenant_id = ?`）则删（拦截器覆盖）；若是"表间 tenant 一致关联"（如 `dgb.tenant_id = dd.tenant_id`）则**保留**
（拦截器不保证跨表关联）。
**模式 D（保留）**：`if (!Objects.equals(current.getTenantId(), entity.getTenantId()))` 一致性校验 → 不动。

---

### Task 1: dc3-common-dal（4 文件）

**Files:** `dc3-common/dc3-common-dal/src/main/java/.../service/impl/*.java`（含 tenant 用法的 4 个）
**Interfaces:** Consumes D-1 拦截器（已装）；Produces 清理后的 service（隔离由拦截器兜底）

- [ ] 1.1 rg 定位 dal 模块 service/impl 的 A/B/C/D 模式分布（每处 file:line + 模式判定）
- [ ] 1.2 删 A（单表 .eq tenant）+ C（内存 filter），每处确认 4 条验证铁律
- [ ] 1.3 审 B（join 原生 SQL），逐处判断删/保留
- [ ] 1.4 编译 + 测：`mvn -pl dc3-common/dc3-common-dal -am test -Dsurefire.failIfNoSpecifiedTests=false`
- [ ] 1.5 commit: `refactor(tenant): remove redundant hand filter in dal (covered by interceptor)`

### Task 2: dc3-driver（2 文件：virtual/mqtt）

**Files:** `dc3-driver/dc3-driver-virtual/.../service/impl/*.java`、`dc3-driver/dc3-driver-mqtt/.../service/impl/*.java`

- [ ] 2.1-2.5 同 Task 1 流程（定位/删 A+C/审 B/测/commit），scope=`driver`

### Task 3: dc3-common-agentic（6 文件）

**Files:** `dc3-common/dc3-common-agentic/src/main/java/.../service/impl/*.java`

- [ ] 3.1-3.5 同上，scope=`agentic`

### Task 4: dc3-common-auth（7 文件）

**Files:** `dc3-common/dc3-common-auth/src/main/java/.../service/impl/*.java`
**注意**：auth 含 OAuth/service_account/role 等，注意 D-1 已 runIgnore 包裹的 PublicEndpoint 路径（那些 runIgnore 内查询本就无
tenant 过滤，手 .eq(tenant) 在 runIgnore 内无效——确认是否冗余可删）

- [ ] 4.1-4.5 同上，scope=`auth`

### Task 5: dc3-common-data（7 文件）

**Files:** `dc3-common/dc3-common-data/src/main/java/.../service/impl/*.java`

- [ ] 5.1-5.5 同上，scope=`data`

### Task 6: dc3-common-manager（18 文件，最大）

**Files:** `dc3-common/dc3-common-manager/src/main/java/.../service/impl/*.java`
（DeviceServiceImpl/PointServiceImpl/DriverServiceImpl 等）

- [ ] 6.1-6.5 同上，scope=`manager`。**重点**：DeviceServiceImpl/PointServiceImpl 的 join 原生 SQL（Explore 举例 :528-539/:
  329-340）必审 B。

### Task 7: 全套回归

- [ ] 7.1 `mvn test` 全套（确认无隔离破坏、无测试红）
- [ ] 7.2 若红：区分"删错丢隔离"vs"测试 setup 缺 tenant"vs"既有红"，按 D-1 Task6 经验处理
- [ ] 7.3 commit 若有 fix

---

## Self-Review

- **Spec 覆盖**：A/C 删（Task1-6）+ B 审（Task1-6 各审）+ D 保留（Global Constraints）+ 验证（铁律 + Task7 全套）✅
- **顺序**：小模块先（dal/driver 试水）→ 大模块（manager 最后），每模块独立可测 ✅
- **风险控制**：4 条验证铁律 + B 逐处审 + 全套测试兜底 ✅
