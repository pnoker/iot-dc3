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

const deviceMocks = vi.hoisted(() => ({
  addDevice: vi.fn(() => Promise.resolve({data: true})),
  deleteDevice: vi.fn(() => Promise.resolve({data: true})),
  listDeviceStatus: vi.fn(() => Promise.resolve({data: {}})),
  importDevice: vi.fn(() => Promise.resolve({data: true})),
  importDeviceTemplate: vi.fn(() => Promise.resolve({data: ''})),
  listDevice: vi.fn(() =>
    Promise.resolve({data: {records: [{id: 'dev-1', driverId: 'drv-1', deviceName: 'Pump'}], total: 1}})
  ),
  updateDevice: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/device', () => deviceMocks);
vi.mock('@/api/driver', () => ({
  listDriverByIds: vi.fn(() => Promise.resolve({data: {'drv-1': {driverName: 'Modbus'}}})),
}));
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('Device list view', () => {
  it('lists devices on mount', async () => {
    const Device = (await import('@/views/device/Device.vue')).default;
    await mountListPage({
      component: Device,
      stubs: {
        DeviceTool: {template: '<div />'},
        DeviceCard: {template: '<div />'},
        DeviceAddForm: {template: '<div />'},
        DeviceImportForm: {template: '<div />'},
      },
    });
    await flushPromises();
    expect(deviceMocks.listDevice).toHaveBeenCalledTimes(1);
  });
});
