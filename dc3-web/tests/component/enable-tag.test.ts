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
