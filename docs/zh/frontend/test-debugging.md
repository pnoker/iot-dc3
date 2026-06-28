# Test Debugging FAQ

Quick answers to the failures that recur most often when writing or running
the frontend tests. Pair this with `tests/README.md` (in the `dc3-web/` project) for
the conventions, and `tests/guardrails/ai-guardrails.test.ts` for the
mechanically-enforced rules.

## "Unexpected Vue warning: ..." thrown from a test

The setup file in `tests/setup/vitest.setup.ts` promotes `[Vue warn]` /
`[Vue error]` to thrown errors. The most common causes:

- **Failed to resolve component: el-xxx** — the component template uses an
  Element Plus component you haven't stubbed. Add it to `layoutStubs` in
  `tests/setup/stubs/element-plus.ts` (preferred) or pass it under
  `global.stubs` in the mount call.
- **injection "Symbol(router)" not found** — the component calls
  `useRouter()` / `useRoute()` but the test didn't mount with a router.
  Use a memory router:
  ```ts
  import { createMemoryHistory, createRouter } from 'vue-router';
  const router = createRouter({ history: createMemoryHistory(), routes: [...] });
  mount(Comp, { global: { plugins: [i18n, router] } });
  ```
- **Failed setting prop "modelValue"** — already in the allowlist; if you
  see a different prop, add the regex to `VUE_WARN_ALLOWLIST` with a comment
  explaining why it can't be fixed at the source.

To temporarily bypass while debugging locally:

```bash
VITEST_ALLOW_VUE_WARN=1 pnpm test
```

Don't commit code that needs the bypass — the warning indicates a real
contract gap.

## "Cannot access X before initialization" in vi.mock factory

Symptom: `ReferenceError: Cannot access 'someMock' before initialization`
pointing at a `vi.mock(...)` call. Root cause: the factory references a
top-level `const`, but `vi.mock` is hoisted to the top of the file by
Vitest, so the const isn't initialized yet.

Fix: wrap the spies in `vi.hoisted`:

```ts
const apiMocks = vi.hoisted(() => ({
  doSomething: vi.fn(),
}));
vi.mock('@/api/foo', () => apiMocks);
```

The shared-mocks guardrail enforces this for files with more than one
`vi.mock`.

## Test passes locally but fails on CI for "leftover state"

Look for module-level state that survives between tests:

- **Module-level reactive cache** (e.g. `useEntityNames`'s `cache` /
  `inflight` objects). Reset by `vi.resetModules()` + re-importing in
  `beforeEach`.
- **Pinia store persisted across tests**. Always
  `setActivePinia(createPinia())` in `beforeEach`.
- **localStorage / sessionStorage**. The global setup clears these in
  `afterEach`, but if your test relies on data being absent at the start
  of the run, clear in `beforeEach` too.

## "as unknown as T" or "as never" guardrail failure

The guardrail forbids erasing types via double assertion. Replace with:

- A typed fixture builder: `function makeRequest(): Request { return { … }; }`
- `// @ts-expect-error — intentionally invalid input for whitelist test`
  on the single line that violates the contract.

If the test really needs `as never` to satisfy a generic constraint, that
usually means the production code is mistyped; fix it at the source.

## "tests/component/foo.test.ts contains wrapper.vm.x()" guardrail failure

Drive the component through its public surface — props, slots, emits, DOM
events. Calling `wrapper.vm.someMethod()` couples the test to internals
that change whenever the component is refactored. If you genuinely need
an escape hatch, use `defineExpose` in the component and call through the
expose surface (still `wrapper.vm.someMethod()`, but tests survive the
refactor because expose is part of the contract).

## "describe must use lowercase verb (no should...)" guardrail failure

Rephrase. The describe block names the subject, the it block names the
behaviour:

```ts
// ❌
it('should validate before searching', …);

// ✅
it('validates before searching', …);
```

## Coverage drops below threshold

Run `pnpm test:coverage` locally. The console report shows per-file gaps.
If you genuinely added new untested code, add the test; if you removed
covered code (intentional refactor), the percentage may rise enough to
hide the drop, otherwise lower the threshold in `vitest.config.ts` and
flag it in the PR description.

Don't game the threshold by writing tautological tests — the
"forbids tautological assertions" guardrail catches `expect(true).toBe(true)`.

## Snapshot diff is huge after a small API change

The `tests/api/api-contracts.test.ts.snap` file is ~2800 lines because it
covers every API wrapper. A small change to a wrapper triggers a small
focused diff inside it; a giant diff means many wrappers shifted. Ask:

- Did the auth headers change shape? (storage-format change)
- Did the URL prefix move? (proxy / version bump)
- Was a new wrapper added to a tracked module?

Run `pnpm test:api -u` to update only after manually reading the diff.

## Where do new fixtures go?

`tests/fixtures/`. Don't inline a 30-line sample tree in a test if a
sibling test could use the same shape. The guardrail checks the directory
exists; convention asks you to keep adding files (`auth.ts`, `menu.ts`,
`rows.ts` as starting points).
