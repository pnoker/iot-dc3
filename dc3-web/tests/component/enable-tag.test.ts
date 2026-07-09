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
import EnableTag from '@/components/tag/EnableTag.vue';

const ElTagStub = {
  name: 'ElTag',
  props: ['type', 'size'],
  template: '<span class="el-tag-stub" :data-type="type" :data-size="size"><slot /></span>',
};

function mountTag(props: Record<string, unknown>) {
  return mount(EnableTag, {
    props,
    global: {
      plugins: [i18n],
      stubs: {ElTag: ElTagStub},
    },
  });
}

describe('EnableTag', () => {
  it('renders the success type with the enable label for enabled values', () => {
    for (const value of ['ENABLE', 'enabled', true, '0']) {
      const wrapper = mountTag({value});
      const tag = wrapper.find('.el-tag-stub');
      expect(tag.attributes('data-type')).toBe('success');
      expect(tag.text()).toBe('Enable');
    }
  });

  it('renders the info type with the disable label for disabled or missing values', () => {
    for (const value of ['DISABLE', false, null, undefined, '']) {
      const wrapper = mountTag({value});
      const tag = wrapper.find('.el-tag-stub');
      expect(tag.attributes('data-type')).toBe('info');
      expect(tag.text()).toBe('Disable');
    }
  });

  it('forwards the size prop to the underlying tag', () => {
    const wrapper = mountTag({value: 'ENABLE', size: 'small'});
    expect(wrapper.find('.el-tag-stub').attributes('data-size')).toBe('small');
  });
});
