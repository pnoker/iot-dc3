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

import SkeletonCard from '@/components/card/skeleton/SkeletonCard.vue';

import {layoutStubs} from '../setup/stubs/element-plus'; // el-skeleton renders the `template` slot (skeleton placeholder) when

// el-skeleton renders the `template` slot (skeleton placeholder) when
// loading=true, and the `default` slot when loading=false. We stub it
// to render BOTH so we can assert on placeholder structure regardless
// of the loading flag — the real component swaps based on loading.
const ElSkeletonStub = {
  name: 'ElSkeleton',
  props: ['loading'],
  template: '<div class="el-skeleton-stub" :data-loading="loading"><slot name="template" /><slot /></div>',
};

const ElSkeletonItemStub = {
  name: 'ElSkeletonItem',
  props: ['variant'],
  template: '<span class="el-skeleton-item-stub" :data-variant="variant" />',
};

function mountSkeleton(props: Record<string, unknown> = {}) {
  return mount(SkeletonCard, {
    props,
    slots: {default: '<p class="loaded">loaded</p>'},
    global: {
      stubs: {
        ...layoutStubs,
        ElSkeleton: ElSkeletonStub,
        ElSkeletonItem: ElSkeletonItemStub,
      },
    },
  });
}

describe('SkeletonCard', () => {
  it('renders the 12-card placeholder grid with footer buttons by default', () => {
    const wrapper = mountSkeleton({loading: true});

    // 12 cards — verified via the icon placeholder count.
    expect(wrapper.findAll('.skeleton-card-icon')).toHaveLength(12);
    // footer=true (default) → 5 button placeholders per card × 12 = 60.
    expect(wrapper.findAll('[data-variant="button"]')).toHaveLength(60);
  });

  it('omits the footer buttons when footer=false', () => {
    const wrapper = mountSkeleton({loading: true, footer: false});
    expect(wrapper.findAll('[data-variant="button"]')).toHaveLength(0);
  });

  it('exposes the default slot content for the loaded state', () => {
    const wrapper = mountSkeleton({loading: false});
    expect(wrapper.find('.loaded').text()).toBe('loaded');
  });
});
