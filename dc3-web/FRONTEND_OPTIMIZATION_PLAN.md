# IoT DC3 前端优化方案（dc3-web）

> 基于 6 维度并行代码审计（设备/模板/位号流程 + 卡片组件体系 + 业务卡片视觉 + 表单交互一致性）+ 完整性审查 + 综合排序。
> 每条结论均经人工逐行复核，行号为审计时快照，实施时以实际代码为准。
> 技术栈：Vue 3 + TypeScript + Element Plus + Pinia + vue-i18n。约定见根 `AGENTS.md`「Frontend conventions」。

## 优先级矩阵

| 级别 | 编号 | 标题                                                            | 工作量 | 类型   |
|------|------|-----------------------------------------------------------------|--------|--------|
| P0   | P0-1 | 统一 done 回调契约：失败保留弹窗、消除双提示/误报成功           | S      | 正确性 |
| P0   | P0-2 | operationTime 误绑 createTime（4 处同源）                       | S      | 正确性 |
| P0   | P0-3 | DeviceEdit 切换 driver/profile 加未保存守卫 + i18n 离开确认     | S      | 正确性 |
| P0   | P0-4 | PointValue 分页 reset + DeviceImport 空文件永久 loading         | S      | 正确性 |
| P1   | P1-1 | 系统化「错误/加载/空」三态 + CardListShell                      | M      | 体验   |
| P1   | P1-2 | 令牌化收敛：裸 hex → CSS 变量 + 圆角令牌 + 共享 footer 类       | M      | 美化   |
| P1   | P1-3 | 抽 useRemoteDictionary + Device/Profile 共享字段组件            | M      | 重构   |
| P1   | P1-4 | StatCard 补加载态 + 业务卡实体色身份 + 修 DriverCard 死 .active | M      | 美化   |
| P1   | P1-5 | PointValueCard 数值按质量着色 + 稳定字号                        | M      | 美化   |
| P2   | —    | a11y / 暗色模式 / 死代码清理 / DeviceEdit 抽 ConfigMatrix       | S–L    | 打磨   |

---

## P0 正确性修复（建议立即处理，全部 S 工作量、低风险）

### P0-1　统一 done 回调契约：失败保留弹窗、消除双提示与误报成功

**问题**：`AddForm` 传入的 `done` 回调无条件执行 `cancel()+reset()+successMessage()`，父页用
`.finally(() => done())` 调用。后果：

- **创建失败时**：`.finally` 仍跑 `done()` → 弹窗被关、表单被清空、弹出「成功」通知（叠加全局错误提示）；
- **创建成功时**：父页 `.then` 与 `done` 各弹一次 → 两条成功 toast。

涉及：`DeviceAddForm.vue:227`、`ProfileAddForm.vue:111`、`DeviceImportForm.vue:258`、以及卡片
`DeviceCard` / `ProfileCard` / `PointCard` 的 toggle/delete 回调（同模式）。

**Before**（`views/device/add/DeviceAddForm.vue:227`）

```js
emit('add', {...reactiveData.formData}, () => {
  cancel();
  reset();
  successMessage();
});
```

**Before**（`views/device/Device.vue:151`，父页）

```js
const onAdd = (form, done) => {
  addDevice(form)
    .then(() => { successMessage(); load(); })
    .catch(() => {})
    .finally(() => { done(); });
};
```

**After**（表单组件：`done` 只在成功时关闭+清空，永不弹 toast）

```js
emit('add', {...reactiveData.formData}, (ok = true) => {
  if (!ok) return;      // 失败：保留弹窗、保留输入、不清空
  cancel();
  reset();
});
```

**After**（父页：去掉 `.finally`，成功 `.then` 调 `done(true)`，失败 `.catch` 调 `done(false)`）

```js
const onAdd = (form, done) => {
  addDevice(form)
    .then(() => { successMessage(); load(); done(true); })
    .catch(() => { done(false); });
};
```

**说明**：

- 同仓已有正确范本：`views/point/add/PointEditForm.vue:241`（`done(close = true)` 只关弹窗 + 复位
  `submitting`，提示与重置交给父页 `.then`）。本方案与其对齐。
- `ProfileAddForm.vue:111`（事件名 `add-thing`）同样修改；`Profile.vue:122` `addThing` 父页同样改。
- `DeviceImportForm.vue:258` 的 `importTemplate` done 回调同源，一并按此契约修。
- 顺带给 `DeviceAddForm` / `ProfileAddForm` 的确认按钮补 `:loading="submitting"`（引入 `submitting` ref， 提交前置 true、
  `try/finally` 复位），防重复提交——对齐 `PointEditForm.vue:108`。

**验证**：`pnpm test`（补充用例：mock reject 时弹窗不关、不弹成功）；手测新建失败/成功两种路径。 **风险**：低。纯交互契约调整，不改数据流。

---

### P0-2　operationTime 误绑 createTime（4 处同源 bug）

**问题**：详情页「操作时间」与「创建时间」都绑定 `createTime`，渲染完全相同的时刻。卡片层 （`PointCard.vue:60`、
`ProfileCard.vue:36`）与 settings 族（`CommandList.vue:93`、`EventList.vue:90`） 都正确用了 `operateTime`，证明这是 detail
页的复制粘贴回归。

**Before**（`views/device/detail/DeviceDetail.vue:40`）

```html
<el-descriptions-item :label="$t('common.operationTime')"
>{{ timestamp(reactiveData.data.createTime || '') }}
</el-descriptions-item>
```

**After**

```html
<el-descriptions-item :label="$t('common.operationTime')"
>{{ timestamp(reactiveData.data.operateTime || '') }}
</el-descriptions-item>
```

**同改**：

- `views/profile/detail/ProfileDetail.vue:40`（`createTime` → `operateTime`）
- `views/point/detail/PointDetail.vue:32`（同上）

**PointValueCard 的时间字段**（`views/point/value/card/PointValueCard.vue:81/87`）：

```html
<!-- 当前：collectTime 与 saveTime 都显示 data.createTime，二者时刻相同 -->
{{ $t('pointValue.card.collectTime') }}: {{ displayTime(data.createTime) }}   <!-- :81 -->
{{ $t('pointValue.card.saveTime') }}:     {{ displayTime(data.createTime) }}   <!-- :87 -->
```

参照 `PointValue.vue:156-158` 的 `interval = operateTime - createTime`（用于 `delay` 计算）， 语义为 `createTime`=采集/产生时刻、
`operateTime`=保存/入库时刻，建议：

```html
{{ $t('pointValue.card.collectTime') }}: {{ displayTime(data.createTime) }}     <!-- 采集：保持 -->
{{ $t('pointValue.card.saveTime') }}:     {{ displayTime(data.operateTime) }}   <!-- 保存：修正 -->
```

> ⚠️ 若后端 `PointValue` 字段语义相反，则 collectTime/saveTime 对调——实施前与后端字段定义核对一次。

**验证**：构造 `operateTime ≠ createTime` 的 mock 数据，确认两个描述项显示不同时刻。 **风险**：极低。纯展示字段替换。

---

### P0-3　DeviceEdit 切换 driver/profile 加未保存守卫 + i18n 离开确认

**问题**：

1. `device/edit/index.ts:625` `changeAttribute` 与 `:1423` `changeProfile` 切换时直接重建
   `driverFormData`/`pointInfoData`/`commandInfoData`/`eventInfoData` 四类矩阵， **不查 `totalDirtyCount`**，
   几十个未保存脏单元格被无声清空。`onBeforeRouteLeave`（:1478）只拦路由离开，拦不住本页内下拉切换。
2. `:1480` `window.confirm('You have unsaved changes...')` 是全流程唯一硬编码英文文案， 可复用已有的
   `common.discardConfirm`（`config/i18n/locales/en.ts`、`zh.ts`）。

**Before**（`views/device/edit/index.ts:1423`）

```ts
const changeProfile = () => {
  pointInfo();
  commandInfo();
  eventInfo();
};
```

**After**（抽取一个脏数据守卫；`changeAttribute` / `changeProfile` 复用）

```ts
import {useI18n} from 'vue-i18n';
const {t} = useI18n();

const confirmDiscardIfDirty = (next: () => void) => {
  if (totalDirtyCount.value > 0 && !window.confirm(t('common.discardConfirm'))) {
    return; // 用户取消：不执行切换
  }
  next();
};

const changeProfile = () => {
  confirmDiscardIfDirty(() => {
    pointInfo();
    commandInfo();
    eventInfo();
  });
};
// changeAttribute(driverId) 同理：把 reactiveData.loading = true; Promise.allSettled([...]) 包进回调
```

**After**（离开确认 i18n 化，`:1480`）

```ts
const leave = window.confirm(t('common.discardConfirm'));
```

> ⚠️ 注意点：`changeAttribute` 由 driver 下拉的 `@change` 触发，若用户取消需把下拉值 **回退**到切换前
> 的 `oldDriverFormData` 对应 driverId（否则 UI 已变但数据未重建，状态不一致）。建议在 select 上用
> `:value` + 手动 commit 模式，或在守卫取消时 `reactiveData.deviceFormData.driverId = prevDriverId`。
> 进一步可用 `ElMessageBox.confirm` 取代原生 `window.confirm` 以保持视觉统一。

**验证**：先在矩阵里改几个单元格不保存，再切换 driver/profile，应弹确认；取消则数据保留。 **风险**
：中。需处理「取消后回退下拉值」的边界，建议配组件测试。

---

### P0-4　PointValue 分页 reset + DeviceImport 空文件永久 loading

**问题 A**：`views/point/value/PointValue.vue:110` 的 `list()` 使用 `reactiveData.page`，但
`search()` / `sizeChange()` 只更新 `query`/`size` 就调用 `list()`， **未把 `page.current` 重置为 1**
（`usePagedList` 会重置，而这里是手写分页）→ 在第 3 页搜索会落到空页。

**After**（最小修复：在 `search()` / `sizeChange()` 开头重置）

```ts
const search = () => {
  reactiveData.page.current = 1;   // 新增
  list();
};
const sizeChange = () => {
  reactiveData.page.current = 1;   // 新增
  list();
};
```

> 更彻底的做法（M 工作量）：让 PointValue 接入 `usePagedList`，`request` 里按 `embedded` 二选一
> `listPointValue` / `getPointValueLatest`，顺带补齐错误态（见 P1-1）。

**问题 B**：`views/device/import/DeviceImportForm.vue:267` `importThing` 在 `form.validate()` 后立即
`submit()` 并置 `formLoading = true`；但 `el-upload`（`auto-upload=false`）在 **文件列表为空**时
`submit()` 不触发 `http-request` → 确认按钮永久 loading、无任何提示。

**Before**

```js
const importThing = async () => {
  const form = unref(formDataRef);
  if (!form) return;
  try {
    await form.validate();
    formUploadRef.value?.submit();
    reactiveData.formLoading = true;
  } catch { /* ... */ }
};
```

**After**

```js
const importThing = async () => {
  const form = unref(formDataRef);
  if (!form) return;
  try {
    await form.validate();
    const files = formUploadRef.value?.uploadFiles ?? [];
    if (!files.length) {
      failMessage(t('device.import.noFile'));   // 需补 en/zh i18n key
      return;
    }
    formUploadRef.value?.submit();
    reactiveData.formLoading = true;
  } catch { /* ... */ }
};
```

> 同时确认 `http-request` handler 在其 `finally` 里复位 `formLoading = false`，避免上传完成后按钮卡住。

**验证**：PointValue 翻到第 3 页再搜索，应回到第 1 页；导入弹窗不选文件点确认，应提示而非卡 loading。 **风险**：低。

---

## P1 体验与一致性（短期，M 工作量）

### P1-1　系统化「错误/加载/空」三态：停止吞错 + CardListShell 补 error/retry

**问题**：`composables/usePagedList.ts:87` 的 `catch { // handled globally }` 与
`PointValue.vue:126/140` 的 `.catch(() => {})` 静默吞错，`listData` 残留空数组，UI 退化为 `el-empty`， 用户
**无法区分「无数据」与「加载失败」**；全仓 grep `retry` 零命中，无重试入口。

**方案**：

1. `usePagedList` 增加 `error` 态：`catch` 里捕获并存 `error` 标识，`load` 暴露 `retry`；
2. 抽取 `components/card/list/CardListShell.vue`（复用 `usePagedList`），封装 loading（12 个
   `SkeletonCard`）+ empty（`el-empty`）+ **error+retry** 三态 + 卡片栅格；
3. `Device.vue` / `Profile.vue` / `Point.vue` / `PointValue.vue` 仅传 card 组件与 query，删除三处近乎逐行 重复的列表模板。

**收益**：一次修复覆盖 device/profile/point/command/event/pointValue 全族列表页。 **验证**：mock `listXxx`
reject，应显示错误态 + 重试按钮，而非空态。

---

### P1-2　令牌化收敛：裸 hex → CSS 变量 + 圆角令牌 + 共享 footer 类

**问题**：大量样式绕过 Element Plus CSS 变量与 palette 令牌，阻断统一调参与暗色模式。

| 位置                                                            | 当前                                  | 建议                                                                               |
|-----------------------------------------------------------------|---------------------------------------|------------------------------------------------------------------------------------|
| `components/card/actions/ThingsCardActions.vue:24/37/50`        | `icon-color: #e6a23c/#67c23a/#f56c6c` | `var(--el-color-warning/success/danger)`                                           |
| `components/card/base/CardShell.vue:117`                        | `background: #f6f7f9`                 | `var(--el-fill-color-light)`                                                       |
| `views/driver/card/DriverCard.vue:134`                          | `border-top: 1px solid #dcdfe6`       | `var(--el-border-color)`                                                           |
| `components/card/stat/StatCard.vue:125`（TS）                   | `purple: '#9059f6'`                   | 引用 `DASHBOARD_PALETTE.driver`（`config/constant/palette.ts`），消除 TS/SCSS 双写 |
| `components/card/stat/StatCard.vue:168`（SCSS）                 | `--stat-card-accent: #9059f6`         | `@use '@/styles/palette'; → $dashboard-driver`                                     |
| 全仓 30+ 处（含 `ThingsCardHeader.vue:53`、`SkeletonCard.vue`） | `border-radius: 4px`                  | 新增 `$radius-card` 令牌或用 `var(--el-border-radius-base)`                        |

**圆角令牌**：`config/plugins/element/element-variables.scss` 当前仅有 `$form-width-*` 令牌（且被 Vite
`additionalData` 全局注入，符合约定）。在其中新增：

```scss
$radius-card: 4px;     // 卡片图标容器/骨架等
$radius-control: var(--el-border-radius-base);  // 控件
```

各 scoped style 因 `additionalData` 注入可直接 `border-radius: $radius-card;`。

**共享 footer 类**：`.things-card__footer` / `.things-card-footer-operation` 目前在
`ThingsCardActions.vue:86`、`DriverCard.vue:129`、`PointValueCard.vue:351` 三处近乎拷贝（已出现
`#dcdfe6` 与 `var(--el-border-color)` 漂移）。在 `styles/things-card.scss` 抽出公共类：

```scss
.things-card__footer {
  height: 35px;
  margin-top: 2px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--el-border-color);

  .things-card-footer-operation {
    height: 35px;
    display: flex;
  }
}
```

三处卡片删除各自的 scoped footer 外壳样式，仅保留按钮。

**验证**：`pnpm build`；切暗色（若已支持）确认无残留亮色硬编码。 **风险**：低，但面广——建议分文件小步提交，每步 `pnpm build`。

---

### P1-3　抽 useRemoteDictionary 组合式 + Device/Profile 共享字段组件

**问题**：`driverDictionary` / `profileDictionary` 的「loading + `listXxxDictionary({page,label})` + visible-change 触发 +
catch 吞错」模式在 5+ 处逐字重复（`DeviceAddForm.vue:164`、
`DeviceImportForm.vue:177`、`device/edit/index.ts:551`、`DeviceTool.vue:118`、`PointTool.vue:139`， 且 `PointTool.vue:160` 在
setup 顶层无条件预拉是浪费请求）。同时 device 的 add 与 edit 字段/校验各写 一份（仅 `PointEditForm` 做到 add/edit 单组件复用）。

**方案**：

1. 新增 `composables/useRemoteDictionary.ts`：

   ```ts
   export function useRemoteDictionary(kind: 'driver' | 'profile', size = 50) {
     // 返回 { options, loading, remoteMethod, onVisible }
     // 统一 async/await + visible-change 懒加载，五处替换
   }
   ```

2. 抽 `views/device/components/DeviceFormFields.vue`（含 `deviceName/driverId/profileId/remark` 字段 + 校验 + 字典加载），
   `DeviceAddForm` 弹窗与 `DeviceEdit` 的 `InfoCard` `#fields` 插槽共用；
   `ProfileFormFields.vue` 同理。

**验证**：`pnpm test`；手测下拉远程搜索、visible 懒加载。 **风险**：中。涉及多文件，保持 props/事件契约不变即可低风险替换。

---

### P1-4　StatCard 补加载态 + 业务卡实体色身份 + 修 DriverCard 死 .active

**问题 A**：`StatCard.vue:72-87` 的 props 无 `loading`，`Home.vue` 首屏 `value` 默认 `0`、sparkline 空 → 显示 **虚假数据**
（而 `DashboardCard` 有完整 loading、列表卡有 `SkeletonCard`，三套不一致）。

**方案**：给 `StatCard` 加 `loading` prop，数值区与 sparkline 区用 `el-skeleton` 占位，命名与
`DashboardCard.loading` 对齐；可选地给 `SkeletonCard` 增 `variant: 'list' | 'stat' | 'dashboard'`。

**问题 B**：`palette.scss:23-26` 已定义 driver 紫 / device 蓝 / profile 橙 / point 绿 四色令牌，但仅用于 dashboard，5
张业务卡都用通用 PNG 图标 + `el-color-primary`， **翻卡时无法一眼分辨类型**。

**方案**：在 `ThingsCardHeader.vue:49-60` 的图标容器（当前仅 `border-radius:4px` 无底色）加一层 8% 透明度的实体色底（通过新增
`tone` prop 传入 `$dashboard-*` 令牌），各业务卡传自己的实体色。

**问题 C**：`views/driver/card/style.scss:18-20` 的 `.active { border-left: 5px solid #409eff }` 是死代码，
`DriverCard.vue:93` emit 的 `select-change` 未被 `Driver.vue` 消费 → DriverCard 是 **唯一没有选中反馈的 列表卡**（对比
`PointInfoCard.vue:20` 用 `shadow='always'` 表达选中）。

**方案（二选一）**：

- **A. 接线**（推荐，与其他卡片选中语义对齐）：`Driver.vue` 维护 `selectedId`，绑 `@select-change`，
  `DriverCard` 根节点 `:class="{ active: selected }`，`.active` 的 `#409eff`（device 蓝）改为
  `$dashboard-driver`（紫）；
- **B. 删除**：删 `style.scss` 整个文件 + `DriverCard.vue:126` 的 `@use` + 未消费的 `select-change` emit。

**验证**：`pnpm build`；首页首屏应显示骨架而非 0；driver 列表点选应有高亮反馈。 **风险**：低（A）/ 极低（B）。

---

### P1-5　PointValueCard 数值按质量着色 + 稳定字号

**问题**：`views/point/value/card/PointValueCard.vue:321-326` 实时值用 `font-size: xx-large`（相对尺寸， 跨屏不稳定）+ 装饰性
`hue` 动画（`:324`、`:364-374`，primary→light-9→primary），颜色不承载信息； 唯一着色是 `value-missing` 灰。卡片已算出
`delayOk` / `delaySlow`（`:180-185`）却只用来染 header 下边框。

**Before**

```scss
.value {
  font-weight: bold;
  font-size: xx-large;
  animation: hue 1s ease-in;
  cursor: pointer;
}
@keyframes hue { 0%{color:var(--el-color-primary)} 50%{color:var(--el-color-primary-light-9)} 100%{color:var(--el-color-primary)} }
```

**After**

```scss
.value {
  font-weight: bold;
  font-size: 28px;
  font-variant-numeric: tabular-nums;   // 防数值跳动
  cursor: pointer;
  color: var(--value-tone, var(--el-text-color-primary));
}
.value--fresh  { --value-tone: var(--el-color-success); }
.value--stale  { --value-tone: var(--el-color-warning); }
.value-missing { color: var(--el-text-color-secondary); }
/* 删除 @keyframes hue */
```

模板按质量位绑定 `:class`：`delayOk` → `value--fresh`、`delaySlow` → `value--stale`，让颜色传达「新鲜度」。

**验证**：`pnpm build`；构造不同 delay 的位号值，确认数值随新鲜度变色、字号稳定。 **风险**：低。

---

## P2 打磨与基础设施（迭代）

- **a11y 专项（最大盲区）**：全仓仅 9 处 `aria`/`role`。优先给纯图标按钮（`el-button` 仅 `:icon`：
  edit/delete/refresh/disable/import 等）批量补 `aria-label`；自定义交互（`PointValueCard`、卡片选中态） 加 `role` + keyboard
  handler；`components/layout/Layout.vue` 落地 skip-link 与弹窗/抽屉焦点管理。
- **错误通知 i18n 化**：`config/axios/index.ts:130-150` 全局错误通知硬编码英文（`'Server Error'` /
  `'Network Error'`），影响面大于 `device/edit` 单点，统一收敛为 i18n key；并做 `en.ts(1334)` vs
  `zh.ts(1332)` 的 key parity 审计。
- **暗色模式**（依赖 P1-2 令牌化前置完成）：引入 Element Plus dark CSS + `useDark`，settings 增主题切换。
- **死代码清理**：`components/card/title/TitleCard.vue`（零消费者）、`PointInfoCard.vue` 未用字段、
  `profile/detail/index.ts` 未用字段、`Profile.vue` 的 `deviceId` 死参 + `listProfileByDeviceId`； 删除三处无效的
  `enableFlag` 空校验规则（`PointEditForm.vue:172`、`device/edit/index.ts:376`、
  `profile/edit/index.ts:58`）。
- **DeviceEdit 现代化**：由 `defineComponent` 迁移到 `<script setup>`；抽泛型 `ConfigMatrix<T>` 组件 +
  `useConfigMatrix` 组合式，消除 point/command/event 矩阵三段重复（约 2/3 代码）。
- **位号值强类型化**：新增 `PointValueRecord` / `PointValueWriteForm` 替换全流程 `any`；
  `PointValueDetail` 用 `el-descriptions` 结构化替代 JSON 原文。
- **性能**：dictionary remote 与文本过滤加防抖（`useDebounceFn`）；`EventHistory` / `CommandHistory` /
  `PointValue` 大表接入 `el-table-v2` 虚拟滚动；重构 `store/modules/interval.ts` 单例为多实例以支撑实时刷新。
- **i18n 范围声明**：`config/i18n/index.ts:26` `SUPPORTED = ['en','zh']`，实际仅 en/zh——视产品定位决定 补 ja/es/ru 或正式声明
  en/zh-only。

---

## 横切关注点

1. **a11y 是六维度全部遗漏的最大盲区**——优先做一次 a11y 扫描：先给纯图标按钮批量补 `aria-label`
   （成本最低收益最大），再在 `Layout.vue` 落地 skip-link 与焦点管理。
2. **「错误态」系统性缺失横切所有列表页**——根因在组合式层（`usePagedList` 吞错），应让
   `usePagedList` 暴露 `error` + `retry`，并以 `CardListShell` 收敛三态，一次修复覆盖全族（见 P1-1）。
3. **i18n 完整性需作为独立纪律**——实际仅 en/zh；除 `discardConfirm` 等页面级硬编码外，
   `axios` 全局错误通知的硬编码英文影响面更大，应统一收敛并补 en/zh key parity 审计。

---

## 验证命令（`dc3-web/` 下，按改动范围按比例选取）

```bash
pnpm check          # vue-tsc 类型检查
pnpm lint:check     # ESLint
pnpm test           # Vitest 单元/组件
pnpm test:guard     # 路由守卫等 guardrails
pnpm build          # Vite 生产构建（令牌化/样式改动必跑）
```

> 约定提醒（`AGENTS.md`）：用 pnpm；`verbatimModuleSyntax` 开启（类型用 `import type`）；
> `<Entity>Form` 用于新建/编辑载荷、`<Entity>Record` 用于读响应；64 位 ID 用字符串；
> Element Plus 全局变量由 Vite 注入，组件内勿重复 `@use`。
