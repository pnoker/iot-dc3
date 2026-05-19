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
