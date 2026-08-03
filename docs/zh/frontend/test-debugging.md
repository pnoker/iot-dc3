# 测试调试常见问题

这里汇总编写或运行前端测试时最常反复出现的报错，并给出快速解答。测试约定参见 `dc3-web/` 项目下的 `tests/README.md`，机械执行的规则参见
`tests/guardrails/ai-guardrails.test.ts`。

## 测试抛出 "Unexpected Vue warning: ..."

`tests/setup/vitest.setup.ts` 这个 setup 文件会把 `[Vue warn]` / `[Vue error]` 提升为抛出的错误。最常见的几类原因：

- **Failed to resolve component: el-xxx** —— 组件模板里用到了一个你还没有 stub 的 Element Plus 组件。把它加到 `tests/setup/stubs/element-plus.ts`
  的 `layoutStubs` 中（推荐），或在挂载时通过 `global.stubs` 传入。
- **injection "Symbol(router)" not found** —— 组件调用了 `useRouter()` / `useRoute()`，但测试挂载时没有提供 router。使用内存 router：
  ```ts
  import { createMemoryHistory, createRouter } from 'vue-router';
  const router = createRouter({ history: createMemoryHistory(), routes: [...] });
  mount(Comp, { global: { plugins: [i18n, router] } });
  ```
- **Failed setting prop "modelValue"** —— 已在允许名单中；如果报的是别的 prop，把对应的正则加到 `VUE_WARN_ALLOWLIST`，并附注释说明为何无法在源头修复。

本地调试时如需临时绕过：

```bash
VITEST_ALLOW_VUE_WARN=1 pnpm test
```

不要提交需要绕过的代码 —— 这条警告意味着存在真实的契约缺口。

## vi.mock 工厂里出现 "Cannot access X before initialization"

现象：`ReferenceError: Cannot access 'someMock' before initialization`，指向某个 `vi.mock(...)` 调用。根因是：工厂函数引用了一个顶层 `const`，而
`vi.mock` 会被 Vitest 提升到文件顶部，此时这个 const 尚未初始化。

修复方式：用 `vi.hoisted` 包裹 spy：

```ts
const apiMocks = vi.hoisted(() => ({
  doSomething: vi.fn(),
}));
vi.mock('@/api/foo', () => apiMocks);
```

对于包含多个 `vi.mock` 的文件，shared-mocks guardrail 会强制要求这种写法。

## 本地通过、CI 上因 "leftover state" 失败

排查在测试之间残留的模块级状态：

- **模块级响应式缓存**（例如 `useEntityNames` 的 `cache` / `inflight` 对象）。用 `vi.resetModules()` 并在 `beforeEach` 中重新 import 来重置。
- **Pinia store 在测试间持久化**。始终在 `beforeEach` 中 `setActivePinia(createPinia())`。
- **localStorage / sessionStorage**。全局 setup 会在 `afterEach` 清理它们，但如果你的测试依赖"运行开始时数据不存在"，也要在 `beforeEach` 中清理。

## "as unknown as T" 或 "as never" guardrail 失败

这条 guardrail 禁止用双重断言抹除类型。改为：

- 类型化的 fixture 构造器：`function makeRequest(): Request { return { … }; }`
- 在违反契约的那一行单点标注 `// @ts-expect-error — intentionally invalid input for whitelist test`

如果测试确实需要 `as never` 才能满足某个泛型约束，通常说明生产代码的类型标注有误，应在源头修复。

## "tests/component/foo.test.ts contains wrapper.vm.x()" guardrail 失败

请通过组件的公开表面驱动它 —— props、slots、emits、DOM 事件。调用 `wrapper.vm.someMethod()` 会让测试耦合到内部实现，一旦组件重构就会失效。如果确实需要逃生口，在组件里用 `defineExpose` 暴露，再通过 expose 表面调用（仍然是 `wrapper.vm.someMethod()`，但因为 expose 属于契约的一部分，测试在重构后仍能存活）。

## "describe must use lowercase verb (no should...)" guardrail 失败

改写措辞。describe 块命名主语，it 块命名行为：

```ts
// ❌
it('should validate before searching', …);

// ✅
it('validates before searching', …);
```

## 覆盖率低于阈值

本地运行 `pnpm test:coverage`。控制台报告会显示每个文件的缺口。如果确实新增了未覆盖的代码，补上测试；如果你删除了被覆盖的代码（有意的重构），百分比可能足以掩盖下降，否则在 `vitest.config.ts` 中调低阈值并在 PR 描述中说明。

不要靠写同义反复的测试来应付阈值 —— "forbids tautological assertions" guardrail 会捕获 `expect(true).toBe(true)` 这类写法。

## 一次小的 API 改动引发巨大的 snapshot diff

`tests/api/api-contracts.test.ts.snap` 大约 2800 行，因为它覆盖了每一个 API wrapper。对某个 wrapper的小改动会触发其中一小段聚焦的 diff；如果出现巨大 diff，说明很多 wrapper 都发生了变化。自检：

- 鉴权头的结构变了吗？（存储格式变更）
- URL 前缀移动了吗？（代理 / 版本升级）
- 是否往被追踪的模块里新增了 wrapper？

只有在人工阅读过 diff 之后，才运行 `pnpm test:api -u` 更新快照。

## 新的 fixture 应该放在哪里？

放在 `tests/fixtures/`。如果某个兄弟测试可以复用同样的数据结构，就不要在测试里内联一个 30 行的样例树。guardrail 会检查该目录是否存在；约定要求你持续往里添加文件（`auth.ts`、`menu.ts`、`rows.ts` 可作为起点）。
