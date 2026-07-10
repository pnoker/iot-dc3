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

import ThingsCardHeader from '@/components/card/header/ThingsCardHeader.vue';

function mountHeader(props: Record<string, unknown>, slots: Record<string, string> = {}) {
  return mount(ThingsCardHeader, {
    props: {icon: '/icons/driver.svg', ...props},
    slots,
  });
}

describe('ThingsCardHeader', () => {
  it('renders the name and icon, and emits copy-id on name click', async () => {
    const wrapper = mountHeader({name: 'Modbus', icon: '/icons/modbus.svg'});

    expect(wrapper.find('.things-card-header-name').text()).toBe('Modbus');
    expect(wrapper.find('img').attributes('src')).toBe('/icons/modbus.svg');
    expect(wrapper.find('img').attributes('alt')).toBe('Modbus');

    await wrapper.find('.things-card-header-name').trigger('click');
    expect(wrapper.emitted('copy-id')).toHaveLength(1);
  });

  it('toggles the enable/disable border class based on the enabled prop', () => {
    const enabled = mountHeader({name: 'A', enabled: true});
    expect(enabled.find('.header-enable').exists()).toBe(true);
    expect(enabled.find('.header-disable').exists()).toBe(false);

    const disabled = mountHeader({name: 'B', enabled: false});
    expect(disabled.find('.header-enable').exists()).toBe(false);
    expect(disabled.find('.header-disable').exists()).toBe(true);
  });

  it('exposes the status slot with a tooltip via statusTitle', () => {
    const wrapper = mountHeader(
      {name: 'C', statusTitle: 'last seen 5m ago'},
      {default: '<span class="status-tag">online</span>'}
    );

    expect(wrapper.find('.status-tag').text()).toBe('online');
    expect(wrapper.find('.things-card-header-status').attributes('title')).toBe('last seen 5m ago');
  });
});
