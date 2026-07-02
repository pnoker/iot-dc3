# 冗余 tenant 手过滤清理 — Design Doc

- **日期**：2026-07-02
- **状态**：brainstorm 已确认（B=(a) 逐处审查），待 plan 执行
- **位置**：`iot-dc3/docs/superpowers/specs/`
- **来源**：D-1 修复（commit `0157e9d9`）后续技术债——装 `TenantLineInnerInterceptor` 后，service 层手 tenant 过滤成冗余

## 1. 目标

删拦截器覆盖的冗余手 tenant 过滤，统一到框架隔离（杜绝"手过滤 + 框架"双重维护）。

## 2. 范围（4 模式，基于 Explore + D-1 final review 分析）

- **A**（97 处单表 `.eq(tenant)` / `.eq("tenant_id")`）→ **删**（拦截器自动注入 `tenant_id` 谓词）
- **C**（10 处内存 `.filter(getTenantId)` / `.stream` 后过滤）→ **删**（DB 层已过滤）
- **B**（join 原生 SQL 手 `tenant_id`，如 `and dgb.tenant_id = dd.tenant_id`）→ **逐处审查**（拦截器用 JSqlParser 解析整 SQL 含 join，对每张表注入过滤谓词；但跨表关联 `dgb.tenant_id = dd.tenant_id` 是表间一致不是过滤，需审查是否冗余）
- **D**（55 处 `equals(getTenantId)` 一致性校验，如 update 校验 entity tenant）→ **保留**（业务防御，拦截器不替代）

## 3. 验证铁律

每处删前确认 4 条（缺一不删）：
1. service 走 MyBatis（非 JdbcTemplate/原生）
2. 目标表有 `tenant_id` 列（非白名单 11 表）
3. 非 join 子句的跨表关联（join 关联审 B，不盲删）
4. 删后查询语义不变（拦截器注入等价谓词）

删后跑模块测试 + 最终全套 `mvn test`。

## 4. 分批（按模块 subagent）

| 模块 | service/impl 文件数 |
|---|---|
| dc3-common-dal | 4 |
| dc3-driver（virtual/mqtt） | 2 |
| dc3-common-agentic | 6 |
| dc3-common-auth | 7 |
| dc3-common-data | 7 |
| dc3-common-manager | 18（最大） |

## 5. 决策

- **B = (a) 逐处审查**：每处判断拦截器是否覆盖；覆盖则删、不覆盖（如跨表关联）保留。
- **D 保留**（YAGNI，防御性校验）。
- `requireTenant`/`filterTenant`（controller 层）不动（D-1 已定保留）。

## 6. 风险

| 风险 | 对策 |
|---|---|
| 盲删丢隔离（某处拦截器未覆盖） | 4 条验证铁律 + 全套测试兜底 |
| join 子句误删（拦截器不覆盖跨表关联） | B 逐处审查，保守保留不确定的 |
| 删后测试红（语义变化） | 每模块删后测该模块 + 最终全套 |

## 7. YAGNI

- D（equals 校验）不动
- controller 层 requireTenant/filterTenant 不动
- 不重构 service 方法签名（只删冗余过滤行）
