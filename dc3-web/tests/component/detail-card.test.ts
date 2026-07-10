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

import DetailCard from '@/components/card/detail/DetailCard.vue';

import {layoutStubs} from '../setup/stubs/element-plus';

describe('DetailCard', () => {
  it('wraps default slot content inside the card body container', () => {
    const wrapper = mount(DetailCard, {
      slots: {default: '<p class="payload">payload</p>'},
      global: {stubs: {...layoutStubs}},
    });

    expect(wrapper.find('.detail-card').exists()).toBe(true);
    expect(wrapper.find('.detail-card__container .payload').exists()).toBe(true);
  });
});
