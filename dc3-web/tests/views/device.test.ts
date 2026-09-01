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
import {defineComponent, h} from 'vue';
import {describe, expect, it, vi} from 'vitest';

import {mountListPage} from './_helpers';

const deviceMocks = vi.hoisted(() => ({
  addDevice: vi.fn(() => Promise.resolve( true)),
  deleteDevice: vi.fn(() => Promise.resolve( true)),
  listDeviceStatus: vi.fn(() => Promise.resolve({})),
  importDevice: vi.fn(() => Promise.resolve({
    operationId: 'operation-1',
    statusUri: '/api/v3/manager/operations/get_by_id?id=operation-1',
  })),
  getDeviceImportOperation: vi.fn(() => Promise.resolve({
    operationId: 'operation-1',
    status: 'SUCCEEDED',
    progress: 100,
    result: {imported: 1},
    error: null,
    createdAt: '2026-08-31T00:00:00Z',
    updatedAt: '2026-08-31T00:00:01Z',
    expiresAt: '2026-09-07T00:00:00Z',
  })),
  importDeviceTemplate: vi.fn(() => Promise.resolve('')),
  listDevice: vi.fn(() => Promise.resolve({items: [{id: 'dev-1', driverId: 'drv-1', deviceName: 'Pump'}], total: 1})),
  updateDevice: vi.fn(() => Promise.resolve(true)),
}));

vi.mock('@/api/device', () => deviceMocks);
vi.mock('@/api/driver', () => ({
  listDriverByIds: vi.fn(() => Promise.resolve({'drv-1': {driverName: 'Modbus'}})),
}));
const notificationMocks = vi.hoisted(() => ({failMessage: vi.fn(), successMessage: vi.fn()}));
vi.mock('@/utils/notificationUtil', () => notificationMocks);

const DeviceImportFormStub = defineComponent({
  name: 'DeviceImportForm',
  emits: ['import', 'import-template'],
  setup(_, {emit}) {
    return () => h('button', {
      class: 'start-import',
      onClick: () => emit('import', {driverId: 'driver-1', profileId: 'profile-1'},
        new File(['xlsx'], 'devices.xlsx'), 'idempotency-1', vi.fn()),
    });
  },
});

const mountDevice = async () => {
  const Device = (await import('@/views/device/Device.vue')).default;
  const wrapper = await mountListPage({
    component: Device,
    stubs: {
      DeviceTool: {template: '<div />'},
      DeviceCard: {template: '<div />'},
      DeviceAddForm: {template: '<div />'},
      DeviceImportForm: DeviceImportFormStub,
    },
  });
  await flushPromises();
  return wrapper;
};

describe('Device list view', () => {
  it('lists devices on mount', async () => {
    await mountDevice();
    expect(deviceMocks.listDevice).toHaveBeenCalledTimes(1);
  });

  it('reloads devices only after the import operation succeeds', async () => {
    const wrapper = await mountDevice();
    await wrapper.get('.start-import').trigger('click');
    await flushPromises();

    expect(deviceMocks.importDevice).toHaveBeenCalledWith(
      {driverId: 'driver-1', profileId: 'profile-1'},
      expect.any(File),
      'idempotency-1'
    );
    expect(deviceMocks.getDeviceImportOperation).toHaveBeenCalledWith(
      '/api/v3/manager/operations/get_by_id?id=operation-1',
      expect.any(AbortSignal)
    );
    expect(deviceMocks.listDevice).toHaveBeenCalledTimes(2);
    expect(notificationMocks.failMessage).not.toHaveBeenCalled();
  });

  it('shows operation failure without reloading devices', async () => {
    deviceMocks.getDeviceImportOperation.mockResolvedValueOnce({
      operationId: 'operation-1',
      status: 'FAILED',
      progress: 5,
      result: null,
      error: {detail: 'Duplicate device name'},
      createdAt: '2026-08-31T00:00:00Z',
      updatedAt: '2026-08-31T00:00:01Z',
      expiresAt: '2026-09-07T00:00:00Z',
    });
    const wrapper = await mountDevice();
    await wrapper.get('.start-import').trigger('click');
    await flushPromises();

    expect(deviceMocks.listDevice).toHaveBeenCalledTimes(1);
    expect(notificationMocks.failMessage).toHaveBeenCalledWith('Duplicate device name');
  });
});
