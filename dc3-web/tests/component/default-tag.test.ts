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
import DefaultTag from '@/components/tag/DefaultTag.vue';

const ElTagStub = {
  name: 'ElTag',
  props: ['type', 'size'],
  template: '<span class="el-tag-stub" :data-type="type"><slot /></span>',
};

function mountTag(props: Record<string, unknown>) {
  return mount(DefaultTag, {
    props,
    global: {
      plugins: [i18n],
      stubs: {ElTag: ElTagStub},
    },
  });
}

describe('DefaultTag', () => {
  it('renders the success type with the yes label when value matches activeValue', () => {
    const wrapper = mountTag({value: 'DEFAULT'});
    const tag = wrapper.find('.el-tag-stub');
    expect(tag.attributes('data-type')).toBe('success');
    expect(tag.text()).toBe('Yes');
  });

  it('renders the info type with the no label when value does not match', () => {
    const wrapper = mountTag({value: 'NORMAL'});
    const tag = wrapper.find('.el-tag-stub');
    expect(tag.attributes('data-type')).toBe('info');
    expect(tag.text()).toBe('No');
  });

  it('honors a custom activeValue prop for non-default semantics', () => {
    const wrapper = mountTag({value: 1, activeValue: 1});
    expect(wrapper.find('.el-tag-stub').attributes('data-type')).toBe('success');
  });
});
