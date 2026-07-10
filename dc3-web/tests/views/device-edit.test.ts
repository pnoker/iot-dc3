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

import {mountListPage} from './_helpers'; // DeviceEdit is the wizard that the recent thing-model-matrix work landed.

// DeviceEdit is the wizard that the recent thing-model-matrix work landed.
// We lock in the mount flow + the changeProfile fan-out (point/command/event
// sibling fetches) — the regression spot the new guardrail tries to catch
// statically. This view test catches the same intent at runtime.

const deviceMocks = vi.hoisted(() => ({
  getDeviceById: vi.fn(() =>
    Promise.resolve({
      data: {
        id: 'dev-1',
        deviceName: 'Pump',
        driverId: 'drv-1',
        profileId: 'pf-1',
        enableFlag: 'ENABLE',
      },
    })
  ),
  updateDevice: vi.fn(() => Promise.resolve({data: {}})),
}));

const driverMocks = vi.hoisted(() => ({
  getDriverById: vi.fn(() => Promise.resolve({data: {id: 'drv-1', driverName: 'Modbus'}})),
}));

const profileMocks = vi.hoisted(() => ({
  getProfileById: vi.fn(() => Promise.resolve({data: {id: 'pf-1', profileName: 'Sensor'}})),
}));

const dictionaryMocks = vi.hoisted(() => ({
  getDriverDictionary: vi.fn(() => Promise.resolve({data: {records: []}})),
  getProfileDictionary: vi.fn(() => Promise.resolve({data: {records: []}})),
}));

const attributeMocks = vi.hoisted(() => ({
  listCommandAttributeByDriverId: vi.fn(() => Promise.resolve({data: []})),
  listDriverAttributeByDriverId: vi.fn(() => Promise.resolve({data: []})),
  listEventAttributeByDriverId: vi.fn(() => Promise.resolve({data: []})),
  listPointAttributeByDriverId: vi.fn(() => Promise.resolve({data: []})),
}));

const infoMocks = vi.hoisted(() => ({
  addCommandInfo: vi.fn(() => Promise.resolve({data: {}})),
  addDriverInfo: vi.fn(() => Promise.resolve({data: {}})),
  addEventInfo: vi.fn(() => Promise.resolve({data: {}})),
  addPointInfo: vi.fn(() => Promise.resolve({data: {}})),
  listCommandInfoByDeviceId: vi.fn(() => Promise.resolve({data: []})),
  listDriverInfoByDeviceId: vi.fn(() => Promise.resolve({data: []})),
  listEventInfoByDeviceId: vi.fn(() => Promise.resolve({data: []})),
  listPointInfoByDeviceId: vi.fn(() => Promise.resolve({data: []})),
  updateCommandInfo: vi.fn(() => Promise.resolve({data: {}})),
  updateDriverInfo: vi.fn(() => Promise.resolve({data: {}})),
  updateEventInfo: vi.fn(() => Promise.resolve({data: {}})),
  updatePointInfo: vi.fn(() => Promise.resolve({data: {}})),
}));

const pointMocks = vi.hoisted(() => ({
  listPointByProfileId: vi.fn(() => Promise.resolve({data: []})),
}));

const commandMocks = vi.hoisted(() => ({
  listCommandByProfileId: vi.fn(() => Promise.resolve({data: []})),
}));

const eventMocks = vi.hoisted(() => ({
  listEventByProfileId: vi.fn(() => Promise.resolve({data: []})),
}));

vi.mock('@/api/device', () => deviceMocks);
vi.mock('@/api/driver', () => driverMocks);
vi.mock('@/api/profile', () => profileMocks);
vi.mock('@/api/dictionary', () => dictionaryMocks);
vi.mock('@/api/attribute', () => attributeMocks);
vi.mock('@/api/info', () => infoMocks);
vi.mock('@/api/point', () => pointMocks);
vi.mock('@/api/command', () => commandMocks);
vi.mock('@/api/event', () => eventMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));
vi.mock('@/config/router', () => ({default: {push: vi.fn(() => Promise.resolve())}}));

describe('DeviceEdit view', () => {
  it('loads the device, profile, and driver definitions on mount', async () => {
    const DeviceEdit = (await import('@/views/device/edit/DeviceEdit.vue')).default;
    await mountListPage({
      component: DeviceEdit,
      routePath: '/test',
      routeQuery: {id: 'dev-1', active: '0'},
      stubs: {
        EnableFlagSegmented: {template: '<div />'},
      },
    });
    await flushPromises();

    expect(deviceMocks.getDeviceById).toHaveBeenCalledWith('dev-1');
    expect(driverMocks.getDriverById).toHaveBeenCalledWith('drv-1');
    expect(profileMocks.getProfileById).toHaveBeenCalledWith('pf-1');

    // listPointByProfileId must fire once on initial load — the regression
    // spot is "did changeProfile() lose any of its three sibling refreshes?"
    expect(pointMocks.listPointByProfileId).toHaveBeenCalled();
    expect(commandMocks.listCommandByProfileId).toHaveBeenCalled();
    expect(eventMocks.listEventByProfileId).toHaveBeenCalled();
  });
});
