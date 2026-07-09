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
  listDriverStatus: vi.fn(() => Promise.resolve({data: {}})),
  listDriver: vi.fn(() => Promise.resolve({data: {records: [{id: 'd-1', driverName: 'Modbus'}], total: 1}})),
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
});
