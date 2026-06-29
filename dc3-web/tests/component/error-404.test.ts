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
import Error404 from '@/components/error/404.vue';

import {createElButtonStub, layoutStubs} from '../setup/stubs/element-plus';

describe('Error404', () => {
  it('renders a 404 result with the not-found subtitle', () => {
    const wrapper = mount(Error404, {
      global: {
        plugins: [i18n],
        stubs: {
          ...layoutStubs,
          ElButton: createElButtonStub(),
          RouterLink: {props: ['to'], template: '<a class="router-link-stub"><slot /></a>'},
        },
      },
    });

    const result = wrapper.find('.el-result-stub');
    expect(result.attributes('data-title')).toBe('404');
    expect(result.attributes('data-icon')).toBe('warning');
    expect(result.attributes('data-sub-title')).toBe('Sorry, the page you visited does not exist');
    expect(wrapper.find('button.el-button-stub').exists()).toBe(true);
  });
});
