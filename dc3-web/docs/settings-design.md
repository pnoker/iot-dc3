# Settings 页面设计规范 + 改造清单

> 目标：统一 Settings 模块的菜单结构与页面布局，杜绝重复造轮子，形成可复用的「家族化」设计。
> 本文档既是改造清单，也是后续新增 Settings 页面的规范基线。

## 一、菜单结构

### 1.1 当前问题

1. 顶层 20 项过于扁平；身份类 7 项平铺，而功能更小的 Model/Command/Event 反而做了二级分组——分组粒度自相矛盾。
2. `身份审计`（顶部）与 `MCP 审计`（底部）同类却分隔。
3. `服务账号 / MCP 服务 / MCP 审计` 散落末尾，无归组。
4. `资源管理` 与 `服务账号` 共用 `Key` 图标。
5. 目录与菜单归属错位：`views/settings/event/` 下的 `Overview/DeviceEvent/DriverEvent/PointEvent.vue` 在菜单上全挂
   `Alarm` 分组。
6. Alarm 分组臃肿（11 子项）：混了配置类、监测类、实体告警三类。

### 1.2 目标菜单树（已采纳）

```
设置
├─ 身份 (Identity)
│   ├─ 用户  ├─ 主体  ├─ 租户成员  ├─ 本地凭证  └─ 服务账号
├─ 权限 (Access Control)
│   ├─ 角色  ├─ 角色主体绑定  ├─ 资源  ├─ API 接口  └─ 菜单
├─ 模型 (Model)
│   ├─ 模型配置  └─ 模型供应商
├─ 告警 (Alarm)
│   ├─ 告警配置：规则 / 通知策略 / 消息模板 / 通知渠道 / 渠道绑定
│   └─ 告警监测：概览 / 运行状态 / 历史 / 驱动告警 / 设备告警 / 点位告警
├─ 事件与指令 (Event & Command)
│   ├─ 事件历史  └─ 指令历史
├─ 审计 (Audit)
│   ├─ 身份审计  └─ MCP 审计
├─ 集成 (Integration)
│   └─ MCP 服务
└─ 系统 (System)
    ├─ 分组  ├─ 标签  └─ 关于
```

> 架构事实：当前「分组」= 带 `children` 的一级节点。本方案新增「身份/权限/审计/集成/系统」5 个分组容器，
> 并把现有 16 个直达一级项降为二级。trade-off：侧边栏多一层折叠、叶子点击路径变深，换取结构一致。

### 1.3 每处改动需同步的层（漏一层即菜单错乱）

`src/config/settingsNav.ts`
→ `SETTINGS_FALLBACK_SIDEBAR`（分组/顺序）
→ 各分组 `*_CHILDREN` 常量
→ `SETTINGS_TITLE_KEYS` / `SETTINGS_FALLBACK_ICON`（新增分组码）
→ `SETTINGS_GROUP_OPENERS`（每个叶子 → 所属新分组）
→ `SETTINGS_ROUTE_ALIAS`（分组容器 → 默认子项）
→ `SETTINGS_ACTIVE_ALIAS`
→ `SETTINGS_BREADCRUMB_PARENTS`（叶子面包屑父级改为新分组）
`src/config/router/settings.ts`（分组容器路由 + 重定向）+ `operate.ts`（详情路由）
`src/config/i18n/locales/{zh,en}.ts`（`nav.*` 新增分组 key）
`src/views/settings/Layout.vue`（nameMap + icon fallback）
`src/views/settings/Settings.vue`（硬编码菜单码引用）
seed SQL `iot-dc3/dc3/dependencies/postgres/initdb/02-iot-dc3-auth.sql`（`dc3_menu`：新增分组行 + 调整子项
`parent_menu_id` / `menu_index`）
Podman 运行库（对 `dc3_auth.dc3_menu` 执行同等更新）

### 1.4 已定决策点

- **D1 目录归位**：`views/settings/event/` 下的告警实体组件（Overview/DeviceEvent/DriverEvent/PointEvent）物理移动到
  `views/settings/alarm/`，目录与菜单对齐；同步修正 import 与 router 引用。
- **D2 图标去重**：`服务账号` 改用与 `资源`(`Key`) 不同的图标（如 `Postcard`/`Avatar`）。

## 二、布局家族化

### 2.1 样板：alarm 配置驱动三件套

- `alarm/alarmEntityConfig.ts`：纯配置（columns/fields/searchProp/filterProp/defaultForm/CRUD API）。
- `alarm/useAlarmEntityPage.ts`：通用 composable（分页/搜索/JSON 校验/增删改/tagType/formatCell/详情路由跳转）。
- `alarm/AlarmNotify.vue`：通用模板（ToolCard + 表格 + dialog 表单），按 `activeConfig` 渲染。

三处简化前提需在泛化时打开：① 搜索区只有单 keyword + 单 filter；② 操作列写死 detail/edit/delete；③ detail 跳固定路由映射表。

### 2.2 泛化引擎（新增，不破坏 alarm）

```
src/composables/useEntityListPage.ts     ← 由 useAlarmEntityPage 泛化
src/components/entity/EntityListPage.vue  ← 由 AlarmNotify.vue 泛化
EntityListConfig 类型                      ← 由 AlarmEntityConfig 扩展
```

`EntityListConfig` 在 alarm 配置基础上扩展：

- `searchFields: FieldConfig[]`：支持多搜索字段（input/select/enableFlag），替代单 keyword+filter。
- `rowActions`：内置 `detail/edit/delete` + `extraActions[]`（如
  assignRoles/assignResources）；操作列宽度按动作数自动取档（1→100 / 2-3→180 / 4+→260-310）。
- `detail`：`{ mode: 'route'|'drawer'|'none', routeName? }`；`editMode: 'dialog'|'route'`。

### 2.3 推广批次（由简到繁，每批独立验证）

> 进度（2026-06-18）：引擎三件套（`config/types/entityList.ts` + `composables/useEntityListPage.ts` +
> `components/entity/EntityListPage.vue`）已落地；批 1～3 全部完成。详见
> `docs/superpowers/plans/2026-06-18-settings-entity-list-engine.md`。

1. ✅ Label / Group（动作少）—— 验证引擎正确性。引擎增强：treeSelect 表单感知（同类型过滤 + 排除自身/子孙）。
2. ✅ Resource / Menu / Api（Api 只读，验证 `editable=false`）。引擎增强：树模式列表、行感知 relations（实体名解析）、`link` 列、
   `menuExt` 嵌套列 + 图标列、`fromRow`/`toPayload` 表单↔载荷钩子。
3. ✅ User / Role（带 assignRoles/assignResources + 路由 tabs 详情，详情页/分配组件保持原样，引擎仅负责跳转/打开）。
4. ⏳ alarm 自身回迁到新引擎，删除 alarm 专用三件套，消除重复（待办）。

### 2.4 风格偏差收敛

| 偏差     | 现状                                                   | 收敛目标                                              |
| -------- | ------------------------------------------------------ | ----------------------------------------------------- |
| 详情展示 | User/Role/alarm 走路由；Command/Event 用 `el-drawer`   | 统一路由 tabs 详情页（DetailCard + el-tabs）          |
| 编辑方式 | 多数 `el-dialog` EditForm；LocalCredential 自写 dialog | 统一 EditForm；详情页内编辑启用 InfoCard（现 0 使用） |
| 列表形态 | 多数表格；Command/Event 卡片网格                       | **D3：统一为表格**                                    |
| 操作列   | 宽度 120~320 不一、按钮顺序不一                        | 规范按钮顺序 `detail→edit→[extra]→delete` + 宽度档位  |

### 2.5 复用基线（新增页面必须遵循）

- 列表页骨架：`ToolCard`（搜索+分页）→ `BlankCard`（表格容器）→ `el-table.settings-table`。
- 详情页骨架：`BlankCard` → `el-tabs` → `DetailCard` → `el-descriptions`。
- 编辑：`el-dialog.things-dialog` + `el-form label-position="top"`，页脚 `things-dialog-footer`（取消/重置/确认）。
- 启用状态用 `EnableTag`；时间列用 `timestampColumn` formatter；启用筛选用 `EnableFlagSegmented`。

## 三、风险

- 高：菜单 7+ 层联动，漏一层即面包屑/高亮/展开错乱；改完跑 `pnpm test:guard`。
- 高：泛化引擎须完全覆盖 alarm 现有能力（JSON 字段校验、tag 着色、time 格式化）才能回迁。
- 中：配置驱动牺牲灵活性，User assignRoles、Api 只读等特例靠 extraActions/editable 兜住。
- 中：D1 移动 event 目录会动 import 路径。
- 验证门槛：`pnpm check` + `pnpm lint:check` + `pnpm test:guard` + `pnpm test`，涉及页面跑 `pnpm test:e2e`。

复杂度：菜单治理 = 中；引擎泛化 + 推广 = 高。

## 四、执行顺序

1. 【本批】菜单结构治理（8 大分组 + D1 目录归位 + D2 图标去重）。
2. 泛化引擎 + Label 试点。
3. 成批推广（Group/Resource/Menu/Api → User/Role）。
4. 偏差收敛（D3 Command/Event 改表格、LocalCredential、详情统一）。
5. alarm 回迁新引擎，删除重复代码。
