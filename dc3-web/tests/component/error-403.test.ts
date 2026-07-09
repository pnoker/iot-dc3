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
