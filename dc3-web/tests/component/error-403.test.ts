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

import {mount} from '@vue/test-utils';
import {describe, expect, it} from 'vitest';

import i18n from '@/config/i18n';
import Error403 from '@/components/error/403.vue';

import {createElButtonStub, layoutStubs} from '../setup/stubs/element-plus';

describe('Error403', () => {
  it('renders a 403 result with a back-home link to /', () => {
    const wrapper = mount(Error403, {
      global: {
        plugins: [i18n],
        stubs: {
          ...layoutStubs,
          ElButton: createElButtonStub(),
          RouterLink: {
            props: ['to'],
            template: '<a class="router-link-stub" :data-to="JSON.stringify(to)"><slot /></a>',
          },
        },
      },
    });

    const result = wrapper.find('.el-result-stub');
    expect(result.attributes('data-title')).toBe('403');
    expect(result.attributes('data-icon')).toBe('warning');
    expect(result.attributes('data-sub-title')).toBe('Sorry, you do not have permission to access this page');
    expect(wrapper.find('.router-link-stub').attributes('data-to')).toBe('{"path":"/"}');
    expect(wrapper.find('button.el-button-stub').exists()).toBe(true);
  });
});
