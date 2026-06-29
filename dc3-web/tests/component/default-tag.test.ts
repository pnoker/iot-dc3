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
