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

import TitleCard from '@/components/card/title/TitleCard.vue';

import {layoutStubs} from '../setup/stubs/element-plus';

describe('TitleCard', () => {
  it('renders the title prop in the default header span', () => {
    const wrapper = mount(TitleCard, {
      props: {title: 'Devices'},
      slots: {default: '<p class="content">content</p>'},
      global: {stubs: {...layoutStubs}},
    });

    const header = wrapper.find('.title-card__header');
    expect(header.exists()).toBe(true);
    expect(header.text()).toBe('Devices');
    expect(wrapper.find('.content').exists()).toBe(true);
  });

  it('lets a header slot override the default title rendering', () => {
    const wrapper = mount(TitleCard, {
      props: {title: 'unused'},
      slots: {header: '<h2 class="custom">Custom header</h2>'},
      global: {stubs: {...layoutStubs}},
    });

    expect(wrapper.find('.custom').text()).toBe('Custom header');
    expect(wrapper.find('.title-card__header').exists()).toBe(false);
  });
});
