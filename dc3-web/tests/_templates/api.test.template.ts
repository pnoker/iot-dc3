/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
