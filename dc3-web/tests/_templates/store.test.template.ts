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
