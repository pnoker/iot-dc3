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
