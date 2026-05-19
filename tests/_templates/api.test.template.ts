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
//   4. If the module has unusual call signatures, add cases to
//      `sampleArgs(name)` so the snapshot driver passes correct fixtures.
//
// Do NOT create a parallel `tests/api/<module>.test.ts` — duplicating
// the snapshot machinery defeats the purpose of the consolidated contract.
export {};
