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

// TEMPLATE — copy to `tests/component/<kebab-name>.test.ts`.

// import { mount } from '@vue/test-utils';
// import { describe, expect, it } from 'vitest';
//
// import i18n from '@/config/i18n';
// import MyComponent from '@/components/<path>/MyComponent.vue';
//
// // Reuse shared Element Plus stubs — guardrail forbids redefining
// // ElButton / ElForm / ElPagination inline.
// import { createElButtonStub, createElFormStub, layoutStubs } from '../setup/stubs/element-plus';
//
// function mountMyComponent(props: Record<string, unknown> = {}) {
//   return mount(MyComponent, {
//     props: { /* required props */, ...props },
//     global: {
//       plugins: [i18n],
//       stubs: {
//         ...layoutStubs,
//         ElButton: createElButtonStub(),
//       },
//     },
//   });
// }
//
// describe('MyComponent', () => {
//   it('<verb-led behaviour driven through public contract>', async () => {
//     const wrapper = mountMyComponent();
//
//     // Drive the component through DOM events — never wrapper.vm.method().
//     await wrapper.find('button.primary').trigger('click');
//
//     expect(wrapper.emitted('save')).toHaveLength(1);
//   });
// });
export {};
