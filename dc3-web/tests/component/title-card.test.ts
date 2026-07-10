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
