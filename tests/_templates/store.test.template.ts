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

// TEMPLATE — copy to `tests/unit/<store-name>-store.test.ts` and fill
// in the parts marked TODO. Does NOT run as a real test (it's not in
// the vitest include glob because of the `.template.ts` suffix).

// import { beforeEach, describe, expect, it, vi } from 'vitest';
// import { createPinia, setActivePinia } from 'pinia';
//
// import { useXxxStore } from '@/store';
//
// // Mock collaborators in a single hoisted block — required when there
// // is more than one vi.mock in the file (guardrail enforced).
// const apiMocks = vi.hoisted(() => ({
//   doSomething: vi.fn(),
// }));
// vi.mock('@/api/<module>', () => apiMocks);
//
// describe('<xxx> store', () => {
//   beforeEach(() => {
//     setActivePinia(createPinia());
//     vi.clearAllMocks();
//     // TODO: default mock return values
//     apiMocks.doSomething.mockResolvedValue({ data: undefined });
//   });
//
//   describe('<actionName>', () => {
//     it('<verb describing the behaviour, lowercase>', async () => {
//       // Arrange
//       const store = useXxxStore();
//
//       // Act
//       await store.someAction();
//
//       // Assert
//       expect(apiMocks.doSomething).toHaveBeenCalledTimes(1);
//       expect(store.someState).toBe(/* concrete value, no toBeTruthy */);
//     });
//   });
// });
export {};
