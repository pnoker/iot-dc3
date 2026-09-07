# dc3-web 遗留缺陷修复交接（审查 LOW 清单）

| 项目 | 约束 |
| --- | --- |
| 状态 | 待执行；本文为 2026-09 对 dc3-web 一次全量代码审查后的遗留缺陷交接 |
| 执行对象 | Codex 或后续承担修复的工程 Agent |
| 目标范围 | 仅 `dc3-web`（个别项涉及 `dc3/dependencies/nginx`）；**不得改动本清单之外的任何未提交改动** |
| 基线状态 | 工作区现有约 192 个未提交文件（一轮大型前端改动 + 已完成的 CRITICAL/HIGH/MEDIUM 修复），本清单只处理遗留 LOW 项 |
| 门禁 | 每项修复后必须通过 §5 的全量门禁；任意一项红灯不得声称完成 |
| 边界 | 标注「需人工确认」的项**禁止擅自修改**——它们疑似有意设计，改动前必须由人裁决 |
| 证据原则 | 每项修复需附可复现缺陷的测试（或明确说明为何不适用），禁止只改代码不留证据 |

## 1. 结论先行

上一轮审查确认并已修复全部 CRITICAL/HIGH 与绝大多数 MEDIUM 缺陷。本清单是余下的 LOW 级问题：
单条都不阻塞提交，但其中 P1-1、P1-2 是根因明确、改动极小的真实缺陷，建议优先处理；其余为
mock 保真度、潜伏雷或需产品决策项，按序处理即可。

所有行号以当前工作区状态为准，若已有偏移，以函数名/锚点定位。

## 2. P1 —— 建议立即修复（根因明确、改动极小）

### P1-1 agentic：切会话后立即发消息，乐观消息被在途请求覆盖

- **位置**：`dc3-web/src/store/modules/agentic.ts`，`sendMessage`（约 440-470 行）与 `loadMessages`（约 545-575 行）、`mergeEphemeralAssistantState`（约 1069 行）。
- **根因**：`loadMessages` 的响应写回以服务端返回的 `loaded` 列表为基准整体重建 `messagesByConversation[conversationId]`；`mergeEphemeralAssistantState` 只按顺序位置映射 assistant 的 `reasoning/finishReason/charts`，**不保留 previous 中多出来的本地消息**。`sendMessage` 乐观插入的 user 消息与 streaming 占位因此会被仍在途的 `loadMessages` 响应抹掉。
- **触发时序**：点击会话 B → `loadMessages(B)` 在途（慢网络数百 ms）→ 此刻用户点发送（输入框仅被 `sessionsLoading/streaming` 禁用，不含 `currentMessagesLoading`）→ 乐观列表出现 → 在途响应返回，token 校验通过（它就是该会话最新请求）→ 列表被重建，乐观消息消失；后续 `appendAssistantDelta` 按 id 找不到占位而静默 no-op，AI 回复直到流结束后的第二次 `loadMessages` 才显示。
- **失败后果**：用户消息从 UI 闪没、AI 回复不可见；数据服务端不丢，最终一致。
- **建议修法**（二选一，改动均约一行）：
  1. `loadMessages` 写回前加守卫：`if (streamingConversationId.value === conversationId) return;`（注意放在 lifecycle/token 双检之后、写回之前）；
  2. 或在 `mergeEphemeralAssistantState` 中保留 loaded 集合之外的本地尾部消息（user/ephemeral）。
- **验收**：新增 store 级测试模拟该时序（在途 promise 手动控制 resolve 顺序），断言乐观 user 消息在响应写回后仍存在、流式增量仍可见。`tests/unit/agentic-store.test.ts` 已有基建可扩展。

### P1-2 mock 菜单环：父级指到自己的后代后，导航整棵消失

- **位置**：`dc3-web/src/mock/handlers/menu.ts`，`update` handler（约 168-176 行）、`buildTree`（约 53-78 行）。
- **根因**：两处叠加——① `update` 只校验 `findById(parentId)` 存在，不校验「新父级不能是自身或自身的后代」；② `buildTree` 从根 `'0'` 按 `parentMenuId` 递归 `nest`，无环检测。把节点 A 的父级改成 A 的子节点 B（编辑表单 treeSelect 允许选到任意节点）后，A、B 的父链成环、不再从 `'0'` 可达，`buildTree` 返回的树整块缺失。
- **失败后果**：左侧导航对应子树消失；router guard 的 `isRouteInMenuTree` 拒绝这些路由，demo 中相关页面不可达且不刷新不恢复（mock 数据在内存）。仅影响 mock demo，真实后端不受影响。
- **建议修法**：`update` 写入前加环校验（同文件 `delete` 已在用现成的 `descendantsOf`）：

  ```ts
  if (parentId !== '0' && descendantsOf(String(ctx.body?.id)).has(parentId)) {
    return responseOf(ctx.config, fail('R4042', 'Parent menu cannot be a descendant of the menu', 400), 400);
  }
  ```

  可选加固：`buildTree` 只把父链能到达 `'0'` 的行作为根（防其它来源的脏数据），非必须。
- **验收**：mock handler 测试覆盖「把父级改成自己的子节点 → update 返回 400 且树不变形」；现有菜单相关测试（`tests/unit/menu-store.test.ts`）不回归。

## 3. P2 —— 同类隐患与 mock 保真度（按序处理）

### P2-1 usePagedList：`setAllData` 缩数据后停在越界空页

- **位置**：`dc3-web/src/composables/usePagedList.ts`，`applyFilters`（约 84-89 行）。
- **根因**：`search/reset/sizeChange` 都会 `page.current = 1`，但外部直接 `setAllData(rows)` 整体替换数据源的路径不重置页码；`applyFilters` 的 `slice((current-1)*size, ...)` 无 clamp。用户停在第 3 页时数据缩到 ≤2 页 → 列表空白。
- **背景**：与已修复的 DeviceEdit 矩阵分页越界（`views/device/edit/index.ts` 的 `paginateRows`）是同类问题；当前调用方 `AgenticSettings.vue`/`ProviderSettings.vue` 未必凑齐触发时序，属隐患。
- **建议修法**：`applyFilters` 内对 `page.current` 归一化：`state.page.current = Math.min(state.page.current, Math.max(1, Math.ceil(filtered.length / state.page.size)))`（缩页时仅在越界时写回，避免无谓的响应式触发）。
- **验收**：`tests/unit/use-paged-list.test.ts` 增加用例：翻到高页码 → `setAllData` 传入更短数组 → `listData` 非空且 `page.current` 落在有效范围。

### P2-2 mock/fetch：body 与重复 query 的归一化与 axios adapter 不一致

- **位置**：`dc3-web/src/mock/fetch.ts`，`readRequestBody`（约 380-387 行）与 `Object.fromEntries(url.searchParams)`（约 404 行）。
- **根因**：① 只处理 `string` body——`fetch(new Request(url, {method:'POST', body}))` 场景 `init.body` 为 undefined，handler 收到 `{}`；FormData/URLSearchParams 原样透传。② `Object.fromEntries` 把重复 query key 折叠成最后一个、所有值变字符串，而 axios adapter 传入原始对象/数组。
- **失败后果**：e2e fixture 用 `Request` 对象或 `?id=1&id=2` 形式发请求时，fetch mock 与 axios 路径返回不同结果。
- **建议修法**：`input instanceof Request` 时经 `input.clone().text()` 读取 body；重复 key 收集为数组（与 adapter 行为对齐）。先读 `src/mock/adapter.ts` 确认 axios 侧的确切归一化形态再对齐。
- **验收**：`tests/unit/mock-adapter.test.ts` 扩展 Request-body 与重复 key 两类用例。

### P2-3 mock business：四种 attribute 端点共用同一集合

- **位置**：`dc3-web/src/mock/handlers/business.ts`（约 124-127 行）。
- **问题**：`driver_attribute` / `point_attribute` / `command_attribute` / `event_attribute` 各自注册了 `/list`、`/add` 等，但都落在同一个 `db.attributes` 数组上——通过 `driver_attribute/add` 创建的记录会同时出现在其余三个列表里。
- **建议修法**：拆成 4 个集合；或在代码注释中明确「共享属性目录」是有意简化。二选一，不修注释也行，但需定案。
- **验收**：4 个端点的 list/add 互不串数据（mock 层测试或手动 curl mock server 验证）。

### P2-4 AlarmNotify：`flush:'sync'` watcher 依赖字段声明顺序（潜伏雷）

- **位置**：`dc3-web/src/views/settings/alarm/AlarmNotify.vue`（约 305-312 行）、`useAlarmEntityPage.ts` 的 `assignForm`（约 202-204 行）、`alarmEntityConfig.ts` 的 `defaultForm()`（约 316-326 行）。
- **根因**：watcher 监听 `alarmTargetTypeFlag` 并在同一同步帧内执行 `formModel.entityId = ''`；而 `assignForm` 是「先删光 key 再 `Object.assign(formModel, value)`」，回显时 watcher 会中途触发。当前不出问题**仅因** `defaultForm()` 中 `alarmTargetTypeFlag`（第 3 个 key）排在 `entityId`（第 4 个）之前——Object.assign 的后续赋值把被清掉的 `entityId` 写了回来。任何人日后调整字段顺序，编辑回显就会被静默清空，保存后丢 `entityId` 且无报错。
- **附带**：该 watcher 在 `formVisible` 为 false 时（`openEdit` 阶段）会发起一次注定被丢弃的 `loadRemote`，浪费一个请求。
- **建议修法**：watcher 改 `flush: 'post'`（模板交互场景足够），或在 watcher 内加「仅用户交互生效」标志位；顺带用 `formVisible` 守卫掉不可见期的 `loadRemote`。
- **验收**：`tests/views/alarm-notify.test.ts` 增加用例：`defaultForm()` 字段顺序对调后编辑回显 `entityId` 不丢失（用例本身即可揭示雷）。

### P2-5 EntityListPage：删除态硬编码 `row.id`，与 `config.rowKey` 不一致

- **位置**：`dc3-web/src/components/entity/EntityListPage.vue`（约 142、569 行）。
- **问题**：列表 key 与表格 `row-key` 用 `config.rowKey || 'id'`，而删除态 `:removing="isRemoving(String(entry.row.id ?? ''))"` 与 `removeRow` 的空判直接用 `row.id`。当前所有实体 rowKey 都是 `'id'` 不触发；一旦出现非 `id` rowKey 的实体，删除 loading 失效且删除静默无效。
- **建议修法**：统一经 `getCellValue(row, config.rowKey || 'id')` 取键值。

### P2-6 DashboardCard / ThingsCardHeader：props 默认值写死英文

- **位置**：`dc3-web/src/components/card/dashboard/DashboardCard.vue`（`errorText: 'Unable to load data.'`、`retryText: 'Retry'`）、`ThingsCardHeader.vue`（`:aria-label="... : 'Refresh'"`）及 `copyLabel` 类默认值。
- **问题**：项目 UI 文案全部走 `t()`；当前所有调用点都显式传值所以不可见，但新调用点漏传即出现英文 UI。
- **建议修法**：默认值改由组件内 `useI18n()` 提供，或将这几个 prop 设为 required。任选其一，保持同批组件一致即可。

### P2-7 e2e-server：`E2E_BASE_URL` 无端口时 `listen(0)` 陷阱

- **位置**：`dc3-web/scripts/testing/e2e-server.mjs`（约 39 行）。
- **问题**：`Number(env.E2E_PORT || baseUrl.port)`——`E2E_BASE_URL=http://host`（无端口）时 `Number('') === 0`，服务绑到随机端口，Playwright 轮询配置端口 120s 超时；旧版 `APP_CLI_PORT || 8080` 兜底被删，`APP_CLI_PORT` 现被完全忽略。
- **建议修法**：无端口时显式报错（fail fast），或恢复 8080 兜底。

## 4. 需人工确认 —— 禁止 Codex 擅自修改

以下各项疑似有意设计或涉及产品/部署决策，改动前必须由人拍板：

1. **DriverTool「新增」按钮永久禁用**（`views/driver/tool/DriverTool.vue:59` `:disabled="!add"`，无人传 `add`）。`ToolCard.vue` 有注释表明模板默认不启用 Add；若确认驱动创建需可达，在 `Driver.vue` 传 `add` 并接 `show-add`，否则建议改 `v-if` 隐藏置灰按钮。
2. **Home/Overview 的 `Promise.all` 一损俱损**（`views/home/Home.vue:267-272`、`views/settings/alarm/Overview.vue:300-307`）：并发请求任一失败整块置 error 态并沿用旧数据。这是为新增错误 banner 服务的取舍；若要恢复局部降级，需同时设计分区错误提示。
3. **Login mock 模式预填密码** `dc3dc3dc3`（`views/login/Login.vue:204`）：仅 `build:mock` 公开 demo 使用。需确认该口令永不指向真实网关、不与任何真实环境复用。
4. **重置密码走 URL query**（`src/api/localCredential.ts:31` + 后端 `LocalCredentialController` `@RequestParam("password")`，HEAD 已有）：密码明文进网关/代理 access log。需**前后端联动**改请求体传参，涉及 Java 契约变更与 mock/测试同步，建议单独立项，不要混入本批前端改动。
5. **存量 44px 与触控契约 v7 的冲突面**：`NavMenu.vue`（约 210 行，导航项 44px，文档却写 36px）等十余处、`tokens.scss:62` `$touch-target-min: 44px` 定义后从未引用（死 token）、`things-dialog.scss` `param-editor__row-header` 容器行高 44px、`ThingsCardActions`/`AgenticAssistant` 的 FAB 避让设计（44px + `--dc3-floating-action-safe-space`）、`design/frontend-three-terminal-ux.md` 修订记录 v7 排在 v6 之前（时序颠倒）。这些需按 ADR 统一裁决后一次性清理。

## 5. 全局验证门禁

每完成一项修复，以及最终收尾时，在 `dc3-web/` 下依次执行并要求全绿：

```bash
pnpm run check        # vue-tsc 类型检查
pnpm run lint:check   # eslint
pnpm run test         # vitest 全量（当前基线 679 用例）
pnpm run test:e2e:responsive   # Playwright mock e2e（当前基线 64 用例）
```

注意：

- 不要 commit；修复成果以工作区改动形式交付，由人审后统一提交。
- 不要「顺带」重构/格式化清单外代码；每行改动需可追溯到本文某一项。
- 新增测试优先跟随既有测试文件的风格与位置（§2 各项已指明宿主文件）。
- 若某项修复与「需人工确认」清单冲突（例如必须改动被禁改的文件才能完成），停下来在交付说明中提出，不要绕过。
