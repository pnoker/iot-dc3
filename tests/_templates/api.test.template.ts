/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// API contract tests for new wrappers go into `tests/api/api-contracts.test.ts`
// — that file already iterates over every `src/api/**` module and snapshots
// the transport, URL, and body. To add a new module:
//
//   1. Add `import * as <module>Api from '@/api/<module>';` to that file.
//   2. Add the namespace to the `modules` map.
//   3. Add the source-file base name to `coveredApiSourceFiles`.
//   4. If the module has unusual call signatures (or you want explicit, not
//      pattern-derived, fixtures), add an entry to `sampleArgsRegistry` —
//      that is the preferred path for new wrappers. The trailing regex
//      heuristic stays for legacy fixtures only.
//
// Do NOT create a parallel `tests/api/<module>.test.ts` — duplicating
// the snapshot machinery defeats the purpose of the consolidated contract.
//
// ── Two transport flavors to be aware of ───────────────────────────────
//
// (1) Direct `httpGet` / `httpPost` calls. Mocked via:
//
//     vi.mock('@/api/common', () => ({
//       httpGet: apiSpies.httpGet,
//       httpPost: apiSpies.httpPost,
//     }));
//
//     Snapshot will record `{ transport: 'httpGet'|'httpPost', args: [...] }`.
//
// (2) `crud*` shorthand (used by `src/api/command.ts`, `src/api/event.ts`,
//     and any new module that follows the same pattern). Mocked by
//     forwarding through `httpGet`/`httpPost` so existing assertions in
//     `expectStandardUrl` / `expectStandardParams` keep working:
//
//     vi.mock('@/api/common', () => ({
//       httpGet: apiSpies.httpGet,
//       httpPost: apiSpies.httpPost,
//       crudAdd:    (base: string, payload: unknown) => apiSpies.httpPost(`${base}/add`, payload),
//       crudUpdate: (base: string, payload: unknown) => apiSpies.httpPost(`${base}/update`, payload),
//       crudDelete: (base: string, id: string)       => apiSpies.httpPost(`${base}/delete`, undefined, { params: { id } }),
//       crudGetById:(base: string, id: string)       => apiSpies.httpGet(`${base}/get_by_id`, { params: { id } }),
//       crudList:   (base: string, query: unknown)   => apiSpies.httpPost(`${base}/list`, query),
//     }));
//
//     With this mock in place, a wrapper like
//       export const addCommand = (p: Partial<CommandRecord>) => crudAdd(endpoints.command, p);
//     records the same `httpPost` snapshot shape as if you'd written
//       export const addCommand = (p) => httpPost(`${endpoint}/add`, p);
//     This keeps the contract test in lockstep with both flavors.
//
// ── Naming convention reminder (CLAUDE.md) ─────────────────────────────
//
//   - getXxx       single record, snake_case path /get_*
//   - listXxx      collection,    snake_case path /list_* or /list
//   - listXxxByYyy collection scoped by foreign key
//
// `getXxxList*` / `getXxxByIds` / `select_*` are blocked by
// `tests/guardrails/ai-guardrails.test.ts`.
export {};
