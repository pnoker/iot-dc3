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

import ExternalLink from '@/components/link/ExternalLink.vue';

const ElLinkStub = {
  name: 'ElLink',
  props: ['href', 'type', 'rel', 'target'],
  template: '<a class="el-link-stub" :href="href" :data-type="type" :rel="rel" :target="target"><slot /></a>',
};

describe('ExternalLink', () => {
  it('always opens in a new tab with the noopener relation', () => {
    const wrapper = mount(ExternalLink, {
      props: {href: 'https://dc3.io/docs'},
      global: {stubs: {ElLink: ElLinkStub}},
    });

    const anchor = wrapper.find('a.el-link-stub');
    expect(anchor.attributes('href')).toBe('https://dc3.io/docs');
    expect(anchor.attributes('target')).toBe('_blank');
    expect(anchor.attributes('rel')).toBe('noopener noreferrer');
    expect(anchor.attributes('data-type')).toBe('primary');
    // No slot content → falls back to rendering the href as text.
    expect(anchor.text()).toBe('https://dc3.io/docs');
  });

  it('honors the custom type prop and renders slot content when provided', () => {
    const wrapper = mount(ExternalLink, {
      props: {href: 'https://example.com', type: 'success'},
      slots: {default: 'Visit example'},
      global: {stubs: {ElLink: ElLinkStub}},
    });

    const anchor = wrapper.find('a.el-link-stub');
    expect(anchor.attributes('data-type')).toBe('success');
    expect(anchor.text()).toBe('Visit example');
  });
});
