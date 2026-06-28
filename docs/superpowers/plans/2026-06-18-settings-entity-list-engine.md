# Settings 列表页配置驱动引擎 + 六页推广 实施计划

> **For agentic workers:** 用 superpowers:subagent-driven-development 或 executing-plans 逐任务执行。步骤用 `- [ ]` 勾选跟踪。
> 本文件同时充当 `settings-design.md` §2.3「推广批次」的进度跟踪文档。

**Goal:** 由 alarm 三件套泛化出一套配置驱动的列表页引擎（分页 + 树表两种形态），并把 Settings 的
Label/Group/Api/User/Role/Resource/Menu 七个列表页迁移到该引擎。

**Architecture:** 引擎 = 类型 `EntityListConfig`（`src/config/types/entityList.ts`）+ 通用 composable（
`src/composables/useEntityListPage.ts`）+ 通用模板组件（`src/components/entity/EntityListPage.vue`）。每个列表页退化成「构造
config + 渲染 `<entity-list-page>`」的薄壳；详情页（`*Detail.vue`）与分配组件（`UserAssignRoles.vue`/
`RoleAssignResources.vue`）保持原样，引擎仅负责跳转/打开。alarm 自身本轮不动（§2.3 第 4 批回迁单列）。

**Tech Stack:** Vue 3 `<script setup>` + TypeScript + Element Plus + vue-i18n + Vite。复用既有 `ToolCard` / `BlankCard` /
`EnableTag` / `EnableFlagSegmented` / `usePagedList` 之外自管状态（沿用 alarm 模式）/ `@/api/*` 既有 CRUD 函数 /
`timestampLabel` / `prettyJson` / `cleanSearchParams` / `resetSearchForm` / `successMessage`。

## Global Constraints

- 不新增 `@/api/*` wrapper（复用既有函数）；不触发 ai-guardrails 的 API 命名守卫。
- 每个文件保留现有 Apache License 头注释块。
- CRUD 动词遵循 CLAUDE.md：单条 `getXxx`、集合 `listXxx`，引擎不引入新动词。
- 路由 `name` 不变（`Label.vue`/`Group.vue` 等文件名保持，路由懒加载路径不动）。
- 每页验收门槛（替代单元 TDD 循环）：`pnpm check`（vue-tsc）+ `pnpm test:guard` 全绿；广改时附 `pnpm lint:check`。
- `pnpm test:guard` 仅校验测试卫生与 API 命名，不要求新增 composable/component 配套单测；但 `src/composables/**`、
  `src/components/**` 属 guardrails 映射的「建议测试层」——本计划在引擎落地后补一个 composable 单测（Task 2）以符合 AI Change
  Rules，view 薄壳沿用既有 E2E 覆盖。

---

## File Structure

**新增：**

- `src/config/types/entityList.ts` — `EntityListConfig` 及其子类型（field/column/searchField/action/relation）。
- `src/composables/useEntityListPage.ts` — 通用 composable（分页+树表、搜索、增删改、relations、formatCell）。
- `src/components/entity/EntityListPage.vue` — 通用模板（ToolCard 搜索区 + el-table + 编辑 dialog）。
- `tests/unit/use-entity-list-page.test.ts` — composable 行为单测。
- 各页配置：`label/labelConfig.ts`、`group/groupConfig.ts`、`api/apiConfig.ts`、`user/userConfig.ts`、`role/roleConfig.ts`、
  `resource/resourceConfig.ts`、`menu/menuConfig.ts`。

**改写（变薄壳）：** 各页主组件 `Label.vue`/`Group.vue`/`Api.vue`/`User.vue`/`Role.vue`/`Resource.vue`/`Menu.vue`。

**删除（被引擎取代）：** 各页 `index.ts`、`tool/*Tool.vue`、`edit/*EditForm.vue`、`edit/index.ts`（detail/ 与 assign/ 保留）。

**保留：** 全部 `detail/*Detail.vue`、`user/assign/UserAssignRoles.vue`、`role/assign/RoleAssignResources.vue`、路由文件、i18n、
`alarm/*`。

---

## Task 0：引擎类型 `EntityListConfig`

**Files:** Create `src/config/types/entityList.ts`

**Produces:** 后续所有 config 与 composable 依赖的类型。

- [ ] **Step 1: 写类型文件**（含 License 头）

```ts
import type { FormItemRule } from 'element-plus';
import type { PageQuery } from '@/config/types';

export type EntityMode = 'page' | 'tree';
export type EntityFieldKind =
  | 'input'
  | 'number'
  | 'select'
  | 'enableFlag'
  | 'textarea'
  | 'json'
  | 'color'
  | 'treeSelect';
export type EntityColumnKind = 'text' | 'tag' | 'code' | 'time' | 'enable' | 'color' | 'icon';
export type EntitySearchKind = 'input' | 'select' | 'enableFlag';

export interface EntityOption {
  label: string;
  value: string | number;
}

export interface EntityTreeSource {
  load: () => Promise<unknown[]>;
  props?: { label?: string; value?: string; children?: string };
  checkStrictly?: boolean;
}

export interface EntityFieldConfig {
  prop: string;
  label: string;
  kind?: EntityFieldKind; // 默认 'input'
  options?: EntityOption[]; // select
  tree?: EntityTreeSource; // treeSelect
  placeholder?: string;
  required?: boolean;
  rules?: FormItemRule[]; // 追加校验（与 required/json 合并）
  span?: number; // 栅格，默认 12
  rows?: number; // textarea/json
  precision?: number; // number
  maxlength?: number;
  disabledOnEdit?: boolean; // 编辑态禁用（如 userName）
}

export interface EntityColumnContext {
  t: (key: string) => string;
  relations: Record<string, Record<string, string>>;
}

export interface EntityColumnConfig {
  prop: string; // 支持点路径，如 'menuExt.content.url'
  label: string;
  kind?: EntityColumnKind; // 默认 'text'
  width?: number | string;
  minWidth?: number | string;
  fixed?: boolean | 'left' | 'right';
  overflow?: boolean; // 默认 true
  options?: EntityOption[]; // tag/text 的值→标签映射来源
  formatter?: (row: Record<string, any>, ctx: EntityColumnContext) => string;
}

export interface EntitySearchFieldConfig {
  prop: string;
  label: string;
  kind: EntitySearchKind;
  options?: EntityOption[];
  placeholder?: string;
  multiple?: boolean; // select 多选
  includeAll?: boolean; // enableFlag segmented
}

export interface EntityRowAction {
  key: string;
  label: string;
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
  onClick: (row: Record<string, any>) => void;
}

export interface EntityRelation {
  key: string; // ctx.relations[key]
  load: () => Promise<Record<string, string>>;
}

export interface EntityListConfig {
  name: string; // 调试/组件名
  mode?: EntityMode; // 默认 'page'
  editable: boolean;
  rowKey?: string; // tree 模式必填，默认 'id'
  defaultExpandAll?: boolean; // tree
  pageSize?: number; // page，默认 12
  defaultOrderColumn?: string; // page 排序列，默认 'create_time'

  searchFields: EntitySearchFieldConfig[];
  columns: EntityColumnConfig[];
  fields: EntityFieldConfig[];
  defaultForm: () => Record<string, unknown>;
  relations?: EntityRelation[];

  list: (query: PageQuery) => Promise<R>;
  add?: (payload: Record<string, unknown>) => Promise<R>;
  update?: (payload: Record<string, unknown>) => Promise<R>;
  remove?: (id: string) => Promise<R>;

  detail?: { routeName: string }; // 跳转详情；缺省则无 detail 按钮
  extraActions?: EntityRowAction[];
  rowEditable?: (row: Record<string, any>) => boolean; // 行级编辑可用（resource 分组节点禁）
  rowDeletable?: (row: Record<string, any>) => boolean;

  dialogWidth?: string; // 默认 '720px'
  confirmDeleteText?: string;
  emptyText?: string;
}
```

- [ ] **Step 2: 验收** `pnpm check`（仅类型，预期 0 错）。

---

## Task 1：通用 composable `useEntityListPage`

**Files:** Create `src/composables/useEntityListPage.ts`
**Consumes:** Task 0 类型。
**Produces:** `useEntityListPage(config)` 返回
`{ t, config, state, searchForm, formVisible, editing, setFormRef, formModel, formRules, dialogTitle, relations, load, search, reset, sort, sizeChange, currentChange, openAdd, openEdit, openDetail, resetForm, submit, remove, formatCell, tagType, canEdit, canDelete }`。

- [ ] **Step 1: 写 composable**（泛化自 `alarm/useAlarmEntityPage.ts`，关键差异如下，含 License 头）

要点（实现细节）：

- 入参为单个 `EntityListConfig`（不再是 tab 数组）；`const config = ref(rawConfig)`，用 `config.value` 访问。
- `state.page`：`page` 模式带 total/size/current/orders；tree 模式 `mode==='tree'` 时 `load()` 调 `config.list({})`，把
  `res.data`（数组）直接塞 `state.rows`，不设 total。
- `query()`：遍历 `config.searchFields`，对每个非空 `searchForm[prop]` 写入 `result[prop]`（多选 select 写数组）；分页模式带
  `page`。
- `relations`：`load()` 时并发执行 `config.relations?.map(r => r.load())`，结果存入
  `reactive<Record<string,Record<string,string>>>`。
- `formRules`：required（select/treeSelect→change，余→blur）+ json 校验（同 alarm）+ 合并 `field.rules`。
- `assignForm` / `payload`：json 字段 `prettyJson`↔`JSON.parse`；number 字段转 `Number`；其余原样。编辑态保留 `id`/
  `version`。
- `openEdit`：以 `defaultForm()` 为底，按 `fields` 覆盖 `row` 值，写入 `id`/`version`。
- `openDetail`：`config.detail` 存在则 `router.push({ name: config.detail.routeName, query: { id: String(row.id) } })`。
- `formatCell(row, column)`：
    - 取值支持点路径（`column.prop.split('.').reduce(...)`）。
    - `column.formatter` 优先（传 `{ t, relations }`）。
    - `kind==='time'`→`timestampLabel`；`kind==='tag'`→`optionLabel(column.options, value)`；空→`'-'`；余→`String`。
- `tagType(value)`：`ENABLE/SUCCESS/NORMAL/AUTO→success`、`DISABLE/FAILED/FIRING→danger`、
  `PENDING/RETRYING/RECOVERED→warning`、余 `info`（enable 列实际用 `<enable-tag>`，此函数供普通 tag 列）。
- `canEdit(row)=config.rowEditable?config.rowEditable(row):true`；`canDelete` 同理。
- 不再 `watch(props.entity)`（单页固定 config）；`load()` 末尾调用一次。

完整代码以 `alarm/useAlarmEntityPage.ts` 为骨架，按上述差异改写。`enumLabel` 改为基于 `column.options`/`field.options` 的通用
`optionLabel(options, value)`：

```ts
const optionLabel = (options: EntityOption[] | undefined, value: unknown) => {
  const text = String(value ?? '');
  const hit = options?.find((o) => String(o.value) === text);
  return hit ? hit.label : text || '-';
};
```

- [ ] **Step 2: 验收** `pnpm check`。

---

## Task 2：通用模板 `EntityListPage.vue` + composable 单测

**Files:** Create `src/components/entity/EntityListPage.vue`，Create `tests/unit/use-entity-list-page.test.ts`
**Consumes:** Task 0/1。

- [ ] **Step 1: 写模板**（泛化自 `alarm/AlarmNotify.vue`，含 License 头）

差异要点：

- `defineProps<{ config: EntityListConfig }>()`，调用 `useEntityListPage(props.config)`。
- 搜索区 `#filters` 改为 `v-for="field in config.searchFields"`：
    - `kind==='enableFlag'`→`<enable-flag-segmented :include-all="field.includeAll">`；
    - `kind==='select'`→`<el-select :multiple="field.multiple" collapse-tags>`；
    - 余→`<el-input>`。
- 表格：`v-if="config.mode==='tree'"` 的
  `<el-table :row-key="config.rowKey||'id'" :default-expand-all="config.defaultExpandAll" :data="state.rows">`；否则分页
  `<el-table :data="state.rows">`。两者列循环相同。
- 列渲染按 `column.kind`：`enable`→`<enable-tag :value>`；`color`→色块+文本（样式见下）；`icon`→
  `<el-icon><component :is="resolveIcon(value)"/></el-icon>` + 名称（`resolveIcon` 从 `@/config/icons` 或既有 Menu 的
  icon 映射复用，import 同 MenuTool）；`tag`→`<el-tag :type="tagType(...)">`；`code`→`<code>`；余→`<span>`。
- 操作列：`detail`（`config.detail` 时）→ 内置 `extraActions`（`v-for`，`@click="action.onClick(row)"`）→ `edit`（
  `config.editable && canEdit(row)`）→ `delete`（`config.editable && canDelete(row)`，`el-popconfirm`）。列宽按动作数自动取档：
  `computed columnWidth`（1→100 / 2-3→180 / 4→260 / ≥5→320）。
- 分页/排序：`mode==='page'` 时 ToolCard 正常；`mode==='tree'` 时给 ToolCard 传 `:page="{total:0,size:0,current:1}"` 并
  `hide-sort`，并通过 CSS/prop 让其不显示分页（或新增 `tree` 模式下 ToolCard 仅渲染搜索+刷新——简单起见 tree 模式仍用
  ToolCard 但隐藏分页区，必要时给 ToolCard 加 `hidePagination` prop，本任务一并加）。
- 编辑 dialog：字段循环按 `field.kind`，新增 `color`→`<el-color-picker show-alpha>`；`treeSelect`→
  `<el-tree-select :data="treeData[field.prop]" :props="..." check-strictly>`，`treeData` 在 dialog 打开时按
  `field.tree.load()` 填充。`maxlength`/`show-word-limit`/`disabled`（编辑态 `field.disabledOnEdit`）透传。
- 颜色色块样式 `.entity-list__swatch` 迁自原 Label/LabelDetail 的 `.label-color__swatch`。

- [ ] **Step 2: 写 composable 单测** `tests/unit/use-entity-list-page.test.ts`：mock 一个最小 config（含 1 个 list 返回固定
  records、1 个 search 字段、2 列），断言 `query()` 注入搜索参数、`formatCell` 对 time/tag/空值的输出、`payload()` 对 json
  字段的解析。遵守 guardrails：kebab 文件名、小写动词 `it()` 描述、无 `toBeTruthy`、fixture 放 `tests/fixtures/`。

- [ ] **Step 3: 验收** `pnpm check` + `pnpm test:unit tests/unit/use-entity-list-page.test.ts` + `pnpm test:guard`。

---

## Task 3：Label 范本

**Files:** Create `label/labelConfig.ts`；Rewrite `label/Label.vue`；Delete `label/index.ts`、`label/tool/LabelTool.vue`、
`label/edit/LabelEditForm.vue`、`label/edit/index.ts`。保留 `label/detail/LabelDetail.vue`。
**Consumes:** Task 0/1/2。

- [ ] **Step 1: 写 `labelConfig.ts`**

```ts
// License 头
import { addLabel, deleteLabel, listLabel, updateLabel } from '@/api/label';
import { ENTITY_TYPE_OPTIONS } from '@/config/constant/enums';
import type { EntityListConfig } from '@/config/types/entityList';
import { nameRules, remarkRules } from '@/utils/formRuleUtil';

type T = (key: string) => string;

export const createLabelConfig = (t: T): EntityListConfig => ({
  name: 'label',
  editable: true,
  searchFields: [
    {
      prop: 'labelName',
      label: t('settings.label.labelName'),
      kind: 'input',
      placeholder: t('settings.label.labelNamePlaceholder'),
    },
    { prop: 'entityTypeFlag', label: t('settings.common.entityType'), kind: 'select', options: ENTITY_TYPE_OPTIONS },
    { prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag', includeAll: true },
  ],
  columns: [
    { prop: 'labelName', label: t('settings.label.labelName'), minWidth: 160 },
    { prop: 'labelCode', label: t('settings.label.labelCode'), kind: 'code', minWidth: 150 },
    { prop: 'entityTypeFlag', label: t('settings.common.entityType'), width: 110 },
    { prop: 'labelColor', label: t('settings.label.labelColor'), kind: 'color', width: 130 },
    { prop: 'enableFlag', label: t('common.enable'), kind: 'enable', width: 90 },
    { prop: 'remark', label: t('common.remark'), minWidth: 180 },
    { prop: 'createTime', label: t('common.createTime'), kind: 'time', width: 165 },
  ],
  fields: [
    {
      prop: 'entityTypeFlag',
      label: t('settings.common.entityType'),
      kind: 'select',
      options: ENTITY_TYPE_OPTIONS,
      required: true,
    },
    { prop: 'labelColor', label: t('settings.label.labelColor'), kind: 'color' },
    {
      prop: 'labelName',
      label: t('settings.label.labelName'),
      placeholder: t('settings.label.labelNamePlaceholder'),
      maxlength: 32,
      rules: nameRules(t, t('common.entityLabel')),
    },
    {
      prop: 'labelCode',
      label: t('settings.label.labelCode'),
      placeholder: t('settings.label.labelCodePlaceholder'),
      maxlength: 32,
    },
    { prop: 'enableFlag', label: t('common.enableFlag'), kind: 'enableFlag' },
    { prop: 'remark', label: t('common.remark'), kind: 'textarea', span: 24, maxlength: 300, rules: remarkRules(t) },
  ],
  defaultForm: () => ({
    entityTypeFlag: 'DEVICE',
    labelColor: '#F4F4F5',
    labelName: '',
    labelCode: '',
    enableFlag: 'ENABLE',
    remark: '',
  }),
  list: listLabel,
  add: addLabel,
  update: updateLabel,
  remove: deleteLabel,
  detail: { routeName: 'settingsLabelDetail' },
  confirmDeleteText: t('settings.label.confirmDelete'),
  emptyText: t('settings.label.empty'),
});
```

- [ ] **Step 2: 改写 `label/Label.vue`** 为薄壳：

```vue
<!-- License 头 -->
<template>
  <entity-list-page :config="config" />
</template>
<script lang="ts" setup>
  import { useI18n } from 'vue-i18n';
  import EntityListPage from '@/components/entity/EntityListPage.vue';
  import { createLabelConfig } from './labelConfig';
  const { t } = useI18n();
  const config = createLabelConfig(t);
</script>
```

- [ ] **Step 3: 删除** `label/index.ts`、`label/tool/`、`label/edit/`。
- [ ] **Step 4: 验收** `pnpm check` + `pnpm test:guard`。手测：`/settings/label` 列表/搜索/新增/编辑/删除/详情跳转正常。
- [ ] **Step 5: Commit** `refactor(settings): extract config-driven entity list engine, migrate label`

---

## Task 4：Group

**Files:** Create `group/groupConfig.ts`；Rewrite `group/Group.vue`；Delete `group/index.ts`、`group/tool/`、`group/edit/`
。保留 `group/detail/GroupDetail.vue`。

差异（相对 Label）：

- 父组名列用 relation：
  `relations: [{ key: 'parentName', load: async () => { const res = await listGroup({ page:{current:1,size:5000,orders:[{column:'group_index',asc:true}]} }); const map:Record<string,string>={}; (res.data?.records||[]).forEach(g=>{map[String(g.id)]=g.groupName;}); return map; } }]`。
- 列 `parentGroupId`：
  `{ prop:'parentGroupId', label:t('settings.group.parentGroupId'), minWidth:150, formatter:(row,ctx)=>ctx.relations.parentName?.[String(row.parentGroupId)] || '-' }`。
- 编辑字段 `parentGroupId`：`kind:'treeSelect'`，
  `tree:{ load: 同上 listGroup 返回 records 作扁平/树, props:{label:'groupName',value:'id',children:'children'}, checkStrictly:true }`
  （Group 为扁平，可直接用 records 列表作为单层树）。
- 其余列/字段按 Explore 测绘：列 groupName/groupCode/groupTypeFlag(text)/parentGroupId/enableFlag(enable)
  /remark/createTime(time)；字段 groupTypeFlag(select ENTITY_TYPE_OPTIONS required)/parentGroupId(treeSelect)/groupName(
  maxlength32 required)/groupCode/groupIndex(number min0)/enableFlag/remark(textarea)；搜索 groupName(input)
  /groupTypeFlag(select)/enableFlag(segmented includeAll)。
- CRUD：`listGroup/addGroup/updateGroup/deleteGroup`；detail `settingsGroupDetail`。

- [ ] Step 1 写 `groupConfig.ts` · Step 2 改写 `Group.vue` · Step 3 删旧文件 · Step 4 验收 `pnpm check`+
  `pnpm test:guard`+手测 · Step 5 Commit `refactor(settings): migrate group to entity list engine`

---

## Task 5：Api（只读 editable:false）

**Files:** Create `api/apiConfig.ts`；Rewrite `api/Api.vue`；Delete `api/index.ts`、`api/tool/`。（api 无 edit/ 目录。）保留
`api/detail/ApiDetail.vue`。

要点：`editable:false`，无 `add/update/remove`，`fields:[]`，`defaultForm:()=>({})`。

- 列：apiName(160)/apiCode(code,200)/apiGroup(160)/serviceName(160)/apiTypeFlag(tag,100)/enableFlag(enable,90)/remark(
  140)/createTime(time,180)/operateTime(time,180)。
- 搜索：apiName/apiCode/apiGroup/serviceName(均 input)/apiTypeFlag(select 选项 GET/POST/PUT/DELETE)/enableFlag(
  segmented)。
- `list: listApi`；detail `settingsApiDetail`。
- 引擎需正确隐藏 Add 按钮与 edit/delete 操作（`editable:false`），操作列只剩 detail（宽 100）。

- [ ] Step 1 写 config · Step 2 改写 `Api.vue` · Step 3 删旧 · Step 4 验收（重点确认只读：无新增/编辑/删除按钮）· Step 5
  Commit `refactor(settings): migrate read-only api page to entity list engine`

---

## Task 6：User（assignRoles + tabs 详情）

**Files:** Create `user/userConfig.ts`；Rewrite `user/User.vue`；Delete `user/index.ts`、`user/tool/`、`user/edit/`。保留
`user/detail/UserDetail.vue`、`user/assign/UserAssignRoles.vue`。

要点：

- `User.vue` 薄壳需引入 `UserAssignRoles`，持 `assignRef`，并把 `onAssignRoles` 注入 config 工厂：

```vue
<template>
  <div>
    <entity-list-page :config="config" />
    <user-assign-roles ref="assignRef" @saved="reloadKey++" />
  </div>
</template>
```

config 工厂签名 `createUserConfig(t, { onAssignRoles })`，其中
`extraActions:[{ key:'assignRoles', label:t('settings.user.assignRoles'), type:'warning', onClick:onAssignRoles }]`
。（reload：assign 保存后刷新列表——可由 EntityListPage 暴露 `reload` 或 assign 内部 successMessage 后由用户手动刷新；本任务让
`UserAssignRoles` 关闭后调用 config 持有的 `load`，实现方式：config 工厂同时接收 `onAfterAssign`
留空，简单做法保留现有「分配后提示」行为，不强制自动刷新。）

- 列：nickName(120)/userName(140)/phone(140)/email(180)/enableFlag(enable,90)/createTime(time,165)。操作列
  detail/edit/assignRoles/delete → 宽 ≥4 档（310）。
- 搜索：nickName/userName/phone/email(input)/enableFlag(segmented)。
- 字段：userName(required min2 max32 AUTH_NAME_PATTERN, disabledOnEdit:true)/nickName(required min2 max32 NAME_PATTERN)
  /phone(PHONE_PATTERN)/email(EMAIL_PATTERN)/enableFlag。规则用既有 `@/utils/formRuleUtil` 助手 + 模式常量构造
  `field.rules`。
- CRUD `listUser/addUser/updateUser/deleteUser`；detail `settingsUserDetail`。

- [ ] Step 1 写 config · Step 2 改写 `User.vue`（含 assign 接线）· Step 3 删旧 · Step 4 验收 + 手测（含分配角色弹窗打开）·
  Step 5 Commit `refactor(settings): migrate user page to entity list engine`

---

## Task 7：Role（assignResources + parent treeSelect + tabs 详情）

**Files:** Create `role/roleConfig.ts`；Rewrite `role/Role.vue`；Delete `role/index.ts`、`role/tool/`、`role/edit/`。保留
`role/detail/RoleDetail.vue`、`role/assign/RoleAssignResources.vue`。

要点：同 User 的 extraAction 接线（`assignResources`，type `primary`，打开 `RoleAssignResources`）。

- 列：roleName(160)/roleCode(160)/enableFlag(enable,90)/remark(200)/createTime(time,165)。操作列
  detail/edit/assignResources/delete（宽 320）。
- 搜索：roleName/roleCode(input)/enableFlag(segmented)。
- 字段：parentRoleId(treeSelect，`tree.load=listRoleTree`，带虚拟根 required)/roleName(authNameRules max32)
  /roleCode/enableFlag/remark(textarea)。
- CRUD `listRole/addRole/updateRole/deleteRole`；detail `settingsRoleDetail`。

- [ ] Step 1 写 config · Step 2 改写 `Role.vue` · Step 3 删旧 · Step 4 验收 + 手测 · Step 5 Commit
  `refactor(settings): migrate role page to entity list engine`

---

## Task 8：引擎树模式收尾校验（在 Task 9/10 前）

引擎树能力在 Task 1/2 已含 `mode:'tree'` 分支与 `icon`/点路径列。本任务确保以下树专属能力可用，缺则补：

- [ ] tree 模式 `load()` 不分页、`el-table` 行展开正确（`row-key`、`children`）。
- [ ] 点路径列取值（`menuExt.content.url`）。
- [ ] `icon` 列渲染（复用 Menu 既有 `resolveIcon`/iconMap，迁入 EntityListPage 或共享模块）。
- [ ] `rowEditable`/`rowDeletable` 行级禁用（resource 分组节点 `entityId===0`）。
- [ ] ToolCard 在 tree 模式隐藏分页（`hidePagination` prop 已于 Task 2 加）。
- [ ] 验收 `pnpm check`。

---

## Task 9：Resource（树表）

**Files:** Create `resource/resourceConfig.ts`；Rewrite `resource/Resource.vue`；Delete `resource/index.ts`、
`resource/tool/`、`resource/edit/`。保留 `resource/detail/ResourceDetail.vue`。

要点：

- `mode:'tree'`，`list: listResourceTree`，`rowKey:'id'`，`defaultExpandAll:true`（沿用现状）。
- 实体名解析：
  `relations:[{ key:'entityNames', load: async()=>{ /* 复用现 index.ts 的 listApi/listDriverByIds/listDeviceByIds/listPointByIds/listProfileByIds 聚合逻辑，返回 entityId→name 映射 */ } }]`
  。该聚合逻辑从现 `resource/index.ts` 整体迁入此 loader。
- 列：resourceName(220)/resourceCode(code,180)/serviceName(160)/resourceTypeFlag(text/tag,120)/resourceScopeFlag(120,100)
  /entityId(formatter 用 entityNames + 分组节点显示规则,140)/remark(140)/enableFlag(enable,90)/createTime(time,165)。
- 搜索：resourceName/resourceCode(input)/resourceTypeFlags(select multiple RESOURCE_TYPE_OPTIONS collapse-tags)
  /resourceScopeFlags(select multiple RESOURCE_SCOPE_OPTIONS)/enableFlag(segmented)。
- 字段：parentResourceId(treeSelect tree.load=listResourceTree required)/resourceName(authNameRules required)
  /resourceCode/resourceTypeFlag(select 硬编码 DRIVER/PROFILE/POINT/DEVICE/DATA/MENU/API required)/entityId(input
  positiveIntegerRules)/enableFlag/remark(textarea)。
- `rowEditable:(row)=>row.entityId!==0 && !isGroupingNode(row)`、`rowDeletable` 同（分组节点禁编辑，沿用现状）。
- CRUD `addResource/updateResource/deleteResource`；detail `settingsResourceDetail`。

- [ ] Step 1 写 config（迁入实体名聚合）· Step 2 改写 `Resource.vue` · Step 3 删旧 · Step 4 验收 + 手测（树展开/分组节点禁编辑/实体名显示）·
  Step 5 Commit `refactor(settings): migrate resource tree page to entity list engine`

---

## Task 10：Menu（树表 + 嵌套 ext + 图标）

**Files:** Create `menu/menuConfig.ts`；Rewrite `menu/Menu.vue`；Delete `menu/index.ts`、`menu/tool/`、`menu/edit/`。保留
`menu/detail/MenuDetail.vue`。

要点：

- `mode:'tree'`，`list: listMenuTree`，`rowKey:'id'`。
- 列：menuName(220)/menuCode(code,180)/menuTypeFlag(100)/menuLevel(90)/menuIndex(80)/`menuExt.content.url`(点路径,160)/
  `menuExt.content.icon`(kind:'icon',90)/enableFlag(enable,90)/createTime(time,165)。
- 搜索：menuName/menuCode(input)/menuTypeFlag(select MENU_TYPE_OPTIONS)/enableFlag(segmented)。
- 字段：parentMenuId(treeSelect tree.load=listMenuTree required)/menuName(authNameRules max32 required)/menuCode(required
  max64)/titleZh(required max64)/titleEn(required max64)/menuTypeFlag(select required)/menuLevel(select
  MENU_LEVEL_OPTIONS required)/menuIndex(number)/icon(select iconNames)/url(input)/enableFlag/remark(textarea)。
    - 注意 add/update payload 需把 titleZh/titleEn/icon/url 组装回 `menuExt.content`（沿用现 edit/index.ts 的组装逻辑）——在
      config 的 `add`/`update` 包装函数里做映射，或在 `defaultForm`/`payload` 钩子处理。本任务在 config 提供
      `add:(p)=>addMenu(toMenuPayload(p))`、`update:(p)=>updateMenu(toMenuPayload(p))`，`toMenuPayload` 迁自现
      edit/index.ts。
- CRUD `addMenu/updateMenu/deleteMenu`；detail `settingsMenuDetail`。

- [ ] Step 1 写 config（迁入 ext 组装 + icon）· Step 2 改写 `Menu.vue` · Step 3 删旧 · Step 4 验收 + 手测（图标列/URL
  列/树展开/编辑回填）· Step 5 Commit `refactor(settings): migrate menu tree page to entity list engine`

---

## Task 11：全量回归

- [ ] `pnpm lint:check` + `pnpm check` + `pnpm test:guard` + `pnpm test`（全套 vitest）。
- [ ] `pnpm build`。
- [ ] 七页逐一 E2E 冒烟（路由可达 + 列表渲染）：`E2E_BASE_URL=... pnpm test:e2e`（如有可丢弃后端）。
- [ ] 更新 `docs/settings-design.md` §2.3 勾选「批 1、批 2、批 3」完成；标注 alarm 回迁（批 4）仍待办。

---

## Self-Review 备忘

- **类型一致**：config 工厂返回 `EntityListConfig`，composable/模板消费同一类型；`relations` 的 key 在 column.formatter 中按
  `ctx.relations[key]` 引用，需与声明一致（group `parentName`、resource `entityNames`）。
- **风险**：① 树模式与 ToolCard 分页耦合——Task 2 加 `hidePagination`；② treeSelect 选项异步——dialog 打开时加载；③
  Menu/Resource 的 ext/实体名映射逻辑必须从旧 index.ts 完整迁移，不可简化丢字段；④ User/Role
  分配组件保存后列表刷新策略本计划保守（不强制自动刷新），如需自动刷新再让 EntityListPage 暴露 `reload`。
- **未纳入**：alarm 自身回迁（§2.3 批 4）——本计划完成后另起。
