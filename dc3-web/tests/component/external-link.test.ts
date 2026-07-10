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
