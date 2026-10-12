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

import {flushPromises} from '@vue/test-utils';
import {describe, expect, it, vi} from 'vitest';

import {mountListPage} from './_helpers';

const driverMocks = vi.hoisted(() => ({
  listDriverStatus: vi.fn(() => Promise.resolve( {})),
  listDriver: vi.fn(() => Promise.resolve( {items: [{id: 'd-1', driverName: 'Modbus'}], total: 1})),
}));

vi.mock('@/api/driver', () => driverMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('Driver list view', () => {
  it('lists drivers on mount', async () => {
    const Driver = (await import('@/views/driver/Driver.vue')).default;
    await mountListPage({
      component: Driver,
      stubs: {DriverTool: {template: '<div />'}, DriverCard: {template: '<div />'}},
    });
    await flushPromises();
    expect(driverMocks.listDriver).toHaveBeenCalledTimes(1);
  });

  it('previews the incoming fill with skeletons and keeps cards under refresh (SWR)', async () => {
    const Driver = (await import('@/views/driver/Driver.vue')).default;
    const DriverToolStub = {name: 'DriverTool', template: '<div />'};

    // First paint: the list request stays pending so the loading wall is
    // observable — skeletons must match the requested page size exactly.
    let resolveFirst!: (value: unknown) => void;
    driverMocks.listDriver.mockImplementationOnce(
      () => new Promise((resolve) => (resolveFirst = resolve)),
    );
    driverMocks.listDriverStatus.mockImplementation(() => Promise.resolve({}));

    const wrapper = await mountListPage({
      component: Driver,
      stubs: {
        DriverTool: DriverToolStub,
        DriverCard: {template: '<div class="driver-card-stub" />'},
      },
    });
    await flushPromises();
    expect(wrapper.findAll('.skeleton-card-stub')).toHaveLength(12);
    expect(wrapper.find('.entity-card-wall').classes()).not.toContain('is-refreshing');

    resolveFirst({items: [{id: 'd-1', driverName: 'Modbus'}], total: 1});
    await flushPromises();
    expect(wrapper.findAll('.skeleton-card-stub')).toHaveLength(0);
    expect(wrapper.findAll('.driver-card-stub')).toHaveLength(1);

    // Refresh: stale-while-revalidate keeps the current cards on screen
    // under the wall's activity line — no skeleton flash, no blank wall.
    let resolveSecond!: (value: unknown) => void;
    driverMocks.listDriver.mockImplementationOnce(
      () => new Promise((resolve) => (resolveSecond = resolve)),
    );
    wrapper.findComponent({name: 'DriverTool'}).vm.$emit('refresh');
    await flushPromises();

    expect(wrapper.findAll('.skeleton-card-stub')).toHaveLength(0);
    expect(wrapper.findAll('.driver-card-stub')).toHaveLength(1);
    expect(wrapper.find('.entity-card-wall').classes()).toContain('is-refreshing');

    resolveSecond({items: [{id: 'd-1', driverName: 'Modbus'}], total: 1});
    await flushPromises();
    expect(wrapper.find('.entity-card-wall').classes()).not.toContain('is-refreshing');
  });

  it('swaps status badges atomically instead of blanking them on refresh', async () => {
    const Driver = (await import('@/views/driver/Driver.vue')).default;
    driverMocks.listDriver.mockImplementation(() =>
      Promise.resolve({items: [{id: 'd-1', driverName: 'Modbus'}], total: 1}),
    );
    driverMocks.listDriverStatus.mockImplementationOnce(() => Promise.resolve({'d-1': 'ONLINE'}));

    const wrapper = await mountListPage({
      component: Driver,
      stubs: {
        DriverTool: {name: 'DriverTool', template: '<div />'},
        DriverCard: {
          props: ['statusTable'],
          // Render the badge VALUE, not the key count — only the value pins
          // the swap half of the contract (a dropped response would leave
          // the count unchanged and stay green).
          template: '<div class="status-stub">{{ (statusTable || {})["d-1"] }}</div>',
        },
      },
    });
    await flushPromises();
    expect(wrapper.find('.status-stub').text()).toBe('ONLINE');

    // The second status fetch stays pending while the list has refilled —
    // the visible badges must hold their last known values meanwhile.
    let resolveStatus!: (value: unknown) => void;
    driverMocks.listDriverStatus.mockImplementationOnce(
      () => new Promise((resolve) => (resolveStatus = resolve)),
    );
    wrapper.findComponent({name: 'DriverTool'}).vm.$emit('refresh');
    await flushPromises();
    expect(wrapper.find('.status-stub').text()).toBe('ONLINE');

    // Arrival swaps the value atomically — stale ONLINE must not stick.
    resolveStatus({'d-1': 'OFFLINE'});
    await flushPromises();
    expect(wrapper.find('.status-stub').text()).toBe('OFFLINE');
  });
});
