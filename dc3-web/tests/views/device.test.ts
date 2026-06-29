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
