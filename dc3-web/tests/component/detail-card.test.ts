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
